package IRBuilder;

import IRBuilder.Symbol.SymbolIRManager;
import IRBuilder.Value.*;
import IRBuilder.Value.Object;
import Lexer.TermNode;
import Parser.ASTNode;
import Type.IRInstrType;
import Type.ValueType;
import Type.LexType;
import Type.NonTermType;

import java.util.ArrayList;

public class Visitor {
    private static volatile Visitor instance;
    private ASTNode ASTRoot;
    private ASTNode curASTNode;
    private IRModule IRModule;
    private Function curFunction;
    private BasicBlock curBasicBlock;
    private BasicBlock truebb;
    private BasicBlock falsebb;
    private ArrayList<BasicBlock> continueBrbb;
    private ArrayList<BasicBlock> breakBrbb;

    private Visitor() {
    }

    public void initVisitor(ASTNode ASTRoot) {
        this.ASTRoot = ASTRoot;
        this.curASTNode = ASTRoot;
        this.IRModule = new IRModule();
        this.curFunction = null;
        this.curBasicBlock = null;
        this.truebb = null;
        this.falsebb = null;
        this.continueBrbb = new ArrayList<>();
        this.breakBrbb = new ArrayList<>();
    }

    public static Visitor getInstance() {
        if (instance == null) {
            instance = new Visitor();
        }
        return instance;
    }

    public IRModule getIRModule() {
        return IRModule;
    }

    public ArrayList<String> createAns() {
        visitComputeUnit();
        return IRBuilder.getInstance().getIR(IRModule);
    }

    private void visitComputeUnit() {
        ASTNode thisNode = curASTNode;
        for (ASTNode node : thisNode.getChildren()) {
            if (node.getGrammarType() == NonTermType.DECL) {
                ASTNode child = node.getChildren().get(0);
                if (child.getGrammarType() == NonTermType.CONSTDECL) {
                    curASTNode = child;
                    visitConstDecl();
                    curASTNode = thisNode;
                } else if (child.getGrammarType() == NonTermType.VARDECL) {
                    curASTNode = child;
                    visitVarDecl();
                    curASTNode = thisNode;
                }
            } else if (node.getGrammarType() == NonTermType.FUNCDEF) {
                curASTNode = node;
                visitFuncDef();
                curASTNode = thisNode;
            } else if (node.getGrammarType() == NonTermType.MAINFUNCDEF) {
                curASTNode = node;
                visitMainFuncDef();
                curASTNode = thisNode;
            }
        }
    }

    private void visitFuncDef() {
        ASTNode thisNode = curASTNode;
        String token = ((TermNode) thisNode.getChildren().get(1)).getToken();
        ValueType funcRetType = thisNode.getChildren().get(0).getChildren().get(0).getGrammarType()
                == LexType.INTTK ? ValueType.INT : ValueType.VOID;
        curFunction = IRBuilder.getInstance()
                .buildFunction(token, funcRetType, IRModule);
        SymbolIRManager.getInstance().registerSymbol(token, curFunction);
        ArrayList<Value> paras = new ArrayList<>();
        if (thisNode.getChildren().get(3).getGrammarType() == NonTermType.FUNCFPARAMS) {
            curASTNode = thisNode.getChildren().get(3);
            paras.addAll(visitFuncFParams());
            curASTNode = thisNode;
        }
        curBasicBlock = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
        curASTNode = thisNode.getChildren().get(thisNode.getChildren().size() - 1);
        visitBlock(paras);
        curASTNode = thisNode;
        if (!curFunction.hasBuiltRet()) {
            assert curFunction.getRetValueType() == ValueType.VOID;
            IRBuilder.getInstance().buildRetIRInstr(new Value(null, ValueType.VOID, null), curBasicBlock);
        }
        curBasicBlock = null;
        curFunction = null;
    }

    private void visitMainFuncDef() {
        ASTNode thisNode = curASTNode;
        curFunction = IRBuilder.getInstance()
                .buildFunction("main", ValueType.INT, IRModule);
        SymbolIRManager.getInstance().registerSymbol("main", curFunction);
        curBasicBlock = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
        curASTNode = thisNode.getChildren().get(4);
        visitBlock(null);
        curASTNode = thisNode;
        curFunction = null;
    }

    private void visitConstDecl() {
        ASTNode thisNode = curASTNode;
        assert thisNode.getChildren().get(1).getChildren().get(0).getGrammarType() == LexType.INTTK;
        for (ASTNode child : thisNode.getChildren()) {
            if (child.getGrammarType() == NonTermType.CONSTDEF) {
                String token = ((TermNode) child.getChildren().get(0)).getToken();
                Value dim1 = null;
                Value dim2 = null;
                if (child.getChildren().size() > 3) {
                    curASTNode = child.getChildren().get(2);
                    dim1 = visitConstExp(false);
                    curASTNode = thisNode;
                    if (child.getChildren().size() > 6) {
                        curASTNode = child.getChildren().get(5);
                        dim2 = visitConstExp(false);
                        curASTNode = thisNode;
                    }
                }
                curASTNode = child.getChildren().get(child.getChildren().size() - 1);
                Value value = visitConstInitVal(false, dim1, dim2, true);
                curASTNode = thisNode;
                value.setToken(token);
                if (curFunction == null) {
                    value.setTemp("@" + token);
                    if (value instanceof GlobalArrayDecl) {
                        ((GlobalArrayDecl) value).initialize();
                    }
                } else if (value instanceof Array) {
                    value.setToken(token);
                    value.setTemp("%__" + token + "__" + curBasicBlock.newValueNo());
                    IRBuilder.getInstance().buildAllocaIRInstr((Object) value, curBasicBlock);
                    if (((Array) value).getDim() == 1) {
                        for (int i = 0; i < ((Array) value).getElementNum(); i++) {
                            Value childValue = ((Array) value).getConstValue().get(i);
                            ArrayList<String> offset = new ArrayList<>();
                            offset.add("0");
                            offset.add(String.valueOf(i));
                            Pointer object = IRBuilder.getInstance().buildGepIRInstr((Object) value, offset, curBasicBlock);
                            IRBuilder.getInstance().buildStoreIRInstr(object, childValue, curBasicBlock);
                        }
                    } else {
                        for (int j = 0; j < ((Array) value).getElementNum(); j++) {
                            Value outerChildValue = ((Array) value).getConstValue().get(j);
                            for (int i = 0; i < ((Array) outerChildValue).getElementNum(); i++) {
                                Value childValue = ((Array) outerChildValue).getConstValue().get(i);
                                ArrayList<String> offset = new ArrayList<>();
                                offset.add("0");
                                offset.add(String.valueOf(j));
                                offset.add(String.valueOf(i));
                                Pointer object = IRBuilder.getInstance().buildGepIRInstr((Object) value, offset, curBasicBlock);
                                IRBuilder.getInstance().buildStoreIRInstr(object, childValue, curBasicBlock);
                            }
                        }
                    }
                }
                SymbolIRManager.getInstance().registerSymbol(token, value);
            }
        }
    }

