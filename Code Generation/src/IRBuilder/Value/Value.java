package IRBuilder.Value;

import IRBuilder.Use;
import Type.ValueType;

import java.util.ArrayList;

public class Value {
    private String token;
    private ValueType type;
    private String temp;
    private ArrayList<Use> uses;

    public Value(String token, ValueType type, String temp) {
        this.token = token;
        this.type = type;
        this.temp = temp;
        this.uses = new ArrayList<>();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getToken() {
        return token;
    }

    public String getTemp() {
        return temp;
    }

    public ValueType getType() {
        return type;
    }

    public void addUse(Use use) {
        this.uses.add(use);
    }

    public boolean isImm() {
        if ((this instanceof GlobalDecl || this instanceof Array) && this.type != ValueType.FUNCTION) {
            return ((Object) this).isConst();
        } else if (!(this instanceof GlobalDecl || this instanceof Array) && this.temp.toCharArray()[0] != '%'
                && this.temp.toCharArray()[0] != 'b') {
            return true;
        } else {
            return false;
        }
    }

    public int getImm() {
        if (this instanceof GlobalDecl && this.type != ValueType.FUNCTION) {
            if (((Object) this).isConst()) {
                return Integer.parseInt(((GlobalDecl) this).getInitVal());
            }
        } else if (!(this instanceof GlobalDecl || this instanceof Array)
                && this.temp.toCharArray()[0] != '%' && this.temp.toCharArray()[0] != 'b') {
            return Integer.parseInt(temp);
        }
        return Integer.MAX_VALUE;
    }

    public int getImm(int dim1, int dim2) {
        assert this instanceof Array;
        Value imm;
        if (dim2 >= 0) {
            imm = ((Array) ((Array) this).getConstValue().get(dim1)).getConstValue().get(dim2);
        } else {
            imm = ((Array) this).getConstValue().get(dim1);
        }
        if (imm instanceof GlobalDecl) {
            return Integer.parseInt((((GlobalDecl) imm).getInitVal()));
        } else {
            return Integer.parseInt((imm.getTemp()));
        }
    }
}
