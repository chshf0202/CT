package Error;

import Error.Symbol.*;
import Lexer.TermNode;
import Parser.ASTNode;
import Type.EntityType;
import Type.GrammarType;
import Type.LexType;
import Type.NonTermType;

import java.util.ArrayList;

public class Handler {
    private static volatile Handler instance;
    private boolean debug;
    private final ArrayList<ErrorText> errorText;
    private ASTNode root;
    private EntityType curObjType;
    private EntityType curFuncType;
    private int curLoopLevel;

    private Handler() {
        this.errorText = new ArrayList<>();
    }

    public static Handler getInstance() {
        if (instance == null) {
            instance = new Handler();
        }
        return instance;
    }

    public void initHandler(boolean debug, ASTNode root) {
        this.debug = debug;
        this.root = root;
        this.curObjType = null;
        this.curFuncType = null;
        this.curLoopLevel = 0;
    }

    public ArrayList<String> createAns() {
        visit(root);
        ArrayList<String> ans = new ArrayList<>();
        for (ErrorText text : errorText) {
            ans.add(text.toString());
        }
        return ans;
    }

    public void visit(ASTNode thisNode) {
        GrammarType thisType = thisNode.getGrammarType();
        if (thisType == NonTermType.BLOCK) {
            SymbolErrorManager.getInstance().proceedScope();
        }
        if (thisType == NonTermType.CONSTDECL) {
            assert thisNode.getChildren().get(1).getGrammarType() == NonTermType.BTYPE;
            curObjType = EntityType.INT;
        } else if (thisType == NonTermType.VARDECL) {
            assert thisNode.getChildren().get(0).getGrammarType() == NonTermType.BTYPE;
            curObjType = EntityType.INT;
        }
        if (thisType == NonTermType.FUNCDEF) {
            assert thisNode.getChildren().get(0).getGrammarType() == NonTermType.FUNCTYPE;
            curFuncType = (thisNode.getChildren().get(0).getChildren().get(0).getGrammarType()
                    == LexType.INTTK) ? EntityType.INT : EntityType.VOID;
        } else if (thisType == NonTermType.MAINFUNCDEF) {
            curFuncType = EntityType.INT;
        }
        if (thisType == NonTermType.STMT
                && thisNode.getChildren().get(0).getGrammarType() == LexType.FORTK) {
            curLoopLevel++;
        }

        if (thisType == LexType.STRCON) {
            check('a', thisNode);
        } else if (thisType == NonTermType.CONSTDEF
                || thisType == NonTermType.VARDEF
                || thisType == NonTermType.FUNCFPARAM) {
            check('b', thisNode);
            check('k', thisNode);
        } else if (thisType == NonTermType.LVAL) {
            check('c', thisNode);
            check('k', thisNode);
        } else if (thisType == NonTermType.UNARYEXP) {
            if (thisNode.getChildren().get(0).getGrammarType() == LexType.IDENFR) {
                check('c', thisNode);
                check('d', thisNode);
                check('e', thisNode);
                check('j', thisNode);
            }
        } else if (thisType == NonTermType.STMT) {
            if (thisNode.getChildren().get(0).getGrammarType() == LexType.RETURNTK) {
                check('f', thisNode);
            }
            if (thisNode.getChildren().get(0).getGrammarType() == NonTermType.LVAL) {
                check('h', thisNode);
            }
            if (thisNode.getChildren().get(0).getGrammarType() != NonTermType.BLOCK
                    && thisNode.getChildren().get(0).getGrammarType() != LexType.IFTK
                    && thisNode.getChildren().get(0).getGrammarType() != LexType.FORTK) {
                check('i', thisNode);
            }
            if (thisNode.getChildren().get(0).getGrammarType() == LexType.IFTK
                    || thisNode.getChildren().get(0).getGrammarType() == LexType.PRINTFTK
                    || (thisNode.getChildren().get(0).getGrammarType() == NonTermType.LVAL
                    && thisNode.getChildren().get(2).getGrammarType() == LexType.GETINTTK)) {
                check('j', thisNode);
            }
            if (thisNode.getChildren().get(0).getGrammarType() == LexType.PRINTFTK) {
                check('l', thisNode);
            }
        } else if (thisType == NonTermType.FUNCDEF) {
            check('b', thisNode);
            check('g', thisNode);
            check('j', thisNode);
        } else if (thisType == NonTermType.MAINFUNCDEF) {
            check('g', thisNode);
            check('j', thisNode);
        } else if (thisType == NonTermType.FORSTMT) {
            check('h', thisNode);
        } else if (thisType == NonTermType.CONSTDECL
                || thisType == NonTermType.VARDECL) {
            check('i', thisNode);
        } else if (thisType == LexType.BREAKTK
                || thisType == LexType.CONTINUETK) {
            check('m', thisNode);
        }

        for (ASTNode child : thisNode.getChildren()) {
            visit(child);
        }

        if (thisType == NonTermType.BLOCK) {
            SymbolErrorManager.getInstance().retrieveScope();
        }
        if (thisType == NonTermType.CONSTDECL
                || thisType == NonTermType.VARDECL) {
            curObjType = null;
        }
        if (thisType == NonTermType.FUNCDEF
                || thisType == NonTermType.MAINFUNCDEF) {
            curFuncType = null;
        }
        if (thisType == NonTermType.STMT
                && thisNode.getChildren().get(0).getGrammarType() == LexType.FORTK) {
            curLoopLevel--;
        }
    }