    private Value visitConstInitVal(boolean alloca, Value dim1, Value dim2, boolean isFirst) {
        ASTNode thisNode = curASTNode;
        if (dim1 == null) {
            curASTNode = thisNode.getChildren().get(0);
            Value value = visitConstExp(alloca);
            curASTNode = thisNode;
            if (curBasicBlock == null) {
                if (isFirst) {
                    return IRBuilder.getInstance().buildGlobalDecl(null, ValueType.INT,
                            null, true, value.getTemp(), IRModule);
                } else {
                    return new GlobalDecl(null, ValueType.INT, null, true, value.getTemp());
                }
            } else {
                return new Object(null, ValueType.INT, value.getTemp(), true);
            }
        } else {
            ArrayList<Value> values = new ArrayList<>();
            for (ASTNode child : thisNode.getChildren()) {
                if (child.getGrammarType() == NonTermType.CONSTINITVAL
                        || child.getGrammarType() == NonTermType.INITVAL) {
                    curASTNode = child;
                    Value value = visitConstInitVal(alloca, dim2, null, false);
                    curASTNode = thisNode;
                    values.add(value);
                }
            }
            assert !values.isEmpty();
            int dim;
            ValueType type;
            if (values.get(0) instanceof Array) {
                dim = ((Array) values.get(0)).getDim() + 1;
                type = ValueType.ARRAY;
            } else {
                dim = 1;
                type = ValueType.INT;
            }
            if (curBasicBlock == null) {
                GlobalArrayDecl globalArrayDecl;
                if (isFirst) {
                    globalArrayDecl = IRBuilder.getInstance().buildGlobalArrayDecl(null,
                            null, true, dim, type, IRModule);
                } else {
                    globalArrayDecl = new GlobalArrayDecl(null, null, true, dim, type);
                }
                for (Value value : values) {
                    globalArrayDecl.addConstValue(value);
                }
                globalArrayDecl.setElementNum(values.size());
                return globalArrayDecl;
            } else {
                Array array = new Array(null, null, true, dim, type);
                for (Value value : values) {
                    array.addConstValue(value);
                }
                array.setElementNum(values.size());
                return array;
            }
        }
    }

    private void visitVarDecl() {
        ASTNode thisNode = curASTNode;
        assert thisNode.getChildren().get(0).getChildren().get(0).getGrammarType() == LexType.INTTK;
        for (ASTNode child : thisNode.getChildren()) {
            if (child.getGrammarType() == NonTermType.VARDEF) {
                String token = ((TermNode) child.getChildren().get(0)).getToken();
                if (child.getChildren().get(child.getChildren().size() - 1).getGrammarType() == NonTermType.INITVAL) {
                    Value dim1 = null;
                    Value dim2 = null;
                    if (child.getChildren().size() > 3) {
                        curASTNode = child.getChildren().get(2);
                        dim1 = visitConstExp(false);
                        curASTNode = thisNode;
                        if (child.getChildren().size() > 6) {
                            curASTNode = child.getChildren().get(5);
                            dim2 = visitConstExp(false);
                            curASTNode = thisNode;
                        }
                    }
                    if (curFunction == null) {
                        curASTNode = child.getChildren().get(child.getChildren().size() - 1);
                        Value value = visitConstInitVal(false, dim1, dim2, true);
                        curASTNode = thisNode;
                        value.setToken(token);
                        value.setTemp("@" + token);
                        assert value instanceof Object;
                        ((Object) value).setIsConst(false);
                        if (value instanceof GlobalArrayDecl) {
                            ((GlobalArrayDecl) value).initialize();
                        }
                        SymbolIRManager.getInstance().registerSymbol(token, value);
                    } else {
                        curASTNode = child.getChildren().get(child.getChildren().size() - 1);
                        Value value = visitInitVal(token, "%__" + token + "__"
                                + curBasicBlock.newValueNo(), dim1, dim2);
                        curASTNode = thisNode;
                        SymbolIRManager.getInstance().registerSymbol(token, value);
                    }
                } else {
                    if (curFunction == null) {
                        curASTNode = child;
                        Value value = createEmptyObject();
                        curASTNode = thisNode;
                        value.setToken(token);
                        value.setTemp("@" + token);
                        if (value instanceof GlobalArrayDecl) {
                            ((GlobalArrayDecl) value).initialize();
                        }
                        SymbolIRManager.getInstance().registerSymbol(token, value);
                    } else {
                        curASTNode = child;
                        Object object = createEmptyObject();
                        curASTNode = thisNode;
                        object.setToken(token);
                        object.setTemp("%__" + token + "__" + curBasicBlock.newValueNo());
                        IRBuilder.getInstance().buildAllocaIRInstr(object, curBasicBlock);
                        SymbolIRManager.getInstance().registerSymbol(token, object);
                    }
                }
            }
        }
    }

