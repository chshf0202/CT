package IRBuilder;

import IRBuilder.Value.*;
import IRBuilder.Value.Object;
import Type.IRInstrType;
import Type.ValueType;

import java.util.ArrayList;

public class IRBuilder {
    private static volatile IRBuilder instance;

    private IRBuilder() {
    }

    public static IRBuilder getInstance() {
        if (instance == null) {
            instance = new IRBuilder();
        }
        return instance;
    }

    public ArrayList<String> getIR(IRModule IRModule) {
        ArrayList<String> ans = new ArrayList<>();
        ans.add("declare i32 @getint()");
        ans.add("declare void @putint(i32)");
        ans.add("declare void @putch(i32)");
        ans.add("declare void @putstr(i8*)");
        for (Value value : IRModule.getValues()) {
            if (value instanceof GlobalDecl) {
                ans.add(((GlobalDecl) value).getIR());
            } else if (value instanceof GlobalArrayDecl) {
                ans.add(((GlobalArrayDecl) value).getIR());
            } else if (value instanceof Function) {
                Function function = (Function) value;
                ans.add(function.getIR());
                for (BasicBlock basicBlock : function.getBasicBlocks()) {
                    if (function.getBasicBlocks().indexOf(basicBlock) != 0) {
                        ans.add(basicBlock.getTemp() + ":");
                    }
                    for (IRInstr instr : basicBlock.getIRInstrs()) {
                        ans.add("\t" + instr.getIR());
                    }
                }
                ans.add("}");
            }
        }
        return ans;
    }

    public Function buildFunction(String token, ValueType type, IRModule IRModule) {
        Function function = new Function(token, type);
        IRModule.addValue(function);
        return function;
    }

    public GlobalDecl buildGlobalDecl(String token, ValueType type, String temp, boolean isConst, String initVal, IRModule IRModule) {
        GlobalDecl globalDecl = new GlobalDecl(token, type, temp, isConst, initVal);
        IRModule.addValue(globalDecl);
        return globalDecl;
    }

    public GlobalArrayDecl buildGlobalArrayDecl(String token, String temp, boolean isConst, int dim, ValueType elementType, IRModule IRModule) {
        GlobalArrayDecl globalArrayDecl = new GlobalArrayDecl(token, temp, isConst, dim, elementType);
        IRModule.addValue(globalArrayDecl);
        return globalArrayDecl;
    }

    public BasicBlock buildBasicBlock(boolean addToFunc, Function function) {
        String temp = "b" + function.newbbNo();
        BasicBlock basicBlock = new BasicBlock(temp, function);
        if (addToFunc) {
            function.addBasicBlock(basicBlock);
        }
        return basicBlock;
    }

    public void buildBasicBlock(BasicBlock basicBlock, Function function) {
        function.addBasicBlock(basicBlock);
    }

    public void buildAllocaIRInstr(Object object, BasicBlock basicBlock) {
        String type;
        if (object instanceof Array) {
            type = ((Array) object).getArrayStruct();
        } else if (object instanceof Pointer) {
            type = ((Pointer) object).getPtrStruct();
        } else {
            type = object.getType().getDescription();
        }
        String IR = object.getTemp() + " = alloca " + type;
        IRInstr allocaInstr = new IRInstr(IRInstrType.ALLOCA, IR);
        object.addUse(new Use(object, allocaInstr));
        basicBlock.addIRInstr(allocaInstr);
    }

    public Object buildLoadIRInstr(Pointer object, BasicBlock basicBlock) {
        String type = object.getReferenceStruct();
        String temp = "%t" + basicBlock.newValueNo();
        String IR = temp + " = load " + type + ", " + type + "* " + object.getReference().getTemp();
        IRInstr loadInstr = new IRInstr(IRInstrType.LOAD, IR);
        object.addUse(new Use(object, loadInstr));
        basicBlock.addIRInstr(loadInstr);
        if (object.getReference().getType() == ValueType.INT) {
            return new Object(null, ValueType.INT, temp, false);
        } else if (object.getReference().getType() == ValueType.ARRAY) {
            return new Array(null, temp, false, 1, ValueType.INT);
        } else {
            assert object.getReference().getType() == ValueType.PTR;
            return new Pointer(null, temp, false, ((Pointer) ((Pointer) object).getReference()).getReference());
        }
    }

