package Type;

public enum LexType implements GrammarType {
    IDENFR(null),
    INTCON(null),
    STRCON(null),
    NOT("!"),
    MULT("*"),
    ASSIGN("="),
    AND("&&"),
    DIV("/"),
    SEMICN(";"),
    OR("||"),
    MOD("%"),
    COMMA(","),
    MAINTK("main"),
    FORTK("for"),
    LSS("<"),
    LPARENT("("),
    CONSTTK("const"),
    GETINTTK("getint"),
    LEQ("<="),
    RPARENT(")"),
    INTTK("int"),
    PRINTFTK("printf"),
    GRE(">"),
    LBRACK("["),
    BREAKTK("break"),
    RETURNTK("return"),
    GEQ(">="),
    RBRACK("]"),
    CONTINUETK("continue"),
    PLUS("+"),
    EQL("=="),
    LBRACE("{"),
    IFTK("if"),
    MINU("-"),
    NEQ("!="),
    RBRACE("}"),
    ELSETK("else"),
    VOIDTK("void");

    private final String description;

    private LexType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