    private void registerSymbol(ASTNode thisNode) {
        GrammarType thisType = thisNode.getGrammarType();
        String token;
        EntityType type;
        String isConst;
        String[] s;
        if (thisType == NonTermType.CONSTDEF
                || thisType == NonTermType.VARDEF) {
            token = ((TermNode) thisNode.getChildren().get(0)).getToken();
            type = curObjType;
            isConst = (thisType == NonTermType.CONSTDEF) ? "true" : "false";
            if (thisNode.getChildren().size() > 1
                    && thisNode.getChildren().get(1).getGrammarType() == LexType.LBRACK) {
                type = EntityType.ARRAY;
                if (thisNode.getChildren().size() > 4
                        && thisNode.getChildren().get(4).getGrammarType() == LexType.LBRACK) {
                    s = new String[4];
                    s[0] = isConst;
                    s[1] = "2";
                    //s[2] = eval(thisNode.getChildren().get(2));
                    //s[3] = eval(thisNode.getChildren().get(5));
                    SymbolErrorManager.getInstance().registerSymbol(token, type, s);
                } else {
                    s = new String[3];
                    s[0] = isConst;
                    s[1] = "1";
                    //s[2] = eval(thisNode.getChildren().get(2));
                    SymbolErrorManager.getInstance().registerSymbol(token, type, s);
                }
            } else {
                s = new String[1];
                s[0] = isConst;
                SymbolErrorManager.getInstance().registerSymbol(token, type, s);
            }
        } else if (thisType == NonTermType.FUNCDEF) {
            String returnType;
            token = ((TermNode) thisNode.getChildren().get(1)).getToken();
            assert thisNode.getChildren().get(0).getGrammarType() == NonTermType.FUNCTYPE;
            if (thisNode.getChildren().get(0).getChildren().get(0).getGrammarType() == LexType.INTTK) {
                returnType = "int";
            } else {
                returnType = "void";
            }
            int paraNum = thisNode.getChildren().size() - 5;
            s = new String[2];
            s[0] = returnType;
            s[1] = String.valueOf(paraNum);
            SymbolErrorManager.getInstance().registerSymbol(token, EntityType.FUNC, s);
        } else {
            assert thisNode.getChildren().get(0).getGrammarType() == NonTermType.BTYPE;
            type = EntityType.INT;
            token = ((TermNode) thisNode.getChildren().get(1)).getToken();
            if (thisNode.getChildren().size() > 2
                    && thisNode.getChildren().get(2).getGrammarType() == LexType.LBRACK) {
                type = EntityType.ARRAY;
                if (thisNode.getChildren().size() > 4
                        && thisNode.getChildren().get(4).getGrammarType() == LexType.LBRACK) {
                    s = new String[4];
                    s[0] = "false";
                    s[1] = "2";
                    //s[2] = -1;
                    //s[3] = eval(thisNode.getChildren().get(5));
                    SymbolErrorManager.getInstance().registerPara(token, type, s);
                } else {
                    s = new String[3];
                    s[0] = "false";
                    s[1] = "1";
                    //s[2] = -1;
                    SymbolErrorManager.getInstance().registerPara(token, type, s);
                }
            } else {
                s = new String[1];
                s[0] = "false";
                SymbolErrorManager.getInstance().registerPara(token, type, s);
            }
        }
    }