    public void buildStoreIRInstr(Pointer object, Value value, BasicBlock basicBlock) {
        String type = object.getReferenceStruct();
        String IR = "store " + type + " " + value.getTemp() + ", " + type + "* " + object.getReference().getTemp();
        IRInstr storeInstr = new IRInstr(IRInstrType.STORE, IR);
        object.addUse(new Use(object, storeInstr));
        basicBlock.addIRInstr(storeInstr);
    }

    public void buildRetIRInstr(Value value, BasicBlock basicBlock) {
        String type = value.getType().getDescription();
        String IR = "ret " + type;
        if (value.getType() != ValueType.VOID) {
            IR += " " + value.getTemp();
        }
        IRInstr retInstr = new IRInstr(IRInstrType.RET, IR);
        if (value.getType() != ValueType.VOID) {
            value.addUse(new Use(value, retInstr));
        }
        basicBlock.addIRInstr(retInstr);
    }

    public Value buildCalculateIRInstr(String op, Value value1, Value value2, BasicBlock basicBlock, boolean alloca) {
        String temp = "";
        String type = value1.getType().getDescription();
        if (op.equals("+")) {
            if (value1.isImm() && value2.isImm()) {
                int imm = value1.getImm() + value2.getImm();
                return new Value(null, ValueType.INT, String.valueOf(imm));
            }
            if (alloca) {
                temp = "%t" + basicBlock.newValueNo();
            }
            Value ret = new Value(null, ValueType.INT, temp);
            if (alloca) {
                String IR = temp + " = add " + type + " " + value1.getTemp() + ", " + value2.getTemp();
                IRInstr instr = new IRInstr(IRInstrType.ADD, IR);
                value1.addUse(new Use(value1, instr));
                value2.addUse(new Use(value2, instr));
                basicBlock.addIRInstr(instr);
            }
            return ret;
        } else if (op.equals("-")) {
            if (value1.isImm() && value2.isImm()) {
                int imm = value1.getImm() - value2.getImm();
                return new Value(null, ValueType.INT, String.valueOf(imm));
            }
            if (alloca) {
                temp = "%t" + basicBlock.newValueNo();
            }
            Value ret = new Value(null, ValueType.INT, temp);
            if (alloca) {
                String IR = temp + " = sub " + type + " " + value1.getTemp() + ", " + value2.getTemp();
                IRInstr instr = new IRInstr(IRInstrType.SUB, IR);
                value1.addUse(new Use(value1, instr));
                value2.addUse(new Use(value2, instr));
                basicBlock.addIRInstr(instr);
            }
            return ret;
        } else if (op.equals("*")) {
            if (value1.isImm() && value2.isImm()) {
                int imm = value1.getImm() * value2.getImm();
                return new Value(null, ValueType.INT, String.valueOf(imm));
            }
            if (alloca) {
                temp = "%t" + basicBlock.newValueNo();
            }
            Value ret = new Value(null, ValueType.INT, temp);
            if (alloca) {
                String IR = temp + " = mul " + type + " " + value1.getTemp() + ", " + value2.getTemp();
                IRInstr instr = new IRInstr(IRInstrType.MUL, IR);
                value1.addUse(new Use(value1, instr));
                value2.addUse(new Use(value2, instr));
                basicBlock.addIRInstr(instr);
            }
            return ret;
        } else if (op.equals("/")) {
            if (value1.isImm() && value2.isImm()) {
                int imm = value1.getImm() / value2.getImm();
                return new Value(null, ValueType.INT, String.valueOf(imm));
            }
            if (alloca) {
                temp = "%t" + basicBlock.newValueNo();
            }
            Value ret = new Value(null, ValueType.INT, temp);
            if (alloca) {
                String IR = temp + " = sdiv " + type + " " + value1.getTemp() + ", " + value2.getTemp();
                IRInstr instr = new IRInstr(IRInstrType.SDIV, IR);
                value1.addUse(new Use(value1, instr));
                value2.addUse(new Use(value2, instr));
                basicBlock.addIRInstr(instr);
            }
            return ret;
        } else if (op.equals("%")) {
            if (value1.isImm() && value2.isImm()) {
                int imm = value1.getImm() % value2.getImm();
                return new Value(null, ValueType.INT, String.valueOf(imm));
            }
            if (alloca) {
                temp = "%t" + basicBlock.newValueNo();
            }
            Value ret = new Value(null, ValueType.INT, temp);
            if (alloca) {
                String IR = temp + " = srem " + type + " " + value1.getTemp() + ", " + value2.getTemp();
                IRInstr instr = new IRInstr(IRInstrType.SREM, IR);
                value1.addUse(new Use(value1, instr));
                value2.addUse(new Use(value2, instr));
                basicBlock.addIRInstr(instr);
            }
            return ret;
        } else {
            return null;
        }
    }

