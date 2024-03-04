package IRBuilder.Value;

import Type.IRInstrType;
import Type.ValueType;

import java.util.ArrayList;

public class Function extends Value {
    private ValueType retValueType;
    private ArrayList<Value> paras;
    private ArrayList<BasicBlock> basicBlocks;
    private int valueNo;
    private int bbNo;

    public Function(String token, ValueType retValueType) {
        super(token, ValueType.FUNCTION, "@" + token);
        this.retValueType = retValueType;
        this.paras = new ArrayList<>();
        this.basicBlocks = new ArrayList<>();
        this.valueNo = 0;
        this.bbNo = 0;
    }

    public void addPara(Value para) {
        paras.add(para);
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        this.basicBlocks.add(basicBlock);
    }

    public int newValueNo() {
        int ans = valueNo;
        valueNo++;
        return ans;
    }

    public int newbbNo() {
        int ans = bbNo;
        bbNo++;
        return ans;
    }

    public ValueType getRetValueType() {
        return retValueType;
    }

    public String getIR() {
        String type = retValueType == ValueType.INT ? "i32" : "void";
        StringBuilder IR = new StringBuilder("define dso_local " + type + " " + this.getTemp() + "(");
        if (!paras.isEmpty()) {
            for (int i = 0; i < paras.size(); i++) {
                Value para = paras.get(i);
                String paraType;
                if (para instanceof Pointer) {
                    paraType = ((Pointer) para).getPtrStruct();
                } else if (para instanceof Array) {
                    paraType = ((Array) para).getArrayStruct();
                } else {
                    paraType = para.getType().getDescription();
                }
                IR.append(paraType).append(" %para").append(i).append(", ");
            }
            IR = new StringBuilder(IR.substring(0, IR.length() - 2) + "){");
        } else {
            IR.append("){");
        }
        return IR.toString();
    }

    public ArrayList<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    public boolean hasBuiltRet() {
        BasicBlock lastbb = basicBlocks.get(basicBlocks.size() - 1);
        if (lastbb.getIRInstrs().isEmpty()) {
            return false;
        }
        IRInstr lastInstr = lastbb.getIRInstrs().get(lastbb.getIRInstrs().size() - 1);
        return lastInstr.getInstrType() == IRInstrType.RET;
    }

    public ArrayList<Value> getParas() {
        return paras;
    }
}
