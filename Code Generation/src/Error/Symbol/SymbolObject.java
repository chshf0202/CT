package Error.Symbol;

import Type.EntityType;

public class SymbolObject extends Symbol {
    private final boolean isConst;

    public SymbolObject(int tableId, String token, EntityType type, boolean isConst) {
        super(tableId, token, type);
        this.isConst = isConst;
    }

    public boolean isConst() {
        return isConst;
    }
}
