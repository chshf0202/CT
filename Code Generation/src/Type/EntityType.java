package Type;

public enum EntityType {
    INT("int"),
    VOID("void"),
    FUNC(null),
    ARRAY(null);


    private final String description;

    private EntityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static EntityType string2Entity(String s) {
        for (EntityType type : EntityType.values()) {
            if (s.equals(type.getDescription())) {
                return type;
            }
        }
        return null;
    }
}