package Type;

public enum ValueType {
    VOID("void"),
    INT("i32"),
    I1("i1"),
    PTR(null),
    ARRAY(null),
    IRINSTR(null),
    BASICBLOCK(null),
    FUNCTION(null);

    private final String description;

    private ValueType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