    private Object createEmptyObject() {
        ASTNode thisNode = curASTNode;
        if (thisNode.getChildren().size() == 1) {
            if (curBasicBlock == null) {
                return IRBuilder.getInstance().buildGlobalDecl(null, ValueType.INT,
                        null, false, "0", IRModule);
            } else {
                return new Object(null, ValueType.INT, null, false);
            }
        } else {
            curASTNode = thisNode.getChildren().get(2);
            Value dim1 = visitConstExp(false);
            curASTNode = thisNode;
            Array outerArray;
            if (thisNode.getChildren().size() == 7) {
                if (curBasicBlock == null) {
                    outerArray = IRBuilder.getInstance()
                            .buildGlobalArrayDecl(null, null, false, 2, ValueType.ARRAY, IRModule);
                } else {
                    outerArray = new Array(null, null, false, 2, ValueType.ARRAY);
                }
            } else {
                if (curBasicBlock == null) {
                    outerArray = IRBuilder.getInstance()
                            .buildGlobalArrayDecl(null, null, false, 1, ValueType.INT, IRModule);
                } else {
                    outerArray = new Array(null, null, false, 1, ValueType.INT);
                }
            }
            for (int j = 0; j < Integer.parseInt(dim1.getTemp()); j++) {
                if (thisNode.getChildren().size() == 7) {
                    curASTNode = thisNode.getChildren().get(5);
                    Value dim2 = visitConstExp(false);
                    curASTNode = thisNode;
                    if (curBasicBlock == null) {
                        GlobalArrayDecl globalArrayDecl = new GlobalArrayDecl(null, null, false, 1, ValueType.INT);
                        for (int i = 0; i < Integer.parseInt(dim2.getTemp()); i++) {
                            globalArrayDecl.addConstValue(new GlobalDecl(null, ValueType.INT, null, false, "0"));
                        }
                        globalArrayDecl.setElementNum(Integer.parseInt(dim2.getTemp()));
                        outerArray.addConstValue(globalArrayDecl);
                    } else {
                        Array array = new Array(null, null, false, 1, ValueType.INT);
                        for (int i = 0; i < Integer.parseInt(dim2.getTemp()); i++) {
                            array.addConstValue(new Object(null, ValueType.INT, null, false));
                        }
                        array.setElementNum(Integer.parseInt(dim2.getTemp()));
                        outerArray.addConstValue(array);
                    }
                } else {
                    if (curBasicBlock == null) {
                        outerArray.addConstValue(new GlobalDecl(null, ValueType.INT, null, false, "0"));
                    } else {
                        outerArray.addConstValue(new Object(null, ValueType.INT, null, false));
                    }
                }
            }
            outerArray.setElementNum(Integer.parseInt(dim1.getTemp()));
            return outerArray;
        }
    }

    private Value visitInitVal(String token, String temp, Value dim1, Value dim2) {
        ASTNode thisNode = curASTNode;
        if (dim1 == null) {
            Object object = new Object(null, ValueType.INT, temp, false);
            IRBuilder.getInstance().buildAllocaIRInstr(object, curBasicBlock);
            curASTNode = thisNode.getChildren().get(0);
            Value value = visitExp(true);
            curASTNode = thisNode;
            Pointer ptr = new Pointer(token, temp, false, object);
            IRBuilder.getInstance().buildStoreIRInstr(ptr, value, curBasicBlock);
            return object;
        } else if (dim2 == null) {
            Array array = new Array(token, temp, false, 1, ValueType.INT);
            array.setElementNum(dim1.getImm());
            for (int i = 1; i < thisNode.getChildren().size(); i += 2) {
                ArrayList<String> offset = new ArrayList<>();
                offset.add("0");
                offset.add(String.valueOf((i - 1) / 2));
                curASTNode = thisNode.getChildren().get(i).getChildren().get(0);
                Value value = visitExp(true);
                curASTNode = thisNode;
                array.addConstValue(value);
                if (i == 1) {
                    IRBuilder.getInstance().buildAllocaIRInstr(array, curBasicBlock);
                }
                Pointer object = IRBuilder.getInstance().buildGepIRInstr(array, offset, curBasicBlock);
                IRBuilder.getInstance().buildStoreIRInstr(object, value, curBasicBlock);
            }
            return array;
        } else {
            Array outerArray = new Array(token, temp, false, 2, ValueType.ARRAY);
            outerArray.setElementNum(dim1.getImm());
            for (int j = 1; j < thisNode.getChildren().size(); j += 2) {
                ASTNode child = thisNode.getChildren().get(j);
                Array array = new Array(token, temp, false, 1, ValueType.INT);
                outerArray.addConstValue(array);
                array.setElementNum(dim2.getImm());
                for (int i = 1; i < child.getChildren().size(); i += 2) {
                    ArrayList<String> offset = new ArrayList<>();
                    offset.add("0");
                    offset.add(String.valueOf((j - 1) / 2));
                    offset.add(String.valueOf((i - 1) / 2));
                    curASTNode = child.getChildren().get(i).getChildren().get(0);
                    Value value = visitExp(true);
                    curASTNode = child;
                    array.addConstValue(value);
                    if (j == 1 && i == 1) {
                        IRBuilder.getInstance().buildAllocaIRInstr(outerArray, curBasicBlock);
                    }
                    Pointer object = IRBuilder.getInstance().buildGepIRInstr(outerArray, offset, curBasicBlock);
                    IRBuilder.getInstance().buildStoreIRInstr(object, value, curBasicBlock);
                }
                array.setElementNum(array.getConstValue().size());
            }
            outerArray.setElementNum(outerArray.getConstValue().size());
            return outerArray;
        }
    }

    private ArrayList<Value> visitFuncFParams() {
        ASTNode thisNode = curASTNode;
        ArrayList<Value> ret = new ArrayList<>();
        for (int i = 0; i < thisNode.getChildren().size(); i++) {
            ASTNode child = thisNode.getChildren().get(i);
            if (child.getGrammarType() == NonTermType.FUNCFPARAM) {
                curASTNode = child;
                ret.add(visitFuncFParam());
                curASTNode = thisNode;
            }
        }
        return ret;
    }

    private Value visitFuncFParam() {
        ASTNode thisNode = curASTNode;
        assert thisNode.getChildren().get(0).getChildren().get(0).getGrammarType() == LexType.INTTK;
        String token = ((TermNode) thisNode.getChildren().get(1)).getToken();
        if (thisNode.getChildren().size() == 2) {
            Object object = new Object(token, ValueType.INT, "%__" + token + "__para", false);
            Pointer ptr = new Pointer(null, null, false, object);
            SymbolIRManager.getInstance().registerPara(token, object);
            return ptr;
        } else if (thisNode.getChildren().size() == 4) {
            Pointer ptr1 = new Pointer(token, "%__" + token + "__para", false,
                    new Object(null, ValueType.INT, null, false));
            Pointer ptr2 = new Pointer(null, null, false, ptr1);
            SymbolIRManager.getInstance().registerPara(token, ptr1);
            return ptr2;
        } else {
            Array array = new Array(null, null, false, 1, ValueType.INT);
            curASTNode = thisNode.getChildren().get(5);
            Value value = visitConstExp(false);
            curASTNode = thisNode;
            assert value.isImm();
            for (int i = 0; i < Integer.parseInt(value.getTemp()); i++) {
                array.addConstValue(new Object(null, ValueType.INT, null, false));
            }
            array.setElementNum(Integer.parseInt(value.getTemp()));
            Pointer ptr1 = new Pointer(token, "%__" + token + "__para", false, array);
            Pointer ptr2 = new Pointer(null, null, false, ptr1);
            SymbolIRManager.getInstance().registerPara(token, ptr1);
            return ptr2;
        }
    }

