package IRBuilder;

import IRBuilder.Value.Value;

import java.util.ArrayList;

public class IRModule {
    private ArrayList<Value> values;

    public IRModule() {
        this.values = new ArrayList<>();
    }

    public ArrayList<Value> getValues() {
        return values;
    }

    public void addValue(Value value) {
        values.add(value);
    }
}
