package IRBuilder.Value;

import Type.ValueType;

public class GlobalDecl extends Object {
    private String initVal;

    public GlobalDecl(String token, ValueType type, String temp, boolean isConst, String initVal) {
        super(token, type, temp, isConst);
        this.initVal = initVal;
    }

    public String getInitVal() {
        return initVal;
    }

    public String getIR() {
        String constIR = this.isConst() ? "constant" : "global";
        String typeIR = this.getType() == ValueType.INT ? "i32" : "";
        return this.getTemp() + " = dso_local " + constIR + " " + typeIR + " " + initVal;
    }
}