    private void visitBlock(ArrayList<Value> paras) {
        ASTNode thisNode = curASTNode;
        SymbolIRManager.getInstance().proceedScope();
        if (paras != null && !paras.isEmpty()) {
            for (int i = 0; i < paras.size(); i++) {
                Value para = paras.get(i);
                assert para instanceof Pointer;
                IRBuilder.getInstance().buildAllocaIRInstr((Object) ((Pointer) para).getReference(), curBasicBlock);
                Value reference;
                if (((Pointer) para).getReference().getType() == ValueType.INT) {
                    reference = new Object(null, ValueType.INT, null, false);
                } else if (((Pointer) para).getReference().getType() == ValueType.PTR) {
                    reference = new Pointer(null, null, false,
                            ((Pointer) ((Pointer) para).getReference()).getReference());
                } else {
                    reference = new Array(null, null, false, 1, ValueType.INT);
                    for (int j = 0; j < ((Array) ((Pointer) para).getReference()).getElementNum(); j++) {
                        ((Array) reference).addConstValue(new Object(null, ValueType.INT, null, false));
                    }
                    ((Array) reference).setElementNum(((Array) ((Pointer) para).getReference()).getElementNum());
                }
                IRBuilder.getInstance().buildStoreIRInstr((Pointer) para
                        , new Pointer(null, "%para" + i, false, reference), curBasicBlock);
            }
        }
        for (ASTNode blockItem : thisNode.getChildren()) {
            if (blockItem.getGrammarType() == NonTermType.BLOCKITEM) {
                if (blockItem.getChildren().get(0).getGrammarType() == NonTermType.DECL) {
                    ASTNode decl = blockItem.getChildren().get(0);
                    if (decl.getChildren().get(0).getGrammarType() == NonTermType.CONSTDECL) {
                        curASTNode = decl.getChildren().get(0);
                        visitConstDecl();
                        curASTNode = thisNode;
                    } else {
                        curASTNode = decl.getChildren().get(0);
                        visitVarDecl();
                        curASTNode = thisNode;
                    }
                } else {
                    curASTNode = blockItem.getChildren().get(0);
                    visitStmt();
                    curASTNode = thisNode;
                }
            }
        }
        SymbolIRManager.getInstance().retrieveScope();
    }

    private void visitStmt() {
        ASTNode thisNode = curASTNode;
        ASTNode firstChild = thisNode.getChildren().get(0);
        if (firstChild.getGrammarType() == NonTermType.LVAL) {
            curASTNode = firstChild;
            Value lvalue = visitLVal(false);
            curASTNode = thisNode;
            Value value;
            if (thisNode.getChildren().get(2).getGrammarType() == LexType.GETINTTK) {
                value = IRBuilder.getInstance().buildCallIRInstr("@getint", null, null, curBasicBlock);
            } else {
                curASTNode = thisNode.getChildren().get(2);
                value = visitExp(true);
                curASTNode = thisNode;
            }
            assert lvalue != null;
            assert value != null;
            if (lvalue instanceof Pointer) {
                IRBuilder.getInstance().buildStoreIRInstr((Pointer) lvalue, value, curBasicBlock);
            } else {
                Pointer ptr = new Pointer(lvalue.getToken(), lvalue.getTemp(), false, lvalue);
                IRBuilder.getInstance().buildStoreIRInstr(ptr, value, curBasicBlock);
            }
        } else if (firstChild.getGrammarType() == NonTermType.BLOCK) {
            curASTNode = firstChild;
            visitBlock(null);
            curASTNode = thisNode;
        } else if (firstChild.getGrammarType() == LexType.RETURNTK) {
            if (thisNode.getChildren().get(1).getGrammarType() == NonTermType.EXP) {
                curASTNode = thisNode.getChildren().get(1);
                Value value = visitExp(true);
                curASTNode = thisNode;
                if (curBasicBlock.getIRInstrs().isEmpty()
                        || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                        && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                    IRBuilder.getInstance().buildRetIRInstr(value, curBasicBlock);
                }
            } else {
                if (curBasicBlock.getIRInstrs().isEmpty()
                        || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                        && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                    IRBuilder.getInstance().buildRetIRInstr(new Value(null, ValueType.VOID, null), curBasicBlock);
                }
            }
        } else if (firstChild.getGrammarType() == LexType.PRINTFTK) {
            String format = ((TermNode) thisNode.getChildren().get(2)).getToken();
            int paraNo = 0;
            for (int i = 1; i < format.length() - 1; i++) {
                if (format.charAt(i) == '%' && i + 1 < format.length() && format.charAt(i + 1) == 'd') {
                    curASTNode = thisNode.getChildren().get(4 + paraNo * 2);
                    Value value = visitExp(true);
                    curASTNode = thisNode;
                    ArrayList<Value> paras = new ArrayList<>();
                    paras.add(value);
                    IRBuilder.getInstance().buildCallIRInstr("@putint", null, paras, curBasicBlock);
                    i++;
                    paraNo++;
                } else {
                    ArrayList<Value> paras = new ArrayList<>();
                    if (format.charAt(i) == '\\') {
                        paras.add(new Value("\n", ValueType.INT, String.valueOf(10)));
                        i++;
                    } else {
                        paras.add(new Value(String.valueOf(format.charAt(i)), ValueType.INT
                                , String.valueOf((int) format.charAt(i))));
                    }
                    IRBuilder.getInstance().buildCallIRInstr("@putch", null, paras, curBasicBlock);
                }
            }
        } else if (firstChild.getGrammarType() == LexType.IFTK) {
            BasicBlock stmt1bb = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
            BasicBlock stmt2bb = null;
            if (thisNode.getChildren().size() > 5) {
                stmt2bb = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
            }
            BasicBlock afterIfbb = IRBuilder.getInstance().buildBasicBlock(false, curFunction);
            curASTNode = thisNode.getChildren().get(2);
            visitCond(stmt1bb, stmt2bb, afterIfbb);
            curASTNode = thisNode;
            curBasicBlock = stmt1bb;
            curASTNode = thisNode.getChildren().get(4);
            visitStmt();
            curASTNode = thisNode;
            if (curBasicBlock.getIRInstrs().isEmpty()
                    || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                    && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                IRBuilder.getInstance().buildBrIRInstr(afterIfbb, curBasicBlock);
            }
            if (thisNode.getChildren().size() > 5) {
                curBasicBlock = stmt2bb;
                curASTNode = thisNode.getChildren().get(6);
                visitStmt();
                curASTNode = thisNode;
                if (curBasicBlock.getIRInstrs().isEmpty()
                        || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                        && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                    IRBuilder.getInstance().buildBrIRInstr(afterIfbb, curBasicBlock);
                }
            }
            curBasicBlock = afterIfbb;
            IRBuilder.getInstance().buildBasicBlock(afterIfbb, curFunction);
        } else if (firstChild.getGrammarType() == LexType.FORTK) {
            BasicBlock condbb = null;
            BasicBlock forStmt2bb = null;
            BasicBlock stmtbb = null;
            int forStmt1Index = -1;
            int condIndex = -1;
            int forStmt2Index = -1;
            int stmtIndex = -1;
            for (int i = 0; i < thisNode.getChildren().size(); i++) {
                ASTNode child = thisNode.getChildren().get(i);
                if (child.getGrammarType() == NonTermType.FORSTMT) {
                    if (i == 2) {
                        forStmt1Index = i;
                    } else {
                        forStmt2bb = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
                        forStmt2Index = i;
                    }
                } else if (child.getGrammarType() == NonTermType.COND) {
                    condbb = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
                    condIndex = i;
                } else if (child.getGrammarType() == NonTermType.STMT) {
                    stmtbb = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
                    stmtIndex = i;
                }
            }
            assert stmtbb != null;
            BasicBlock afterForbb = IRBuilder.getInstance().buildBasicBlock(false, curFunction);
            if (forStmt1Index != -1) {
                curASTNode = thisNode.getChildren().get(forStmt1Index);
                visitForStmt();
                curASTNode = thisNode;
                BasicBlock after = condbb == null ? stmtbb : condbb;
                if (curBasicBlock.getIRInstrs().isEmpty()
                        || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                        && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                    IRBuilder.getInstance().buildBrIRInstr(after, curBasicBlock);
                }
            }
            if (condbb != null) {
                if (curBasicBlock.getIRInstrs().isEmpty()
                        || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                        && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                    IRBuilder.getInstance().buildBrIRInstr(condbb, curBasicBlock);
                }
                curBasicBlock = condbb;
                curASTNode = thisNode.getChildren().get(condIndex);
                visitCond(stmtbb, afterForbb, afterForbb);
                curASTNode = thisNode;
            } else if (curBasicBlock.getIRInstrs().isEmpty()
                    || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                    && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                IRBuilder.getInstance().buildBrIRInstr(stmtbb, curBasicBlock);
            }
            if (forStmt2bb != null) {
                curBasicBlock = forStmt2bb;
                curASTNode = thisNode.getChildren().get(forStmt2Index);
                visitForStmt();
                curASTNode = thisNode;
                BasicBlock after = condbb == null ? stmtbb : condbb;
                if (curBasicBlock.getIRInstrs().isEmpty()
                        || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                        && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                    IRBuilder.getInstance().buildBrIRInstr(after, curBasicBlock);
                }
            }
            BasicBlock after = forStmt2bb != null ? forStmt2bb : condbb != null ? condbb : stmtbb;
            continueBrbb.add(after);
            breakBrbb.add(afterForbb);
            curBasicBlock = stmtbb;
            curASTNode = thisNode.getChildren().get(stmtIndex);
            visitStmt();
            curASTNode = thisNode;
            if (curBasicBlock.getIRInstrs().isEmpty()
                    || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                    && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                IRBuilder.getInstance().buildBrIRInstr(after, curBasicBlock);
            }
            continueBrbb.remove(continueBrbb.size() - 1);
            breakBrbb.remove(breakBrbb.size() - 1);
            curBasicBlock = afterForbb;
            IRBuilder.getInstance().buildBasicBlock(afterForbb, curFunction);
        } else if (firstChild.getGrammarType() == LexType.BREAKTK) {
            assert !breakBrbb.isEmpty();
            if (curBasicBlock.getIRInstrs().isEmpty()
                    || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                    && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                IRBuilder.getInstance().buildBrIRInstr(breakBrbb.get(breakBrbb.size() - 1), curBasicBlock);
            }
        } else if (firstChild.getGrammarType() == LexType.CONTINUETK) {
            assert !continueBrbb.isEmpty();
            if (curBasicBlock.getIRInstrs().isEmpty()
                    || curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR
                    && curBasicBlock.getIRInstrs().get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.RET) {
                IRBuilder.getInstance().buildBrIRInstr(continueBrbb.get(continueBrbb.size() - 1), curBasicBlock);
            }
        } else if (firstChild.getGrammarType() == NonTermType.EXP) {
            curASTNode = firstChild;
            visitExp(true);
            curASTNode = thisNode;
        }
    }

