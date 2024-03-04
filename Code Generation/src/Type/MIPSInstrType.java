package Type;

public enum MIPSInstrType {
    WORD(".word"),
    ASCIIZ(".asciiz"),
    LABEL(null),
    LW("lw"),
    LI("li"),
    LA("la"),
    SW("sw"),
    ADDIU("addiu"),
    ADDU("addu"),
    SUBU("sub"),
    SLL("sll"),
    MUL("mul"),
    DIV("div"),
    MFHI("mfhi"),
    MFLO("mflo"),
    SLT("slt"),
    SLTI("slti"),
    SLTU("sltu"),
    SLTIU("sltiu"),
    SLE("sle"),
    BEQ("beq"),
    J("j"),
    JAL("jal"),
    JR("jr"),
    SYSCALL("syscall");


    private final String description;

    private MIPSInstrType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
