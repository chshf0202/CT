package IRBuilder.Symbol;

import Error.Symbol.*;
import IRBuilder.Value.Function;
import IRBuilder.Value.Value;
import Type.EntityType;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolIRTable {
    private static int n = 0;
    private final int id;
    private final SymbolIRTable prev;
    private final HashMap<String, Value> symbols;
    private final ArrayList<SymbolIRTable> subs;

    public SymbolIRTable(SymbolIRTable prev) {
        this.id = genId();
        this.prev = prev;
        this.symbols = new HashMap<>();
        this.subs = new ArrayList<>();
    }

    private int genId() {
        n++;
        return n;
    }

    public SymbolIRTable getPrev() {
        return prev;
    }

    public void addSubs(SymbolIRTable subs) {
        this.subs.add(subs);
    }

    public boolean isEmpty() {
        return symbols.isEmpty();
    }

    public void registerSymbol(String token, Value value) {
        symbols.put(token, value);
    }

    public void registerPara(SymbolIRTable paraTable, ArrayList<String> parasToken, String funcToken) {
        Value func = prev.symbols.get(funcToken);
        for (String token : parasToken) {
            symbols.put(token, paraTable.symbols.get(token));
            ((Function) func).addPara(paraTable.symbols.get(token));
        }
        paraTable.symbols.clear();
        parasToken.clear();
    }

    public Value findSymbol(String token, boolean recursively) {
        if (symbols.containsKey(token)) {
            return symbols.get(token);
        } else if (recursively && prev != null) {
            return prev.findSymbol(token, true);
        } else {
            return null;
        }
    }
}