    public void check(char e, ASTNode node) {
        if (debug) {
            switch (e) {
                case 'a': {
                    checkA(node);
                    break;
                }
                case 'b': {
                    checkB(node);
                    break;
                }
                case 'c': {
                    checkC(node);
                    break;
                }
                case 'd': {
                    checkD(node);
                    break;
                }
                case 'e': {
                    checkE(node);
                    break;
                }
                case 'f': {
                    checkF(node);
                    break;
                }
                case 'g': {
                    checkG(node);
                    break;
                }
                case 'h': {
                    checkH(node);
                    break;
                }
                case 'i': {
                    checkI(node);
                    break;
                }
                case 'j': {
                    checkJ(node);
                    break;
                }
                case 'k': {
                    checkK(node);
                    break;
                }
                case 'l': {
                    checkL(node);
                    break;
                }
                case 'm': {
                    checkM(node);
                    break;
                }
                default: {
                }
            }
        }
    }

    private void checkA(ASTNode node) {
        if (((TermNode) node).getToken().equals("\"\"")) {
            return;
        }
        char[] chars = ((TermNode) node).getToken().toCharArray();
        int lineNum = ((TermNode) node).getLineNum();
        for (int i = 1; i < chars.length - 2; i++) {
            char c = chars[i], pre = chars[i + 1];
            if (!(c == 32 || c == 33 || (c >= 40 && c <= 126 && c != 92) || (c == '%' && pre == 'd') || (c == '\\' && pre == 'n'))) {
                errorText.add(new ErrorText(lineNum, 'a'));
                return;
            }
        }
        char c = chars[chars.length - 2];
        if (!(c == 32 || c == 33 || (c >= 40 && c <= 126 && c != 92))) {
            errorText.add(new ErrorText(lineNum, 'a'));
        }
    }

