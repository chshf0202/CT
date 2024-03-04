package Error.Symbol;

import Type.EntityType;

public class Symbol {
    private final int tableId;
    private final String token;
    private final EntityType type;

    public Symbol(int tableId, String token, EntityType type) {
        this.tableId = tableId;
        this.token = token;
        this.type = type;
    }

    public EntityType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }
}
