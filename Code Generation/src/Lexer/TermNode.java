package Lexer;

import Parser.ASTNode;
import Type.LexType;

public class TermNode extends ASTNode {
    private String token;
    private int lineNum;
    private int number;

    public TermNode(ASTNode father, LexType type, String token, int lineNum, int number) {
        super(father, type);
        this.token = token;
        this.lineNum = lineNum;
        this.number = number;
    }

    public String getToken() {
        return token;
    }

    public int getLineNum() {
        return lineNum;
    }

    public int getNumber() {
        return number;
    }
}
