package IRBuilder.Symbol;

import IRBuilder.Value.Function;
import IRBuilder.Value.Value;

import java.util.ArrayList;

public class SymbolIRManager {
    private static volatile SymbolIRManager instance;
    private final SymbolIRTable rootTable;
    private SymbolIRTable curTable;
    private SymbolIRTable tempParaTable;
    private String tempFuncToken;
    private ArrayList<String> tempParaToken;

    private SymbolIRManager() {
        this.rootTable = new SymbolIRTable(null);
        this.tempParaTable = null;
        this.tempParaToken = null;
        this.tempFuncToken = null;
        this.curTable = rootTable;
    }

    public static SymbolIRManager getInstance() {
        if (instance == null) {
            instance = new SymbolIRManager();
        }
        return instance;
    }

    public void registerSymbol(String token, Value value) {
        if (value instanceof Function) {
            tempFuncToken = token;
            tempParaTable = new SymbolIRTable(null);
            tempParaToken = new ArrayList<>();
        }
        curTable.registerSymbol(token, value);
    }

    public void registerPara(String token, Value value) {
        assert tempParaTable != null;
        tempParaToken.add(token);
        tempParaTable.registerSymbol(token, value);
    }

    public Value findSymbol(String token, boolean recursively) {
        if (tempParaTable == null) {
            return curTable.findSymbol(token, recursively);
        } else {
            return tempParaTable.findSymbol(token, false);
        }
    }

    public SymbolIRTable proceedScope() {
        curTable = new SymbolIRTable(curTable);
        curTable.getPrev().addSubs(curTable);
        if (tempParaTable != null) {
            curTable.registerPara(tempParaTable, tempParaToken, tempFuncToken);
        }
        tempFuncToken = null;
        tempParaToken = null;
        tempParaTable = null;
        return curTable;
    }

    public SymbolIRTable retrieveScope() {
        return curTable = curTable.getPrev();
    }
}
