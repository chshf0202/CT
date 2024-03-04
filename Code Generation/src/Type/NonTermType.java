package Type;

public enum NonTermType implements GrammarType {
    COMPUNIT("<CompUnit>"),
    DECL("<Decl>"),
    FUNCDEF("<FuncDef>"),
    MAINFUNCDEF("<MainFuncDef>"),
    CONSTDECL("<ConstDecl>"),
    VARDECL("<VarDecl>"),
    BTYPE("<BType>"),
    CONSTDEF("<ConstDef>"),
    CONSTEXP("<ConstExp>"),
    CONSTINITVAL("<ConstInitVal>"),
    VARDEF("<VarDef>"),
    INITVAL("<InitVal>"),
    EXP("<Exp>"),
    FUNCTYPE("<FuncType>"),
    FUNCFPARAMS("<FuncFParams>"),
    BLOCK("<Block>"),
    FUNCFPARAM("<FuncFParam>"),
    BLOCKITEM("<BlockItem>"),
    STMT("<Stmt>"),
    COND("<Cond>"),
    FORSTMT("<ForStmt>"),
    LVAL("<LVal>"),
    ADDEXP("<AddExp>"),
    LOREXP("<LOrExp>"),
    PRIMARYEXP("<PrimaryExp>"),
    NUMBER("<Number>"),
    UNARYEXP("<UnaryExp>"),
    UNARYOP("<UnaryOp>"),
    FUNCRPARAMS("<FuncRParams>"),
    MULEXP("<MulExp>"),
    RELEXP("<RelExp>"),
    EQEXP("<EqExp>"),
    LANDEXP("<LAndExp>");

    private final String description;

    private NonTermType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
