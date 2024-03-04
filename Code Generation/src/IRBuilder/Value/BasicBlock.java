package IRBuilder.Value;

import Type.ValueType;

import java.util.ArrayList;

public class BasicBlock extends Value {
    private Function function;
    private ArrayList<IRInstr> IRInstrs;

    public BasicBlock(String temp, Function function) {
        super(null, ValueType.BASICBLOCK, temp);
        this.IRInstrs = new ArrayList<>();
        this.function = function;
    }

    public void addIRInstr(IRInstr irInstr) {
        this.IRInstrs.add(irInstr);
    }

    public int newValueNo() {
        return function.newValueNo();
    }

    public ArrayList<IRInstr> getIRInstrs() {
        return IRInstrs;
    }
}