    private void checkB(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if (child.getGrammarType() == LexType.IDENFR) {
                String token = ((TermNode) child).getToken();
                Symbol symbol = SymbolErrorManager.getInstance().findSymbol(token, false);
                if (symbol != null) {
                    errorText.add(new ErrorText(((TermNode) child).getLineNum(), 'b'));
                } else {
                    registerSymbol(node);
                }
            }
        }
    }

    private void checkC(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            if (child.getGrammarType() == LexType.IDENFR) {
                String token = ((TermNode) child).getToken();
                Symbol symbol = SymbolErrorManager.getInstance().findSymbol(token, true);
                if (symbol == null) {
                    errorText.add(new ErrorText(((TermNode) child).getLineNum(), 'c'));
                }
            }
        }
    }

    private void checkD(ASTNode node) {
        int paraNum;
        if (node.getChildren().size() < 3) {
            return;
        }
        if (node.getChildren().get(2).getGrammarType() == LexType.RPARENT) {
            paraNum = 0;
        } else {
            paraNum = (node.getChildren().get(2).getChildren().size() + 1) / 2;
        }
        String token = ((TermNode) node.getChildren().get(0)).getToken();
        Symbol symbol = SymbolErrorManager.getInstance().findSymbol(token, true);
        if (symbol == null) {
            return;
        }
        assert symbol instanceof SymbolFunction;
        if (((SymbolFunction) symbol).getParaNum() != paraNum) {
            errorText.add(new ErrorText(((TermNode) node.getChildren().get(0)).getLineNum(), 'd'));
        }
    }

    private void checkE(ASTNode node) {
        String token = ((TermNode) node.getChildren().get(0)).getToken();
        Symbol symbol = SymbolErrorManager.getInstance().findSymbol(token, true);
        assert symbol instanceof SymbolFunction;
        if (node.getChildren().size() <= 3) {
            return;
        }
        if (node.getChildren().get(2).getGrammarType() != LexType.RPARENT) {
            int i = 0;
            boolean error = false;
            for (SymbolObject para : ((SymbolFunction) symbol).getParas()) {
                if (i >= node.getChildren().get(2).getChildren().size()) {
                    break;
                }
                if (para instanceof SymbolArray) {
                    error = error || comparePara(node.getChildren().get(2).getChildren().get(i),
                            ((SymbolArray) para).getDim());
                } else {
                    error = error || comparePara(node.getChildren().get(2).getChildren().get(i), 0);
                }
                i += 2;
            }
            if (error) {
                errorText.add(new ErrorText(((TermNode) node.getChildren().get(0)).getLineNum(), 'e'));
            }
        }
    }

    private boolean comparePara(ASTNode node, int dim) {
        if (node.getGrammarType() == NonTermType.UNARYEXP) {
            if (node.getChildren().get(0).getGrammarType() == NonTermType.PRIMARYEXP) {
                ASTNode primaryExp = node.getChildren().get(0);
                if (primaryExp.getChildren().get(0).getGrammarType() == LexType.LBRACK) {
                    return comparePara(primaryExp.getChildren().get(1), dim);
                } else if (primaryExp.getChildren().get(0).getGrammarType() == NonTermType.LVAL) {
                    ASTNode LVal = primaryExp.getChildren().get(0);
                    String token = ((TermNode) LVal.getChildren().get(0)).getToken();
                    Symbol symbol = SymbolErrorManager.getInstance().findSymbol(token, true);
                    if (symbol instanceof SymbolArray) {
                        if (LVal.getChildren().size() == 1) {
                            return dim != ((SymbolArray) symbol).getDim();
                        } else if (LVal.getChildren().size() == 4) {
                            return dim != ((SymbolArray) symbol).getDim() - 1;
                        } else {
                            return dim != ((SymbolArray) symbol).getDim() - 2;
                        }
                    } else {
                        return dim != 0;
                    }
                } else {
                    return dim != 0;
                }
            } else if (node.getChildren().get(0).getGrammarType() == LexType.IDENFR) {
                String token = ((TermNode) node.getChildren().get(0)).getToken();
                Symbol symbol = SymbolErrorManager.getInstance().findSymbol(token, true);
                assert symbol instanceof SymbolFunction;
                return symbol.getType() == EntityType.VOID;
            }
        }
        boolean error = false;
        for (ASTNode child : node.getChildren()) {
            error = error || comparePara(child, dim);
        }
        return error;
    }

    private void checkF(ASTNode node) {
        if (curFuncType == EntityType.VOID
                && node.getChildren().get(1).getGrammarType() != LexType.SEMICN) {
            errorText.add(new ErrorText(((TermNode) node.getChildren().get(0)).getLineNum(), 'f'));
        }
    }

    private void checkG(ASTNode node) {
        ASTNode block = node.getChildren().get(node.getChildren().size() - 1);
        assert block.getGrammarType() == NonTermType.BLOCK;
        ASTNode blockItem = block.getChildren().get(block.getChildren().size() - 2);
        ASTNode rBrace = block.getChildren().get(block.getChildren().size() - 1);
        assert rBrace.getGrammarType() == LexType.RBRACE;
        if (curFuncType == EntityType.INT) {
            if (blockItem.getGrammarType() == LexType.LBRACE) {
                errorText.add(new ErrorText(((TermNode) rBrace).getLineNum(), 'g'));
            } else {
                assert blockItem.getGrammarType() == NonTermType.BLOCKITEM;
                ASTNode stmt = blockItem.getChildren().get(0);
                assert stmt.getGrammarType() == NonTermType.STMT;
                ASTNode returnTk = stmt.getChildren().get(0);
                if (returnTk.getGrammarType() != LexType.RETURNTK
                        || stmt.getChildren().get(1).getGrammarType() == LexType.SEMICN) {
                    errorText.add(new ErrorText(((TermNode) rBrace).getLineNum(), 'g'));
                }
            }
        }
    }

    private void checkH(ASTNode node) {
        ASTNode lVal = node.getChildren().get(0);
        assert lVal.getGrammarType() == NonTermType.LVAL;
        ASTNode ident = lVal.getChildren().get(0);
        assert ident.getGrammarType() == LexType.IDENFR;
        Symbol symbol = SymbolErrorManager.getInstance().findSymbol(((TermNode) ident).getToken(), true);
        if (symbol == null) {
            return;
        }
        assert symbol instanceof SymbolObject;
        if (((SymbolObject) symbol).isConst()) {
            errorText.add(new ErrorText(((TermNode) ident).getLineNum(), 'h'));
        }
    }

    private int findNonTermLineNum(ASTNode node) {
        if (node instanceof TermNode) {
            return ((TermNode) node).getLineNum();
        } else {
            return findNonTermLineNum(node.getChildren().get(node.getChildren().size() - 1));
        }
    }

    private void checkI(ASTNode node) {
        if (node.getChildren().get(node.getChildren().size() - 1).getGrammarType()
                != LexType.SEMICN) {
            int lineNum = findNonTermLineNum(node.getChildren().get(node.getChildren().size() - 1));
            errorText.add(new ErrorText(lineNum, 'i'));
        }
    }

    private void checkJ(ASTNode node) {
        if (node.getGrammarType() == NonTermType.UNARYEXP) {
            if (node.getChildren().get(node.getChildren().size() - 1).getGrammarType() != LexType.RPARENT) {
                int lineNum = findNonTermLineNum(node.getChildren().get(node.getChildren().size() - 1));
                errorText.add(new ErrorText(lineNum, 'j'));
            }
        } else if (node.getGrammarType() == NonTermType.FUNCDEF
                || node.getGrammarType() == NonTermType.MAINFUNCDEF) {
            if (node.getChildren().get(node.getChildren().size() - 2).getGrammarType() != LexType.RPARENT) {
                int lineNum = findNonTermLineNum(node.getChildren().get(node.getChildren().size() - 2));
                errorText.add(new ErrorText(lineNum, 'j'));
            }
        } else {
            if (node.getChildren().get(0).getGrammarType() == LexType.IFTK) {
                if (node.getChildren().get(3).getGrammarType() != LexType.RPARENT) {
                    int lineNum = findNonTermLineNum(node.getChildren().get(3));
                    errorText.add(new ErrorText(lineNum, 'j'));
                }
            } else {
                if (node.getChildren().get(node.getChildren().size() - 2).getGrammarType() != LexType.RPARENT) {
                    int lineNum = findNonTermLineNum(node.getChildren().get(node.getChildren().size() - 2));
                    errorText.add(new ErrorText(lineNum, 'j'));
                }
            }
        }
    }

    private void checkK(ASTNode node) {
        boolean error = false;
        int i;
        for (i = 0; i < node.getChildren().size(); i++) {
            ASTNode child = node.getChildren().get(i);
            if (!error && child.getGrammarType() == LexType.LBRACK) {
                error = true;
            } else if (error && child.getGrammarType() == LexType.RBRACK) {
                error = false;
            } else if (error && child.getGrammarType() == LexType.LBRACK
                    || error && child.getGrammarType() == LexType.ASSIGN) {
                break;
            }
        }
        if (error) {
            int lineNum = findNonTermLineNum(node.getChildren().get(i - 1));
            errorText.add(new ErrorText(lineNum, 'k'));
        }
    }

    private void checkL(ASTNode node) {
        if (node.getChildren().size() < 5) {
            return;
        }
        int expNum = (node.getChildren().size() - 5) / 2;
        ASTNode formatString = node.getChildren().get(2);
        assert formatString.getGrammarType() == LexType.STRCON;
        String token = ((TermNode) formatString).getToken();
        int formatNum = 0;
        while (token.contains("%d")) {
            formatNum++;
            token = token.substring(token.indexOf("%d") + 2);
        }
        if (formatNum != expNum) {
            errorText.add(new ErrorText(((TermNode) node.getChildren().get(0)).getLineNum(), 'l'));
        }
    }

    private void checkM(ASTNode node) {
        if (curLoopLevel == 0) {
            errorText.add(new ErrorText(((TermNode) node).getLineNum(), 'm'));
        }
    }
}
