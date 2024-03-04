package IRBuilder;

import IRBuilder.Value.Value;

public class Use {
    private Value value;
    private Value user;

    public Use(Value value, Value user) {
        this.value = value;
        this.user = user;
    }
}
