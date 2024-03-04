package Error.Symbol;

import Type.EntityType;

import java.util.ArrayList;

public class SymbolFunction extends Symbol {
    private final int paraNum;
    private final ArrayList<SymbolObject> paras;

    public SymbolFunction(int tableId, String token, EntityType type, int paraNum) {
        super(tableId, token, type);
        this.paraNum = paraNum;
        this.paras = new ArrayList<>();
    }

    public void addPara(SymbolObject para) {
        paras.add(para);
    }

    public int getParaNum() {
        return paras.size();
    }

    public ArrayList<SymbolObject> getParas() {
        return paras;
    }
}
