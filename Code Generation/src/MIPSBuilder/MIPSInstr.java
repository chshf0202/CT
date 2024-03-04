package MIPSBuilder;

import Type.MIPSInstrType;

public class MIPSInstr {
    private MIPSInstrType instrType;
    private String mips;

    public MIPSInstr(MIPSInstrType instrType, String mips) {
        this.instrType = instrType;
        this.mips = mips;
    }

    public MIPSInstrType getInstrType() {
        return instrType;
    }

    public String getMIPS() {
        return mips;
    }
}