    public Value buildCallIRInstr(String token, Function function, ArrayList<Value> paras, BasicBlock basicBlock) {
        if (token.equals("@getint")) {
            String temp = "%t" + basicBlock.newValueNo();
            String type = "i32";
            String IR = temp + " = call " + type + " @getint()";
            IRInstr instr = new IRInstr(IRInstrType.CALL, IR);
            basicBlock.addIRInstr(instr);
            return new Value(null, ValueType.INT, temp);
        } else if (token.equals("@putch")) {
            String type = "i32";
            String IR = "call void @putch(" + type + " " + paras.get(0).getTemp() + ")";
            IRInstr instr = new IRInstr(IRInstrType.CALL, IR);
            basicBlock.addIRInstr(instr);
            return null;
        } else if (token.equals("@putint")) {
            String type = "i32";
            String IR = "call void @putint(" + type + " " + paras.get(0).getTemp() + ")";
            IRInstr instr = new IRInstr(IRInstrType.CALL, IR);
            paras.get(0).addUse(new Use(paras.get(0), instr));
            basicBlock.addIRInstr(instr);
            return null;
        } else {
            assert function != null;
            assert paras != null;
            String retType = function.getRetValueType().getDescription();
            StringBuilder IR;
            String temp = null;
            if (retType.equals("void")) {
                IR = new StringBuilder("call " + retType + " " + token + "(");
            } else {
                temp = "%t" + basicBlock.newValueNo();
                IR = new StringBuilder(temp + " = call " + retType + " " + token + "(");
            }
            IRInstr instr;
            if (paras.isEmpty()) {
                instr = new IRInstr(IRInstrType.CALL, IR + ")");
            } else {
                for (Value para : paras) {
                    String paraType;
                    if (para instanceof Pointer) {
                        paraType = ((Pointer) para).getPtrStruct();
                    } else {
                        paraType = para.getType().getDescription();
                    }
                    IR.append(paraType).append(" ").append(para.getTemp()).append(", ");
                }
                IR = new StringBuilder(IR.substring(0, IR.length() - 2) + ")");
                instr = new IRInstr(IRInstrType.CALL, IR.toString());
                for (Value para : paras) {
                    para.addUse(new Use(para, instr));
                }
            }
            basicBlock.addIRInstr(instr);
            if (temp == null) {
                return null;
            } else {
                return new Value(null, function.getRetValueType(), temp);
            }
        }
    }

    public Value buildZextIRInstr(Value value, ValueType type, BasicBlock basicBlock) {
        String temp = "%t" + basicBlock.newValueNo();
        String IR = temp + " = zext " + value.getType().getDescription() + " " + value.getTemp()
                + " to " + type.getDescription();
        IRInstr instr = new IRInstr(IRInstrType.ZEXT, IR);
        basicBlock.addIRInstr(instr);
        value.addUse(new Use(value, instr));
        return new Object(null, type, temp, false);
    }

    public Value buildICmpIRInstr(String cond, Value value1, Value value2, BasicBlock basicBlock) {
        String type1 = value1.getType().getDescription();
        String type2 = value2.getType().getDescription();
        String type;
        if (type1.equals(type2)) {
            type = type1;
        } else if (value1.getType() == ValueType.I1) {
            value1 = buildZextIRInstr(value1, ValueType.INT, basicBlock);
            type = ValueType.INT.getDescription();
        } else {
            value2 = buildZextIRInstr(value2, ValueType.INT, basicBlock);
            type = ValueType.INT.getDescription();
        }
        String temp = "%t" + basicBlock.newValueNo();
        String IR = temp + " = icmp " + cond + " " + type + " " + value1.getTemp() + ", " + value2.getTemp();
        IRInstr instr = new IRInstr(IRInstrType.ICMP, IR);
        basicBlock.addIRInstr(instr);
        value1.addUse(new Use(value1, instr));
        value2.addUse(new Use(value2, instr));
        return new Value(null, ValueType.I1, temp);
    }

