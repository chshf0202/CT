package IRBuilder.Value;

import Type.ValueType;

public class Pointer extends Object {
    private Value reference;
    private String ptrStruct;

    public Pointer(String token, String temp, boolean isConst, Value reference) {
        super(token, ValueType.PTR, temp, isConst);
        this.reference = reference;
        if (this.reference.getType() == ValueType.INT) {
            this.ptrStruct = "i32*";
        } else if (this.reference.getType() == ValueType.ARRAY) {
            this.ptrStruct = ((Array) reference).getArrayStruct() + "*";
        } else {
            assert this.reference.getType() == ValueType.PTR;
            this.ptrStruct = ((Pointer) this.reference).getPtrStruct();
        }
    }

    public Value getReference() {
        return reference;
    }

    public String getReferenceStruct() {
        if (this.reference.getType() == ValueType.INT) {
            return ValueType.INT.getDescription();
        } else if (this.reference.getType() == ValueType.ARRAY) {
            return ((Array) reference).getArrayStruct();
        } else {
            assert this.reference.getType() == ValueType.PTR;
            return ((Pointer) reference).ptrStruct;
        }
    }

    public String getPtrStruct() {
        return ptrStruct;
    }
}
