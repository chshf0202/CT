package Error.Symbol;

import Type.EntityType;

import java.util.ArrayList;

public class SymbolArray extends SymbolObject {
    private final int dim;
    private final ArrayList<Integer> size;

    public SymbolArray(int tableId, String token, EntityType type, boolean isConst, int dim, int... size) {
        super(tableId, token, type, isConst);
        this.dim = dim;
        this.size = new ArrayList<>();
//        for (int i = 0; i < dim; i++) {
//            this.size.add(size[i]);
//        }
    }

    public int getDim() {
        return dim;
    }
}