    private void visitForStmt() {
        ASTNode thisNode = curASTNode;
        assert thisNode.getChildren().get(0).getGrammarType() == NonTermType.LVAL;
        curASTNode = thisNode.getChildren().get(0);
        Value lvalue = visitLVal(false);
        curASTNode = thisNode;
        Value value;
        curASTNode = thisNode.getChildren().get(2);
        value = visitExp(true);
        curASTNode = thisNode;
        assert lvalue != null;
        assert value != null;
        if (lvalue instanceof Pointer) {
            IRBuilder.getInstance().buildStoreIRInstr((Pointer) lvalue, value, curBasicBlock);
        } else {
            Pointer ptr = new Pointer(lvalue.getToken(), lvalue.getTemp(), false, lvalue);
            IRBuilder.getInstance().buildStoreIRInstr(ptr, value, curBasicBlock);
        }
    }

    private Value visitConstExp(boolean alloca) {
        ASTNode thisNode = curASTNode;
        assert thisNode.getChildren().get(0).getGrammarType() == NonTermType.ADDEXP;
        curASTNode = thisNode.getChildren().get(0);
        Value value = visitAddExp(alloca);
        curASTNode = thisNode;
        return value;
    }

    private Value visitAddExp(boolean alloca) {
        ASTNode thisNode = curASTNode;
        if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.ADDEXP) {
            curASTNode = thisNode.getChildren().get(0);
            Value value1 = visitAddExp(alloca);
            curASTNode = thisNode.getChildren().get(2);
            Value value2 = visitMulExp(alloca);
            curASTNode = thisNode;
            if (thisNode.getChildren().get(1).getGrammarType() == LexType.PLUS) {
                return IRBuilder.getInstance().buildCalculateIRInstr("+", value1, value2, curBasicBlock, alloca);
            } else {
                return IRBuilder.getInstance().buildCalculateIRInstr("-", value1, value2, curBasicBlock, alloca);
            }
        } else {
            curASTNode = thisNode.getChildren().get(0);
            Value value = visitMulExp(alloca);
            curASTNode = thisNode;
            return value;
        }
    }

    private Value visitMulExp(boolean alloca) {
        ASTNode thisNode = curASTNode;
        if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.MULEXP) {
            curASTNode = thisNode.getChildren().get(0);
            Value value1 = visitMulExp(alloca);
            curASTNode = thisNode.getChildren().get(2);
            Value value2 = visitUnaryExp(alloca);
            curASTNode = thisNode;
            if (thisNode.getChildren().get(1).getGrammarType() == LexType.MULT) {
                return IRBuilder.getInstance().buildCalculateIRInstr("*", value1, value2, curBasicBlock, alloca);
            } else if (thisNode.getChildren().get(1).getGrammarType() == LexType.DIV) {
                return IRBuilder.getInstance().buildCalculateIRInstr("/", value1, value2, curBasicBlock, alloca);
            } else {
                return IRBuilder.getInstance().buildCalculateIRInstr("%", value1, value2, curBasicBlock, alloca);
            }
        } else {
            curASTNode = thisNode.getChildren().get(0);
            Value value = visitUnaryExp(alloca);
            curASTNode = thisNode;
            return value;
        }
    }

    private Value visitUnaryExp(boolean alloca) {
        ASTNode thisNode = curASTNode;
        if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.UNARYOP) {
            Value value1 = new Value(null, ValueType.INT, "0");
            curASTNode = thisNode.getChildren().get(1);
            Value value2 = visitUnaryExp(alloca);
            curASTNode = thisNode;
            if (thisNode.getChildren().get(0).getChildren().get(0).getGrammarType() == LexType.PLUS) {
                return value2;
            } else if (thisNode.getChildren().get(0).getChildren().get(0).getGrammarType() == LexType.MINU) {
                return IRBuilder.getInstance().buildCalculateIRInstr("-", value1, value2, curBasicBlock, alloca);
            } else if (thisNode.getChildren().get(0).getChildren().get(0).getGrammarType() == LexType.NOT) {
                if (value2.isImm()) {
                    if (Integer.parseInt(value2.getTemp()) == 0) {
                        return new Value(null, ValueType.INT, "1");
                    } else {
                        return new Value(null, ValueType.INT, "0");
                    }
                } else {
                    return IRBuilder.getInstance().buildICmpIRInstr("eq", value1, value2, curBasicBlock);
                }
            } else {
                return null;
            }
        } else if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.PRIMARYEXP) {
            ASTNode primaryExp = thisNode.getChildren().get(0);
            if (primaryExp.getChildren().get(0).getGrammarType() == NonTermType.NUMBER) {
                String number = ((TermNode) primaryExp.getChildren().get(0).getChildren().get(0)).getToken();
                return new Value(null, ValueType.INT, number);
            } else if (primaryExp.getChildren().get(0).getGrammarType() == NonTermType.LVAL) {
                curASTNode = primaryExp.getChildren().get(0);
                Value value = visitLVal(true);
                curASTNode = thisNode;
                return value;
            } else {
                curASTNode = primaryExp.getChildren().get(1);
                Value value = visitExp(alloca);
                curASTNode = thisNode;
                return value;
            }
        } else {
            String token = ((TermNode) thisNode.getChildren().get(0)).getToken();
            Value function = SymbolIRManager.getInstance().findSymbol(token, true);
            assert function instanceof Function;
            if (thisNode.getChildren().get(2).getGrammarType() == NonTermType.FUNCRPARAMS) {
                curASTNode = thisNode.getChildren().get(2);
                ArrayList<Value> paras = visitFuncRParas(alloca);
                curASTNode = thisNode;
                return IRBuilder.getInstance().buildCallIRInstr(function.getTemp()
                        , (Function) function, paras, curBasicBlock);
            } else {
                return IRBuilder.getInstance().buildCallIRInstr(function.getTemp()
                        , (Function) function, new ArrayList<>(), curBasicBlock);
            }
        }
    }

    private Value visitExp(boolean alloca) {
        //currently same as constExp, need to be changed when optimizing
        ASTNode thisNode = curASTNode;
        assert thisNode.getChildren().get(0).getGrammarType() == NonTermType.ADDEXP;
        curASTNode = thisNode.getChildren().get(0);
        Value value = visitAddExp(alloca);
        curASTNode = thisNode;
        return value;
    }

    private Value visitLVal(boolean load) {
        ASTNode thisNode = curASTNode;
        assert thisNode.getChildren().get(0).getGrammarType() == LexType.IDENFR;
        String token = ((TermNode) thisNode.getChildren().get(0)).getToken();
        Value lvalue = SymbolIRManager.getInstance().findSymbol(token, true);
        if (lvalue.getType() == ValueType.INT) {
            if (lvalue.isImm() || !load || curBasicBlock == null) {
                if (lvalue instanceof GlobalDecl && load) {
                    return new Object(null, ValueType.INT, ((GlobalDecl) lvalue).getInitVal(), false);
                }
                return lvalue;
            } else {
                assert lvalue instanceof Object;
                Pointer ptr = new Pointer(lvalue.getToken(), lvalue.getTemp(), ((Object) lvalue).isConst(), lvalue);
                return IRBuilder.getInstance().buildLoadIRInstr(ptr, curBasicBlock);
            }
        } else if (curBasicBlock == null) {
            if (thisNode.getChildren().size() == 1) {
                return lvalue;
            } else if (thisNode.getChildren().size() == 4) {
                curASTNode = thisNode.getChildren().get(2);
                Value dim1 = visitExp(true);
                curASTNode = thisNode;
                assert lvalue instanceof Array;
                assert lvalue.isImm() && dim1.isImm();
                return new Object(null, ValueType.INT, String.valueOf(lvalue.getImm(dim1.getImm(), -1)), false);
            } else {
                curASTNode = thisNode.getChildren().get(2);
                Value dim1 = visitExp(true);
                curASTNode = thisNode;
                curASTNode = thisNode.getChildren().get(5);
                Value dim2 = visitExp(true);
                curASTNode = thisNode;
                assert lvalue instanceof Array;
                assert lvalue.isImm() && dim1.isImm() && dim2.isImm();
                return new Object(null, ValueType.INT, String.valueOf(lvalue.getImm(dim1.getImm(), dim2.getImm())), false);
            }
        } else if (!load) {
            Pointer ptr = new Pointer(lvalue.getToken(), lvalue.getTemp(), ((Object) lvalue).isConst(), lvalue);
            if (thisNode.getChildren().size() == 1) {
                return lvalue;
            } else if (thisNode.getChildren().size() == 4) {
                curASTNode = thisNode.getChildren().get(2);
                Value dim1 = visitExp(true);
                curASTNode = thisNode;
                ArrayList<String> offset = new ArrayList<>();
                if (lvalue instanceof Array) {
                    if (lvalue.isImm() && dim1.isImm()) {
                        return new Object(null, ValueType.INT, String.valueOf(lvalue.getImm(dim1.getImm(), -1)), false);
                    }
                    offset.add("0");
                    offset.add(dim1.getTemp());
                    return IRBuilder.getInstance().buildGepIRInstr((Object) lvalue, offset, curBasicBlock);
                } else {
                    Object object = IRBuilder.getInstance().buildLoadIRInstr(ptr, curBasicBlock);
                    offset.add(dim1.getTemp());
                    return IRBuilder.getInstance().buildGepIRInstr(object, offset, curBasicBlock);//pointer
                }
            } else {
                curASTNode = thisNode.getChildren().get(2);
                Value dim1 = visitExp(true);
                curASTNode = thisNode;
                curASTNode = thisNode.getChildren().get(5);
                Value dim2 = visitExp(true);
                curASTNode = thisNode;
                ArrayList<String> offset = new ArrayList<>();
                if (lvalue instanceof Array) {
                    if (lvalue.isImm() && dim1.isImm() && dim2.isImm()) {
                        return new Object(null, ValueType.INT, String.valueOf(lvalue.getImm(dim1.getImm(), dim2.getImm())), false);
                    }
                    offset.add("0");
                    offset.add(dim1.getTemp());
                    offset.add(dim2.getTemp());
                    return IRBuilder.getInstance().buildGepIRInstr((Object) lvalue, offset, curBasicBlock);
                } else {
                    Object object = IRBuilder.getInstance().buildLoadIRInstr(ptr, curBasicBlock);
                    offset.add(dim1.getTemp());
                    Pointer gep = IRBuilder.getInstance().buildGepIRInstr(object, offset, curBasicBlock);//pointer
                    offset.clear();
                    offset.add("0");
                    offset.add(dim2.getTemp());
                    return IRBuilder.getInstance().buildGepIRInstr(gep, offset, curBasicBlock);//pointer;
                }
            }
        } else if (lvalue.getType() == ValueType.PTR) {
            Pointer ptr = new Pointer(lvalue.getToken(), lvalue.getTemp(), ((Object) lvalue).isConst(), lvalue);
            Object object = IRBuilder.getInstance().buildLoadIRInstr(ptr, curBasicBlock);
            if (!(object instanceof Pointer) && !(object instanceof Array)) {
                return object;
            }
            if (thisNode.getChildren().size() == 1) {
                return object;
            } else if (thisNode.getChildren().size() == 4) {
                curASTNode = thisNode.getChildren().get(2);
                Value dim1 = visitExp(true);
                curASTNode = thisNode;
                ArrayList<String> offset = new ArrayList<>();
                offset.add(dim1.getTemp());
                Pointer gep = IRBuilder.getInstance().buildGepIRInstr(object, offset, curBasicBlock);//pointer
                if (((Pointer) lvalue).getReference().getType() == ValueType.INT) {
                    return IRBuilder.getInstance().buildLoadIRInstr(gep, curBasicBlock);
                } else {
                    offset.clear();
                    offset.add("0");
                    offset.add("0");
                    return IRBuilder.getInstance().buildGepIRInstr(gep, offset, curBasicBlock);//pointer
                }
            } else {
                curASTNode = thisNode.getChildren().get(2);
                Value dim1 = visitExp(true);
                curASTNode = thisNode;
                curASTNode = thisNode.getChildren().get(5);
                Value dim2 = visitExp(true);
                curASTNode = thisNode;
                ArrayList<String> offset = new ArrayList<>();
                offset.add(dim1.getTemp());
                Pointer gep1 = IRBuilder.getInstance().buildGepIRInstr(object, offset, curBasicBlock);//pointer
                offset.clear();
                offset.add("0");
                offset.add(dim2.getTemp());
                Pointer gep2 = IRBuilder.getInstance().buildGepIRInstr(gep1, offset, curBasicBlock);//pointer
                return IRBuilder.getInstance().buildLoadIRInstr(gep2, curBasicBlock);
            }
        } else if (thisNode.getChildren().size() == 1) {
            ArrayList<String> offset = new ArrayList<>();
            offset.add("0");
            offset.add("0");
            return IRBuilder.getInstance().buildGepIRInstr((Object) lvalue, offset, curBasicBlock);
        } else if (thisNode.getChildren().size() == 4) {
            curASTNode = thisNode.getChildren().get(2);
            Value dim1 = visitExp(true);
            curASTNode = thisNode;
            if (lvalue.isImm() && dim1.isImm()) {
                return new Object(null, ValueType.INT, String.valueOf(lvalue.getImm(dim1.getImm(), -1)), false);
            }
            ArrayList<String> offset = new ArrayList<>();
            if (((Array) lvalue).getDim() == 1) {
                offset.add("0");
                offset.add(dim1.getTemp());
                Pointer gep = IRBuilder.getInstance().buildGepIRInstr((Object) lvalue, offset, curBasicBlock);
                return IRBuilder.getInstance().buildLoadIRInstr(gep, curBasicBlock);
            } else {
                offset.add("0");
                offset.add(dim1.getTemp());
                offset.add("0");
                return IRBuilder.getInstance().buildGepIRInstr((Object) lvalue, offset, curBasicBlock);
            }
        } else {
            curASTNode = thisNode.getChildren().get(2);
            Value dim1 = visitExp(true);
            curASTNode = thisNode;
            curASTNode = thisNode.getChildren().get(5);
            Value dim2 = visitExp(true);
            curASTNode = thisNode;
            if (lvalue.isImm() && dim1.isImm() && dim2.isImm()) {
                return new Object(null, ValueType.INT, String.valueOf(lvalue.getImm(dim1.getImm(), dim2.getImm())), false);
            }
            ArrayList<String> offset = new ArrayList<>();
            offset.add("0");
            offset.add(dim1.getTemp());
            offset.add(dim2.getTemp());
            Pointer gep = IRBuilder.getInstance().buildGepIRInstr((Object) lvalue, offset, curBasicBlock);
            return IRBuilder.getInstance().buildLoadIRInstr(gep, curBasicBlock);
        }
    }

    private ArrayList<Value> visitFuncRParas(boolean alloca) {
        ASTNode thisNode = curASTNode;
        ArrayList<Value> ret = new ArrayList<>();
        for (int i = 0; i < thisNode.getChildren().size(); i += 2) {
            curASTNode = thisNode.getChildren().get(i);
            Value value = visitExp(alloca);
            curASTNode = thisNode;
            ret.add(value);
        }
        return ret;
    }

    private void visitCond(BasicBlock stmt1bb, BasicBlock stmt2bb, BasicBlock afterIfbb) {
        ASTNode thisNode = curASTNode;
        truebb = stmt1bb;
        falsebb = stmt2bb != null ? stmt2bb : afterIfbb;
        curASTNode = thisNode.getChildren().get(0);
        visitLOrExp();
        curASTNode = thisNode;
        truebb = null;
        falsebb = null;
    }

    private void visitLOrExp() {
        ASTNode thisNode = curASTNode;
        if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.LOREXP) {
            BasicBlock thisFalsebb = falsebb;
            falsebb = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
            curASTNode = thisNode.getChildren().get(0);
            visitLOrExp();
            curASTNode = thisNode;
            curBasicBlock = falsebb;
            falsebb = thisFalsebb;
            curASTNode = thisNode.getChildren().get(2);
            visitLAndExp();
            curASTNode = thisNode;
        } else {
            curASTNode = thisNode.getChildren().get(0);
            visitLAndExp();
            curASTNode = thisNode;
        }
    }

    private void visitLAndExp() {
        ASTNode thisNode = curASTNode;
        Value value;
        if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.LANDEXP) {
            BasicBlock thisTruebb = truebb;
            truebb = IRBuilder.getInstance().buildBasicBlock(true, curFunction);
            curASTNode = thisNode.getChildren().get(0);
            visitLAndExp();
            curASTNode = thisNode;
            curBasicBlock = truebb;
            truebb = thisTruebb;
            curASTNode = thisNode.getChildren().get(2);
        } else {
            curASTNode = thisNode.getChildren().get(0);
        }
        value = visitEqExp();
        curASTNode = thisNode;
        if (value.isImm()) {
            String i1 = Integer.parseInt(value.getTemp()) == 0 ? "0" : "1";
            Value cmp = new Value(null, ValueType.I1, i1);
            if (curBasicBlock.getIRInstrs().isEmpty() || curBasicBlock.getIRInstrs()
                    .get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR) {
                IRBuilder.getInstance().buildBrIRInstr(cmp, truebb, falsebb, curBasicBlock);
            }
        } else {
            Value cmp = IRBuilder.getInstance().buildICmpIRInstr("ne", value,
                    new Value(null, ValueType.INT, "0"), curBasicBlock);
            if (curBasicBlock.getIRInstrs().isEmpty() || curBasicBlock.getIRInstrs()
                    .get(curBasicBlock.getIRInstrs().size() - 1).getInstrType() != IRInstrType.BR) {
                IRBuilder.getInstance().buildBrIRInstr(cmp, truebb, falsebb, curBasicBlock);
            }
        }
    }

    public Value visitEqExp() {
        ASTNode thisNode = curASTNode;
        if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.EQEXP) {
            curASTNode = thisNode.getChildren().get(0);
            Value value1 = visitEqExp();
            curASTNode = thisNode.getChildren().get(2);
            Value value2 = visitRelExp();
            curASTNode = thisNode;
            if (value1.isImm() && value2.isImm()) {
                String isEq = Integer.parseInt(value1.getTemp()) == Integer.parseInt(value2.getTemp()) ? "1" : "0";
                String isNeq = isEq.equals("1") ? "0" : "1";
                if (thisNode.getChildren().get(1).getGrammarType() == LexType.EQL) {
                    return new Value(null, ValueType.INT, isEq);
                } else {
                    return new Value(null, ValueType.INT, isNeq);
                }
            } else {
                Value cmp;
                if (thisNode.getChildren().get(1).getGrammarType() == LexType.EQL) {
                    cmp = IRBuilder.getInstance().buildICmpIRInstr("eq", value1, value2, curBasicBlock);
                } else {
                    cmp = IRBuilder.getInstance().buildICmpIRInstr("ne", value1, value2, curBasicBlock);
                }
                return cmp;
            }
        } else {
            curASTNode = thisNode.getChildren().get(0);
            Value value = visitRelExp();
            curASTNode = thisNode;
            return value;
        }
    }

    private Value visitRelExp() {
        ASTNode thisNode = curASTNode;
        if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.RELEXP) {
            curASTNode = thisNode.getChildren().get(0);
            Value value1 = visitRelExp();
            curASTNode = thisNode.getChildren().get(2);
            Value value2 = visitAddExp(true);
            curASTNode = thisNode;
            if (value1.isImm() && value2.isImm()) {
                if (thisNode.getChildren().get(1).getGrammarType() == LexType.GEQ) {
                    if (Integer.parseInt(value1.getTemp()) >= Integer.parseInt(value2.getTemp())) {
                        return new Value(null, ValueType.INT, "1");
                    } else {
                        return new Value(null, ValueType.INT, "0");
                    }
                } else if (thisNode.getChildren().get(1).getGrammarType() == LexType.LEQ) {
                    if (Integer.parseInt(value1.getTemp()) <= Integer.parseInt(value2.getTemp())) {
                        return new Value(null, ValueType.INT, "1");
                    } else {
                        return new Value(null, ValueType.INT, "0");
                    }
                } else if (thisNode.getChildren().get(1).getGrammarType() == LexType.GRE) {
                    if (Integer.parseInt(value1.getTemp()) > Integer.parseInt(value2.getTemp())) {
                        return new Value(null, ValueType.INT, "1");
                    } else {
                        return new Value(null, ValueType.INT, "0");
                    }
                } else {
                    if (Integer.parseInt(value1.getTemp()) < Integer.parseInt(value2.getTemp())) {
                        return new Value(null, ValueType.INT, "1");
                    } else {
                        return new Value(null, ValueType.INT, "0");
                    }
                }
            } else {
                Value cmp;
                if (thisNode.getChildren().get(1).getGrammarType() == LexType.GEQ) {
                    cmp = IRBuilder.getInstance().buildICmpIRInstr("sge", value1, value2, curBasicBlock);
                } else if (thisNode.getChildren().get(1).getGrammarType() == LexType.LEQ) {
                    cmp = IRBuilder.getInstance().buildICmpIRInstr("sle", value1, value2, curBasicBlock);
                } else if (thisNode.getChildren().get(1).getGrammarType() == LexType.GRE) {
                    cmp = IRBuilder.getInstance().buildICmpIRInstr("sgt", value1, value2, curBasicBlock);
                } else {
                    cmp = IRBuilder.getInstance().buildICmpIRInstr("slt", value1, value2, curBasicBlock);
                }
                return cmp;
            }
        } else {
            curASTNode = thisNode.getChildren().get(0);
            Value value = visitAddExp(true);
            curASTNode = thisNode;
            return value;
        }
    }
}
