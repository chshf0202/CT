package Error.Symbol;

import Type.EntityType;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    private static int n = 0;
    private final int id;
    private final SymbolTable prev;
    private final HashMap<String, Symbol> symbols;
    private final ArrayList<SymbolTable> subs;

    public SymbolTable(SymbolTable prev) {
        this.id = genId();
        this.prev = prev;
        this.symbols = new HashMap<>();
        this.subs = new ArrayList<>();
    }

    private int genId() {
        n++;
        return n;
    }

    public SymbolTable getPrev() {
        return prev;
    }

    public void addSubs(SymbolTable subs) {
        this.subs.add(subs);
    }

    public boolean isEmpty() {
        return symbols.isEmpty();
    }

    public Symbol registerSymbol(String token, EntityType type, String... s) {
        if (type == EntityType.FUNC) {
            EntityType returnType = EntityType.string2Entity(s[0]);
            int paraNum = Integer.parseInt(s[1]);
            SymbolFunction symbolFunction = new SymbolFunction(id, token, returnType, paraNum);
            symbols.put(token, symbolFunction);
            return symbolFunction;
        } else if (type == EntityType.ARRAY) {
            boolean isConst = Boolean.parseBoolean(s[0]);
            int dim = Integer.parseInt(s[1]);
            int[] size = new int[dim];
//            for (int i = 0; i < dim; i++) {
//                size[i] = Integer.parseInt(s[i + 2]);
//            }
            SymbolArray symbolArray = new SymbolArray(id, token, type, isConst, dim, size);
            symbols.put(token, symbolArray);
            return symbolArray;
        } else {
            boolean isConst = Boolean.parseBoolean(s[0]);
            SymbolObject symbolObject = new SymbolObject(id, token, type, isConst);
            symbols.put(token, symbolObject);
            return symbolObject;
        }
    }

    public void registerPara(SymbolTable paraTable, ArrayList<String> parasToken, String funcToken) {
        Symbol func = prev.symbols.get(funcToken);
        for (String token : parasToken) {
            symbols.put(token, paraTable.symbols.get(token));
            assert paraTable.symbols.get(token) instanceof SymbolObject;
            ((SymbolFunction) func).addPara(((SymbolObject) paraTable.symbols.get(token)));
        }
        paraTable.symbols.clear();
        parasToken.clear();
    }

    public Symbol findSymbol(String token, boolean recursively) {
        if (symbols.containsKey(token)) {
            return symbols.get(token);
        } else if (recursively && prev != null) {
            return prev.findSymbol(token, true);
        } else {
            return null;
        }
    }

}
