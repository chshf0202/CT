package IRBuilder.Value;

import Type.ValueType;

public class GlobalArrayDecl extends Array {
    String IR;

    public GlobalArrayDecl(String token, String temp, boolean isConst, int dim, ValueType elementType) {
        super(token, temp, isConst, dim, elementType);
        this.IR = null;
    }

    public String initialize() {
        String constIR = this.isConst() ? "constant" : "global";
        this.IR = this.getTemp() + " = dso_local " + constIR + " ";
        if (this.isAllZero()) {
            this.IR += getArrayStruct() + " zeroinitializer";
            return getArrayStruct() + " zeroinitializer";
        }
        StringBuilder initializer = new StringBuilder(getArrayStruct() + " " + "[");
        if (this.getDim() == 1) {
            for (Value element : this.getConstValue()) {
                assert element instanceof GlobalDecl;
                initializer.append(element.getType().getDescription()).append(" ")
                        .append(((GlobalDecl) element).getInitVal()).append(", ");
            }
            initializer = new StringBuilder(initializer.substring(0, initializer.length() - 2));
        } else {
            for (Value element : this.getConstValue()) {
                initializer.append(((GlobalArrayDecl) element).initialize()).append(", ");
            }
            initializer = new StringBuilder(initializer.substring(0, initializer.length() - 2));
        }
        initializer.append("]");
        this.IR += initializer;
        return initializer.toString();
    }

    public boolean isAllZero() {
        if (this.getDim() == 1) {
            for (Value element : this.getConstValue()) {
                assert element instanceof GlobalDecl;
                if (Integer.parseInt(((GlobalDecl) element).getInitVal()) != 0) {
                    return false;
                }
            }
        } else {
            for (Value element : this.getConstValue()) {
                assert element instanceof GlobalArrayDecl;
                if (!((GlobalArrayDecl) element).isAllZero()) {
                    return false;
                }
            }
        }
        return true;
    }

    public String getIR() {
        return IR;
    }
}
