import IRBuilder.Visitor;
import Lexer.Lexer;
import MIPSBuilder.MIPSBuilder;
import Parser.ASTNode;
import Parser.Parser;
import Error.Handler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) {
        String source = "";
        try (BufferedReader reader = new BufferedReader(new FileReader("testfile.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source += line;
                source += '\n';
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<ASTNode> lexicon = Lexer.getInstance().createLexicon(source);
        ASTNode root = Parser.getInstance().createAST(lexicon);
        Handler.getInstance().initHandler(true, root);
        Visitor.getInstance().initVisitor(root);
        MIPSBuilder.getInstance().initIRVisitor(Visitor.getInstance().getIRModule());
        ArrayList<String> ans = new ArrayList<>(Handler.getInstance().createAns());
        if (!ans.isEmpty()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("error.txt"))) {
                for (String string : ans) {
                    writer.write(string);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            ans = new ArrayList<>(Visitor.getInstance().createAns());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("llvm_ir.txt"))) {
                for (String string : ans) {
                    writer.write(string);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            ans = new ArrayList<>(MIPSBuilder.getInstance().createAns());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("mips.txt"))) {
                for (String string : ans) {
                    writer.write(string);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
