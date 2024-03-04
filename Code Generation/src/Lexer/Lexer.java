package Lexer;

import Parser.ASTNode;
import Type.LexType;
import Error.Handler;

import java.util.ArrayList;
import java.util.HashMap;

public class Lexer {
    private static volatile Lexer instance;
    private String source;
    private int curPos;
    private String token;
    private LexType lexType;
    private final HashMap<String, LexType> reservedWords;
    private int lineNum;
    private int number;

    private Lexer() {
        this.source = null;
        curPos = 0;
        token = "";
        lexType = null;
        reservedWords = new HashMap<>();
        lineNum = 1;
        number = 0;
        for (LexType lexType : LexType.values()) {
            if (lexType.getDescription() != null) {
                reservedWords.put(lexType.getDescription(), lexType);
            }
        }
    }

    public static Lexer getInstance() {
        if (instance == null) {
            instance = new Lexer();
        }
        return instance;
    }

    public ArrayList<ASTNode> createLexicon(String source) {
        this.source = source;
        ArrayList<ASTNode> lexicon = new ArrayList<>();
        while (next() == 0) {
            lexicon.add(new TermNode(null, lexType, token, lineNum, number));
        }
        return lexicon;
    }

    public String getToken() {
        return token;
    }

    public LexType getLexType() {
        return lexType;
    }

    public int getLineNum() {
        return lineNum;
    }

    public int getNumber() {
        return number;
    }

    public int next() {
        token = "";
        lexType = null;
        int len = source.length();
        if (curPos == len) {
            return 1;
        }
        char c = source.charAt(curPos);
        curPos++;
        if (" \n\t\r".indexOf(c) != -1) {
            if (c == '\n') {
                lineNum++;
            }
            return next();
        }
        if (Character.isAlphabetic(c) || c == '_') {
            token += c;
            while (curPos < len &&
                    (Character.isAlphabetic(source.charAt(curPos))
                            || source.charAt(curPos) == '_'
                            || Character.isDigit(source.charAt(curPos)))) {
                c = source.charAt(curPos++);
                token += c;
            }
            lexType = reservedWords.getOrDefault(token, LexType.IDENFR);
            return 0;
        } else if (Character.isDigit(c)) {
            token += c;
            while (curPos < len && Character.isDigit(source.charAt(curPos))) {
                c = source.charAt(curPos++);
                token += c;
            }
            lexType = LexType.INTCON;
            number = Integer.parseInt(token);
            return 0;
        } else if (c == '/') {
            token += c;
            if (curPos < len && source.charAt(curPos) == '/') {
                curPos++;
                while (curPos < len && source.charAt(curPos) != '\n') {
                    curPos++;
                }
                if (curPos < len) {
                    curPos++;
                    lineNum++;
                }
                return next();
            } else if (curPos < len && source.charAt(curPos) == '*') {
                curPos++;
                while (curPos < len) {
                    while (curPos < len && source.charAt(curPos) != '*') {
                        c = source.charAt(curPos++);
                        if (c == '\n') {
                            lineNum++;
                        }
                    }
                    while (curPos < len && source.charAt(curPos) == '*') {
                        curPos++;
                    }
                    if (curPos < len && source.charAt(curPos) == '/') {
                        curPos++;
                        return next();
                    }
                }
            } else {
                lexType = LexType.DIV;
                return 0;
            }
        } else if (c == '+') {
            token += c;
            lexType = LexType.PLUS;
            return 0;
        } else if (c == '-') {
            token += c;
            lexType = LexType.MINU;
            return 0;
        } else if (c == '*') {
            token += c;
            lexType = LexType.MULT;
            return 0;
        } else if (c == '%') {
            token += c;
            lexType = LexType.MOD;
            return 0;
        } else if (c == ';') {
            token += c;
            lexType = LexType.SEMICN;
            return 0;
        } else if (c == ',') {
            token += c;
            lexType = LexType.COMMA;
            return 0;
        } else if (c == '(') {
            token += c;
            lexType = LexType.LPARENT;
            return 0;
        } else if (c == ')') {
            token += c;
            lexType = LexType.RPARENT;
            return 0;
        } else if (c == '[') {
            token += c;
            lexType = LexType.LBRACK;
            return 0;
        } else if (c == ']') {
            token += c;
            lexType = LexType.RBRACK;
            return 0;
        } else if (c == '{') {
            token += c;
            lexType = LexType.LBRACE;
            return 0;
        } else if (c == '}') {
            token += c;
            lexType = LexType.RBRACE;
            return 0;
        } else if (c == '<') {
            token += c;
            if (curPos < len && source.charAt(curPos) == '=') {
                c = source.charAt(curPos++);
                token += c;
                lexType = LexType.LEQ;
            } else {
                lexType = LexType.LSS;
            }
            return 0;
        } else if (c == '>') {
            token += c;
            if (curPos < len && source.charAt(curPos) == '=') {
                c = source.charAt(curPos++);
                token += c;
                lexType = LexType.GEQ;
            } else {
                lexType = LexType.GRE;
            }
            return 0;
        } else if (c == '=') {
            token += c;
            if (curPos < len && source.charAt(curPos) == '=') {
                c = source.charAt(curPos++);
                token += c;
                lexType = LexType.EQL;
            } else {
                lexType = LexType.ASSIGN;
            }
            return 0;
        } else if (c == '!') {
            token += c;
            if (curPos < len && source.charAt(curPos) == '=') {
                c = source.charAt(curPos++);
                token += c;
                lexType = LexType.NEQ;
            } else {
                lexType = LexType.NOT;
            }
            return 0;
        } else if (c == '&' && curPos < len && source.charAt(curPos) == '&') {
            token = "&&";
            lexType = LexType.AND;
            curPos++;
            return 0;
        } else if (c == '|' && curPos < len && source.charAt(curPos) == '|') {
            token = "||";
            lexType = LexType.OR;
            curPos++;
            return 0;
        } else if (c == '\"') {
            token += c;
            while (curPos < len && source.charAt(curPos) != '\"') {
                c = source.charAt(curPos++);
                token += c;
            }
            token += '\"';
            curPos++;
            lexType = LexType.STRCON;
            return 0;
        }
        return -1;
        //error
    }
}