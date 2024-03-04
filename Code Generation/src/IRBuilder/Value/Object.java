package IRBuilder.Value;

import Type.ValueType;

public class Object extends Value {
    private boolean isConst;

    public Object(String token, ValueType type, String temp, boolean isConst) {
        super(token, type, temp);
        this.isConst = isConst;
    }

    public boolean isConst() {
        return isConst;
    }

    public void setIsConst(boolean isConst) {
        if (this instanceof Array) {
            for (Value value : ((Array) this).getConstValue()) {
                ((Object) value).setIsConst(isConst);
            }
        }
        this.isConst = isConst;
    }
}
