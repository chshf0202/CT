package IRBuilder.Value;

import Type.ValueType;

import java.util.ArrayList;

public class Array extends Object {
    private int dim;
    private int elementNum;
    private ValueType elementType;
    private ArrayList<Value> constValue;

    public Array(String token, String temp, boolean isConst, int dim, ValueType elementType) {
        super(token, ValueType.ARRAY, temp, isConst);
        this.elementType = elementType;
        assert dim == 1 || dim == 2;
        this.dim = dim;
        this.constValue = new ArrayList<>();
    }

    public int getDim() {
        return dim;
    }

    public void setElementNum(int elementNum) {
        this.elementNum = elementNum;
    }

    public int getElementNum() {
        return elementNum;
    }

    public ArrayList<Value> getConstValue() {
        return constValue;
    }

    public void addConstValue(Value value) {
        this.constValue.add(value);
    }

    public String getArrayStruct() {
        if (dim == 1) {
            return "[" + elementNum + " x " + elementType.getDescription() + "]";
        } else {
            assert constValue.get(0).getType() == ValueType.ARRAY;
            return "[" + elementNum + " x " + ((Array) constValue.get(0)).getArrayStruct() + "]";
        }
    }
}
