package MIPSBuilder.Symbol;

import java.util.HashMap;
import java.util.HashSet;

public class SymbolMIPSManager {
    private static volatile SymbolMIPSManager instance;
    private HashMap<String, String> symbols;
    private HashSet<String> localPointer;
    private HashSet<String> functionPara;


    private SymbolMIPSManager() {
        this.symbols = new HashMap<>();
        this.localPointer = new HashSet<>();
        this.functionPara = new HashSet<>();
    }

    public static SymbolMIPSManager getInstance() {
        if (instance == null) {
            instance = new SymbolMIPSManager();
        }
        return instance;
    }

    public void register(String token, String symbol) {
        this.symbols.put(token, symbol);
    }

    public String find(String token) {
        return this.symbols.getOrDefault(token, null);
    }

    public void addPointer(String pointer) {
        this.localPointer.add(pointer);
    }

    public boolean isPointer(String object) {
        return this.localPointer.contains(object);
    }

    public void addPara(String para) {
        this.functionPara.add(para);
    }

    public boolean isPara(String object) {
        return this.functionPara.contains(object);
    }
}