    public void buildBrIRInstr(Value value, BasicBlock truebb, BasicBlock falsebb, BasicBlock basicBlock) {
        assert value.getType() == ValueType.I1;
        String type = value.getType().getDescription();
        String IR = "br " + type + " " + value.getTemp() + ", label %" + truebb.getTemp() + ", label %" + falsebb.getTemp();
        IRInstr instr = new IRInstr(IRInstrType.BR, IR);
        basicBlock.addIRInstr(instr);
        value.addUse(new Use(value, instr));
    }

    public void buildBrIRInstr(BasicBlock bb, BasicBlock basicBlock) {
        String IR = "br label %" + bb.getTemp();
        IRInstr instr = new IRInstr(IRInstrType.BR, IR);
        basicBlock.addIRInstr(instr);
    }

    public Pointer buildGepIRInstr(Object object, ArrayList<String> offset, BasicBlock basicBlock) {
        String type;
        if (object instanceof Pointer) {
            type = ((Pointer) object).getReferenceStruct();
        } else {
            assert object instanceof Array;
            type = ((Array) object).getArrayStruct();
        }
        String temp = "%t" + basicBlock.newValueNo();
        StringBuilder IR = new StringBuilder(temp + " = getelementptr " + type + ", " + type + "* " + object.getTemp());
        for (String off : offset) {
            IR.append(", i32 ").append(off);
        }
        IRInstr gepInstr = new IRInstr(IRInstrType.GEP, IR.toString());
        object.addUse(new Use(object, gepInstr));
        basicBlock.addIRInstr(gepInstr);
        if (object instanceof Pointer) {//special: return both pointer and reference has the same temp
            if (((Pointer) object).getReference().getType() == ValueType.INT) {
                return new Pointer(null, temp, false,
                        new Object(null, ValueType.INT, temp, false));
            } else if (((Pointer) object).getReference().getType() == ValueType.PTR) {
                assert ((Pointer) object).getReference() instanceof Pointer;
                Pointer reference = (Pointer) ((Pointer) object).getReference();
                if (reference.getReference().getType() == ValueType.INT) {
                    return new Pointer(null, temp, false,
                            new Object(null, ValueType.INT, temp, false));
                } else {
                    assert reference.getReference().getType() == ValueType.ARRAY;
                    Array array = new Array(null, temp, false, 1, ValueType.INT);
                    for (Value value : ((Array) reference.getReference()).getConstValue()) {
                        array.addConstValue(value);
                    }
                    array.setElementNum(array.getConstValue().size());
                    return new Pointer(null, temp, false, array);
                }
            } else {
                if (offset.size() == 1) {
                    Array array = new Array(null, temp, false, 1, ValueType.INT);
                    for (Value value : ((Array) ((Pointer) object).getReference()).getConstValue()) {
                        array.addConstValue(value);
                    }
                    array.setElementNum(array.getConstValue().size());
                    return new Pointer(null, temp, false, array);
                } else {
                    assert offset.size() == 2;
                    return new Pointer(null, temp, false,
                            new Object(null, ValueType.INT, temp, false));
                }
            }
        } else {
            if (offset.size() == 2) {
                if (((Array) object).getDim() == 1) {
                    return new Pointer(null, temp, false,
                            new Object(null, ValueType.INT, temp, false));
                } else {
                    Array array = new Array(null, temp, false, 1, ValueType.INT);
                    for (Value value : ((Array) ((Array) object).getConstValue().get(0)).getConstValue()) {
                        array.addConstValue(value);
                    }
                    array.setElementNum(array.getConstValue().size());
                    return new Pointer(null, temp, false, array);
                }
            } else {
                assert offset.size() == 3;
                return new Pointer(null, temp, false,
                        new Object(null, ValueType.INT, temp, false));
            }
        }
    }
}
