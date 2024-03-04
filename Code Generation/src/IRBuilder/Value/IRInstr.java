package IRBuilder.Value;

import Type.IRInstrType;
import Type.ValueType;

public class IRInstr extends Value {
    private IRInstrType instrType;
    private String IR;

    public IRInstr(IRInstrType instrType, String IR) {
        super(null, ValueType.IRINSTR, null);
        this.instrType = instrType;
        this.IR = IR;
    }

    public String getIR() {
        return IR;
    }

    public IRInstrType getInstrType() {
        return instrType;
    }
}
