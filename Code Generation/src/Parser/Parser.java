package Parser;

import Lexer.TermNode;
import Type.GrammarType;
import Type.LexType;
import Type.NonTermType;

import java.util.ArrayList;

public class Parser {
    private static volatile Parser instance;
    private ArrayList<ASTNode> lexicon;
    private int curPos;

    private Parser() {
        curPos = 0;
    }

    public static Parser getInstance() {
        if (instance == null) {
            instance = new Parser();
        }
        return instance;
    }

    public ASTNode createAST(ArrayList<ASTNode> lexicon) {
        this.lexicon = lexicon;
        return parseCompUnit();
    }

    public void createAns(ArrayList<String> ans, ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            createAns(ans, child);
        }
        if (node instanceof TermNode) {
            ans.add(((LexType) node.getGrammarType()).name() + " " + ((TermNode) node).getToken());
        } else {
            if (node.getGrammarType() != NonTermType.BLOCKITEM
                    && node.getGrammarType() != NonTermType.DECL
                    && node.getGrammarType() != NonTermType.BTYPE) {
                ans.add(((NonTermType) node.getGrammarType()).getDescription());
            }
        }
    }

    //remain for symbol table
    public boolean isIdent(ASTNode n) {
        return n.getGrammarType() == LexType.IDENFR;
    }

    public ASTNode parseCompUnit() {
        ASTNode thisNode = new ASTNode(null, NonTermType.COMPUNIT);
        while (curPos < lexicon.size() &&
                (lexicon.get(curPos).getGrammarType() == LexType.INTTK ||
                lexicon.get(curPos).getGrammarType() == LexType.VOIDTK ||
                lexicon.get(curPos).getGrammarType() == LexType.CONSTTK)) {
            if (lexicon.get(curPos).getGrammarType() == LexType.CONSTTK) {
                ASTNode newNode = parseDecl();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            } else if (lexicon.get(curPos).getGrammarType() == LexType.VOIDTK) {
                ASTNode newNode = parseFuncDef();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            } else {
                ASTNode preRead = lexicon.get(curPos + 1);
                if (preRead.getGrammarType() == LexType.MAINTK) {
                    ASTNode newNode = parseMainFuncDef();
                    newNode.setFather(thisNode);
                    thisNode.addChild(newNode);
                } else if (isIdent(preRead)) {
                    ASTNode prePreRead = lexicon.get(curPos + 2);
                    //there could be a problem
                    if (prePreRead.getGrammarType() == LexType.LPARENT) {
                        ASTNode newNode = parseFuncDef();
                        newNode.setFather(thisNode);
                        thisNode.addChild(newNode);
                    } else {
                        ASTNode newNode = parseDecl();
                        newNode.setFather(thisNode);
                        thisNode.addChild(newNode);
                    }
                } else {
                    //error
                }
            }
        }
        return thisNode;
    }

    public ASTNode parseDecl() {
        ASTNode thisNode = new ASTNode(null, NonTermType.DECL);
        if (lexicon.get(curPos).getGrammarType() == LexType.CONSTTK) {
            ASTNode newNode = parseConstDecl();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else if (lexicon.get(curPos).getGrammarType() == LexType.INTTK) {
            ASTNode newNode = parseVarDecl();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseConstDecl() {
        ASTNode thisNode = new ASTNode(null, NonTermType.CONSTDECL);
        if (lexicon.get(curPos).getGrammarType() == LexType.CONSTTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        ASTNode newNode = parseBType();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        newNode = parseConstDef();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        while (lexicon.get(curPos).getGrammarType() == LexType.COMMA) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseConstDef();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseBType() {
        ASTNode thisNode = new ASTNode(null, NonTermType.BTYPE);
        if (lexicon.get(curPos).getGrammarType() == LexType.INTTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseConstDef() {
        ASTNode thisNode = new ASTNode(null, NonTermType.CONSTDEF);
        if (isIdent(lexicon.get(curPos))) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        while (lexicon.get(curPos).getGrammarType() == LexType.LBRACK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            ASTNode newNode = parseConstExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            if (lexicon.get(curPos).getGrammarType() == LexType.RBRACK) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.ASSIGN) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        ASTNode newNode = parseConstInitVal();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        return thisNode;
    }

    public ASTNode parseConstInitVal() {
        ASTNode thisNode = new ASTNode(null, NonTermType.CONSTINITVAL);
        if (lexicon.get(curPos).getGrammarType() == LexType.LBRACE) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT ||
                    lexicon.get(curPos).getGrammarType() == LexType.INTCON ||
                    isIdent(lexicon.get(curPos)) ||
                    lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                    lexicon.get(curPos).getGrammarType() == LexType.MINU ||
                    lexicon.get(curPos).getGrammarType() == LexType.NOT ||
                    lexicon.get(curPos).getGrammarType() == LexType.LBRACE) {
                ASTNode newNode = parseConstInitVal();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
                while (lexicon.get(curPos).getGrammarType() == LexType.COMMA) {
                    lexicon.get(curPos).setFather(thisNode);
                    thisNode.addChild(lexicon.get(curPos));
                    curPos++;
                    newNode = parseConstInitVal();
                    newNode.setFather(thisNode);
                    thisNode.addChild(newNode);
                }
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.RBRACE) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT ||
                lexicon.get(curPos).getGrammarType() == LexType.INTCON ||
                isIdent(lexicon.get(curPos)) ||
                lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                lexicon.get(curPos).getGrammarType() == LexType.MINU ||
                lexicon.get(curPos).getGrammarType() == LexType.NOT) {
            ASTNode newNode = parseConstExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseVarDecl() {
        ASTNode thisNode = new ASTNode(null, NonTermType.VARDECL);
        ASTNode newNode = parseBType();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        newNode = parseVarDef();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        while (lexicon.get(curPos).getGrammarType() == LexType.COMMA) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseVarDef();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        }
        ASTNode now = lexicon.get(curPos);
        if (now.getGrammarType() == LexType.SEMICN) {
            now.setFather(thisNode);
            thisNode.addChild(now);
            curPos++;
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseVarDef() {
        ASTNode thisNode = new ASTNode(null, NonTermType.VARDEF);
        if (isIdent(lexicon.get(curPos))) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        while (lexicon.get(curPos).getGrammarType() == LexType.LBRACK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            ASTNode newNode = parseConstExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            if (lexicon.get(curPos).getGrammarType() == LexType.RBRACK) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.ASSIGN) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            ASTNode newNode = parseInitVal();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        }
        return thisNode;
    }

    public ASTNode parseInitVal() {
        ASTNode thisNode = new ASTNode(null, NonTermType.INITVAL);
        if (lexicon.get(curPos).getGrammarType() == LexType.LBRACE) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT ||
                    lexicon.get(curPos).getGrammarType() == LexType.INTCON ||
                    isIdent(lexicon.get(curPos)) ||
                    lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                    lexicon.get(curPos).getGrammarType() == LexType.MINU ||
                    lexicon.get(curPos).getGrammarType() == LexType.NOT ||
                    lexicon.get(curPos).getGrammarType() == LexType.LBRACE) {
                ASTNode newNode = parseInitVal();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
                while (lexicon.get(curPos).getGrammarType() == LexType.COMMA) {
                    lexicon.get(curPos).setFather(thisNode);
                    thisNode.addChild(lexicon.get(curPos));
                    curPos++;
                    newNode = parseInitVal();
                    newNode.setFather(thisNode);
                    thisNode.addChild(newNode);
                }
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.RBRACE) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT ||
                lexicon.get(curPos).getGrammarType() == LexType.INTCON ||
                isIdent(lexicon.get(curPos)) ||
                lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                lexicon.get(curPos).getGrammarType() == LexType.MINU ||
                lexicon.get(curPos).getGrammarType() == LexType.NOT) {
            ASTNode newNode = parseExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseFuncDef() {
        ASTNode thisNode = new ASTNode(null, NonTermType.FUNCDEF);
        ASTNode newNode = parseFuncType();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        if (isIdent(lexicon.get(curPos))) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.INTTK) {
            newNode = parseFuncFParams();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        newNode = parseBlock();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        return thisNode;
    }

    public ASTNode parseMainFuncDef() {
        ASTNode thisNode = new ASTNode(null, NonTermType.MAINFUNCDEF);
        if (lexicon.get(curPos).getGrammarType() == LexType.INTTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.MAINTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        ASTNode newNode = parseBlock();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        return thisNode;
    }

    public ASTNode parseFuncType() {
        ASTNode thisNode = new ASTNode(null, NonTermType.FUNCTYPE);
        if (lexicon.get(curPos).getGrammarType() == LexType.VOIDTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else if (lexicon.get(curPos).getGrammarType() == LexType.INTTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseFuncFParams() {
        ASTNode thisNode = new ASTNode(null, NonTermType.FUNCFPARAMS);
        ASTNode newNode = parseFuncFParam();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        while (lexicon.get(curPos).getGrammarType() == LexType.COMMA) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseFuncFParam();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        }
        return thisNode;
    }

    public ASTNode parseFuncFParam() {
        ASTNode thisNode = new ASTNode(null, NonTermType.FUNCFPARAM);
        ASTNode newNode = parseBType();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        if (isIdent(lexicon.get(curPos))) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.LBRACK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (lexicon.get(curPos).getGrammarType() == LexType.RBRACK) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            while (lexicon.get(curPos).getGrammarType() == LexType.LBRACK) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
                newNode = parseConstExp();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
                if (lexicon.get(curPos).getGrammarType() == LexType.RBRACK) {
                    lexicon.get(curPos).setFather(thisNode);
                    thisNode.addChild(lexicon.get(curPos));
                    curPos++;
                } else {
                    //error
                }
            }
        }
        return thisNode;
    }

    private boolean isInBlockItemFirst(ASTNode n) {
        return isDeclFirst(n) || isStmtFirst(n);
    }

    public ASTNode parseBlock() {
        ASTNode thisNode = new ASTNode(null, NonTermType.BLOCK);
        if (lexicon.get(curPos).getGrammarType() == LexType.LBRACE) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        while (isInBlockItemFirst(lexicon.get(curPos))) {
            ASTNode newNode = parseBlockItem();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        }
        if (lexicon.get(curPos).getGrammarType() == LexType.RBRACE) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        return thisNode;
    }

    private boolean isDeclFirst(ASTNode n) {
        GrammarType ng = n.getGrammarType();
        if (ng == LexType.CONSTTK) {
            return true;
        } else if (ng == LexType.INTTK) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isStmtFirst(ASTNode n) {
        GrammarType ng = n.getGrammarType();
        if (isIdent(n)) {
            return true;
        } else if (ng == LexType.SEMICN) {
            return true;
        } else if (ng == LexType.INTCON) {
            return true;
        } else if (ng == LexType.LPARENT) {
            return true;
        } else if (ng == LexType.PLUS) {
            return true;
        } else if (ng == LexType.MINU) {
            return true;
        } else if (ng == LexType.NOT) {
            return true;
        } else if (ng == LexType.LBRACE) {
            return true;
        } else if (ng == LexType.IFTK) {
            return true;
        } else if (ng == LexType.FORTK) {
            return true;
        } else if (ng == LexType.BREAKTK) {
            return true;
        } else if (ng == LexType.CONTINUETK) {
            return true;
        } else if (ng == LexType.RETURNTK) {
            return true;
        } else if (ng == LexType.PRINTFTK) {
            return true;
        } else {
            return false;
        }
    }

    public ASTNode parseBlockItem() {
        ASTNode thisNode = new ASTNode(null, NonTermType.BLOCKITEM);
        if (isDeclFirst(lexicon.get(curPos))) {
            ASTNode newNode = parseDecl();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else if (isStmtFirst(lexicon.get(curPos))) {
            ASTNode newNode = parseStmt();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseStmt() {
        ASTNode thisNode = new ASTNode(null, NonTermType.STMT);
        GrammarType ng = lexicon.get(curPos).getGrammarType();
        if (isIdent(lexicon.get(curPos))) {
            //there may be problems when missing ';' fault is occurred
            int i = 1;
            ASTNode preRead = lexicon.get(curPos + i);
            while (preRead.getGrammarType() != LexType.ASSIGN &&
                    preRead.getGrammarType() != LexType.SEMICN) {
                i++;
                preRead = lexicon.get(curPos + i);
            }
            if (preRead.getGrammarType() == LexType.ASSIGN) {
                ASTNode newNode = parseLVal();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
                if (lexicon.get(curPos).getGrammarType() == LexType.ASSIGN) {
                    lexicon.get(curPos).setFather(thisNode);
                    thisNode.addChild(lexicon.get(curPos));
                    curPos++;
                } else {
                    //error
                }
                if (lexicon.get(curPos).getGrammarType() == LexType.GETINTTK) {
                    lexicon.get(curPos).setFather(thisNode);
                    thisNode.addChild(lexicon.get(curPos));
                    curPos++;
                    if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT) {
                        lexicon.get(curPos).setFather(thisNode);
                        thisNode.addChild(lexicon.get(curPos));
                        curPos++;
                    } else {
                        //error
                    }
                    if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
                        lexicon.get(curPos).setFather(thisNode);
                        thisNode.addChild(lexicon.get(curPos));
                        curPos++;
                    } else {
                        //error
                    }
                } else if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT ||
                        lexicon.get(curPos).getGrammarType() == LexType.INTCON ||
                        isIdent(lexicon.get(curPos)) ||
                        lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                        lexicon.get(curPos).getGrammarType() == LexType.MINU ||
                        lexicon.get(curPos).getGrammarType() == LexType.NOT) {
                    newNode = parseExp();
                    newNode.setFather(thisNode);
                    thisNode.addChild(newNode);
                } else {
                    //error
                }
            } else {
                ASTNode newNode = parseExp();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else if (ng == LexType.SEMICN) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else if (ng == LexType.INTCON || ng == LexType.LPARENT ||
                ng == LexType.PLUS || ng == LexType.MINU || ng == LexType.NOT) {
            ASTNode newNode = parseExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else if (ng == LexType.LBRACE) {
            ASTNode newNode = parseBlock();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else if (ng == LexType.IFTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            ASTNode newNode = parseCond();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            newNode = parseStmt();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            if (lexicon.get(curPos).getGrammarType() == LexType.ELSETK) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
                newNode = parseStmt();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
        } else if (ng == LexType.FORTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            if (isIdent(lexicon.get(curPos))) {
                ASTNode newNode = parseForStmt();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            if (isIdent(lexicon.get(curPos))
                    || lexicon.get(curPos).getGrammarType() == LexType.LPARENT
                    || lexicon.get(curPos).getGrammarType() == LexType.INTCON
                    || lexicon.get(curPos).getGrammarType() == LexType.PLUS
                    || lexicon.get(curPos).getGrammarType() == LexType.MINU
                    || lexicon.get(curPos).getGrammarType() == LexType.NOT) {
                ASTNode newNode = parseCond();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            if (isIdent(lexicon.get(curPos))) {
                ASTNode newNode = parseForStmt();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            ASTNode newNode = parseStmt();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else if (ng == LexType.BREAKTK || ng == LexType.CONTINUETK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else if (ng == LexType.RETURNTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (isIdent(lexicon.get(curPos))
                    || lexicon.get(curPos).getGrammarType() == LexType.LPARENT
                    || lexicon.get(curPos).getGrammarType() == LexType.INTCON
                    || lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                    lexicon.get(curPos).getGrammarType() == LexType.MINU ||
                    lexicon.get(curPos).getGrammarType() == LexType.NOT) {
                ASTNode newNode = parseExp();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else if (ng == LexType.PRINTFTK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.STRCON) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            while (lexicon.get(curPos).getGrammarType() == LexType.COMMA) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
                ASTNode newNode = parseExp();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
            if (lexicon.get(curPos).getGrammarType() == LexType.SEMICN) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseForStmt() {
        ASTNode thisNode = new ASTNode(null, NonTermType.FORSTMT);
        ASTNode newNode = parseLVal();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        if (lexicon.get(curPos).getGrammarType() == LexType.ASSIGN) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        newNode = parseExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        return thisNode;
    }

    public ASTNode parseExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.EXP);
        ASTNode newNode = parseAddExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        return thisNode;
    }

    public ASTNode parseCond() {
        ASTNode thisNode = new ASTNode(null, NonTermType.COND);
        ASTNode newNode = parseLOrExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        return thisNode;
    }

    public ASTNode parseLVal() {
        ASTNode thisNode = new ASTNode(null, NonTermType.LVAL);
        if (isIdent(lexicon.get(curPos))) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        }
        while (lexicon.get(curPos).getGrammarType() == LexType.LBRACK) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            ASTNode newNode = parseExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            if (lexicon.get(curPos).getGrammarType() == LexType.RBRACK) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        }
        return thisNode;
    }

    public ASTNode parsePrimaryExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.PRIMARYEXP);
        if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            ASTNode newNode = parseExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
            } else {
                //error
            }
        } else if (isIdent(lexicon.get(curPos))) {
            ASTNode newNode = parseLVal();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else if (lexicon.get(curPos).getGrammarType() == LexType.INTCON) {
            ASTNode newNode = parseNumber();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseNumber() {
        ASTNode thisNode = new ASTNode(null, NonTermType.NUMBER);
        if (lexicon.get(curPos).getGrammarType() == LexType.INTCON) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseUnaryExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.UNARYEXP);
        if (lexicon.get(curPos).getGrammarType() == LexType.PLUS
                || lexicon.get(curPos).getGrammarType() == LexType.MINU
                || lexicon.get(curPos).getGrammarType() == LexType.NOT) {
            ASTNode newNode = parseUnaryOp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            newNode = parseUnaryExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else if (isIdent(lexicon.get(curPos))) {
            //there can be a problem
            ASTNode preRead = lexicon.get(curPos + 1);
            if (preRead.getGrammarType() == LexType.LPARENT) {
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
                lexicon.get(curPos).setFather(thisNode);
                thisNode.addChild(lexicon.get(curPos));
                curPos++;
                if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT ||
                        lexicon.get(curPos).getGrammarType() == LexType.INTCON ||
                        isIdent(lexicon.get(curPos)) ||
                        lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                        lexicon.get(curPos).getGrammarType() == LexType.MINU ||
                        lexicon.get(curPos).getGrammarType() == LexType.NOT) {
                    ASTNode newNode = parseFuncRParams();
                    newNode.setFather(thisNode);
                    thisNode.addChild(newNode);
                }
                if (lexicon.get(curPos).getGrammarType() == LexType.RPARENT) {
                    lexicon.get(curPos).setFather(thisNode);
                    thisNode.addChild(lexicon.get(curPos));
                    curPos++;
                } else {
                    //error
                }
            } else {
                ASTNode newNode = parsePrimaryExp();
                newNode.setFather(thisNode);
                thisNode.addChild(newNode);
            }
        } else if (lexicon.get(curPos).getGrammarType() == LexType.LPARENT
                || lexicon.get(curPos).getGrammarType() == LexType.INTCON) {
            ASTNode newNode = parsePrimaryExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseUnaryOp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.UNARYOP);
        if (lexicon.get(curPos).getGrammarType() == LexType.PLUS
                || lexicon.get(curPos).getGrammarType() == LexType.MINU
                || lexicon.get(curPos).getGrammarType() == LexType.NOT) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
        } else {
            //error
        }
        return thisNode;
    }

    public ASTNode parseFuncRParams() {
        ASTNode thisNode = new ASTNode(null, NonTermType.FUNCRPARAMS);
        ASTNode newNode = parseExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        while (lexicon.get(curPos).getGrammarType() == LexType.COMMA) {
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
        }
        return thisNode;
    }

    public ASTNode parseMulExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.MULEXP);
        ASTNode newNode = parseUnaryExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        ASTNode oldNode = thisNode;
        while (lexicon.get(curPos).getGrammarType() == LexType.MULT ||
                lexicon.get(curPos).getGrammarType() == LexType.DIV ||
                lexicon.get(curPos).getGrammarType() == LexType.MOD) {
            thisNode = new ASTNode(null, NonTermType.MULEXP);
            oldNode.setFather(thisNode);
            thisNode.addChild(oldNode);
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseUnaryExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            oldNode = thisNode;
        }
        return thisNode;
    }

    public ASTNode parseAddExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.ADDEXP);
        ASTNode newNode = parseMulExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        ASTNode oldNode = thisNode;
        while (lexicon.get(curPos).getGrammarType() == LexType.PLUS ||
                lexicon.get(curPos).getGrammarType() == LexType.MINU) {
            thisNode = new ASTNode(null, NonTermType.ADDEXP);
            oldNode.setFather(thisNode);
            thisNode.addChild(oldNode);
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseMulExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            oldNode = thisNode;
        }
        return thisNode;
    }

    public ASTNode parseRelExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.RELEXP);
        ASTNode newNode = parseAddExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        ASTNode oldNode = thisNode;
        while (lexicon.get(curPos).getGrammarType() == LexType.GRE ||
                lexicon.get(curPos).getGrammarType() == LexType.LSS ||
                lexicon.get(curPos).getGrammarType() == LexType.GEQ ||
                lexicon.get(curPos).getGrammarType() == LexType.LEQ) {
            thisNode = new ASTNode(null, NonTermType.RELEXP);
            oldNode.setFather(thisNode);
            thisNode.addChild(oldNode);
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseAddExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            oldNode = thisNode;
        }
        return thisNode;
    }

    public ASTNode parseEqExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.EQEXP);
        ASTNode newNode = parseRelExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        ASTNode oldNode = thisNode;
        while (lexicon.get(curPos).getGrammarType() == LexType.EQL ||
                lexicon.get(curPos).getGrammarType() == LexType.NEQ) {
            thisNode = new ASTNode(null, NonTermType.EQEXP);
            oldNode.setFather(thisNode);
            thisNode.addChild(oldNode);
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseRelExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            oldNode = thisNode;
        }
        return thisNode;
    }

    public ASTNode parseLAndExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.LANDEXP);
        ASTNode newNode = parseEqExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        ASTNode oldNode = thisNode;
        while (lexicon.get(curPos).getGrammarType() == LexType.AND) {
            thisNode = new ASTNode(null, NonTermType.LANDEXP);
            oldNode.setFather(thisNode);
            thisNode.addChild(oldNode);
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseEqExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            oldNode = thisNode;
        }
        return thisNode;
    }

    public ASTNode parseLOrExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.LOREXP);
        ASTNode newNode = parseLAndExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        ASTNode oldNode = thisNode;
        while (lexicon.get(curPos).getGrammarType() == LexType.OR) {
            thisNode = new ASTNode(null, NonTermType.LOREXP);
            oldNode.setFather(thisNode);
            thisNode.addChild(oldNode);
            lexicon.get(curPos).setFather(thisNode);
            thisNode.addChild(lexicon.get(curPos));
            curPos++;
            newNode = parseLAndExp();
            newNode.setFather(thisNode);
            thisNode.addChild(newNode);
            oldNode = thisNode;
        }
        return thisNode;
    }

    public ASTNode parseConstExp() {
        ASTNode thisNode = new ASTNode(null, NonTermType.CONSTEXP);
        ASTNode newNode = parseAddExp();
        newNode.setFather(thisNode);
        thisNode.addChild(newNode);
        return thisNode;
    }

}