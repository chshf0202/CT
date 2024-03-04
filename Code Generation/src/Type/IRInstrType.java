package Type;

import IRBuilder.Value.IRInstr;

public enum IRInstrType {
    ADD("add"),
    SUB("sub"),
    MUL("mul"),
    SDIV("sdiv"),
    SREM("srem"),
    ICMP("icmp"),
    ZEXT("zext"),
    CALL("call"),
    ALLOCA("alloca"),
    LOAD("load"),
    STORE("store"),
    GEP("getelementptr"),
    BR("br"),
    RET("ret");
    private final String description;

    private IRInstrType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
