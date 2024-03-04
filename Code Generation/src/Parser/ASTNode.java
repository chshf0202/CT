package Parser;

import Type.GrammarType;

import java.util.ArrayList;

public class ASTNode {
    private ASTNode father;
    private ArrayList<ASTNode> children;
    private GrammarType type;

    public ASTNode(ASTNode father, GrammarType type) {
        this.father = father;
        this.children = new ArrayList<>();
        this.type = type;
    }

    public void setFather(ASTNode father) {
        this.father = father;
    }

    public void addChild(ASTNode child) {
        children.add(child);
    }

    public GrammarType getGrammarType() {
        return type;
    }

    public ASTNode getFather() {
        return father;
    }

    public ArrayList<ASTNode> getChildren() {
        return children;
    }
}
