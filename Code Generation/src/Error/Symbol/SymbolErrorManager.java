package Error.Symbol;

import Type.EntityType;

import java.util.ArrayList;

public class SymbolErrorManager {
    private static volatile SymbolErrorManager instance;
    private final SymbolTable rootTable;
    private SymbolTable curTable;
    private SymbolTable tempParaTable;
    private String tempFuncToken;
    private ArrayList<String> tempParaToken;

    private SymbolErrorManager() {
        this.rootTable = new SymbolTable(null);
        this.tempParaTable = null;
        this.tempFuncToken = null;
        this.curTable = rootTable;
        this.tempParaToken = null;
    }

    public static SymbolErrorManager getInstance() {
        if (instance == null) {
            instance = new SymbolErrorManager();
        }
        return instance;
    }

    public Symbol registerSymbol(String token, EntityType type, String... s) {
        if (type == EntityType.FUNC) {
            tempFuncToken = token;
            tempParaTable = new SymbolTable(null);
            tempParaToken = new ArrayList<>();
        }
        return curTable.registerSymbol(token, type, s);
    }

    public Symbol registerPara(String token, EntityType type, String... s) {
        assert tempParaTable != null;
        tempParaToken.add(token);
        return tempParaTable.registerSymbol(token, type, s);
    }

    public Symbol findSymbol(String token, boolean recursively) {
        if (tempParaTable == null) {
            return curTable.findSymbol(token, recursively);
        } else {
            return tempParaTable.findSymbol(token, false);
        }
    }

    public SymbolTable proceedScope() {
        curTable = new SymbolTable(curTable);
        curTable.getPrev().addSubs(curTable);
        if (tempParaTable != null) {
            curTable.registerPara(tempParaTable, tempParaToken, tempFuncToken);
        }
        tempFuncToken = null;
        tempParaTable = null;
        tempParaToken = null;
        return curTable;
    }

    public SymbolTable retrieveScope() {
        return curTable = curTable.getPrev();
    }

}
