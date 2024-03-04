package MIPSBuilder;

import IRBuilder.IRModule;
import IRBuilder.Value.*;
import MIPSBuilder.Symbol.SymbolMIPSManager;
import Type.IRInstrType;
import Type.MIPSInstrType;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MIPSBuilder {
    private static volatile MIPSBuilder instance;
    private IRModule IRModule;
    private MIPSModule mipsModule;

    private MIPSBuilder() {
    }

    public void initIRVisitor(IRModule IRModule) {
        this.IRModule = IRModule;
        this.mipsModule = new MIPSModule();
    }

    public static MIPSBuilder getInstance() {
        if (instance == null) {
            instance = new MIPSBuilder();
        }
        return instance;
    }

    public ArrayList<String> createAns() {
        buildIRModuleMIPS();
        return mipsModule.getMIPS();
    }

    public ArrayList<String> parseIRInstr(IRInstr irInstr) {
        Pattern pattern = Pattern.compile("( |^)(%\\w+|@\\w+|(\\+|-)*\\d+)( |$|\\)|,)");
        Matcher matcher = pattern.matcher(irInstr.getIR());
        ArrayList<String> ans = new ArrayList<>();
        while (matcher.find()) {
            ans.add(matcher.group(2));
        }
        return ans;
    }

    public String parseCalledFunction(IRInstr irInstr) {
        Pattern pattern = Pattern.compile(" (@\\w+)\\(");
        Matcher matcher = pattern.matcher(irInstr.getIR());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    public ArrayList<Integer> parseArrayStruct(IRInstr irInstr) {
        Pattern pattern = Pattern.compile("\\[(\\d+) x \\[(\\d+) x \\w+\\]\\]");
        Matcher matcher = pattern.matcher(irInstr.getIR());
        ArrayList<Integer> ans = new ArrayList<>();
        if (matcher.find()) {
            ans.add(Integer.parseInt(matcher.group(1)));
            ans.add(Integer.parseInt(matcher.group(2)));
        } else {
            pattern = Pattern.compile("\\[(\\d+) x \\w+\\]");
            matcher = pattern.matcher(irInstr.getIR());
            if (matcher.find()) {
                ans.add(Integer.parseInt(matcher.group(1)));
            }
        }
        return ans;
    }

    public ArrayList<String> parseGlobalArrayInitVal(GlobalArrayDecl value) {
        ArrayList<String> ans = new ArrayList<>();
        if (value.getDim() == 1) {
            for (Value element : value.getConstValue()) {
                assert element instanceof GlobalDecl;
                ans.add(((GlobalDecl) element).getInitVal());
            }
        } else {
            for (Value element : value.getConstValue()) {
                assert element instanceof GlobalArrayDecl;
                ans.addAll(parseGlobalArrayInitVal((GlobalArrayDecl) element));
            }
        }
        return ans;
    }

    public void load(String irString, String register, Function function) {
        if (irString.indexOf('%') == -1 && irString.indexOf('@') == -1) {
            String mips = "li " + register + ", " + irString;
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
            mipsModule.addText(mipsInstr);
        } else if (irString.indexOf('@') == -1) {
            String temp = SymbolMIPSManager.getInstance().find(function.getToken() + "_" + irString);
            if (temp.matches("[\\+-]?\\d+")) {
                String mips = "lw " + register + ", " + temp + "($sp)";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LW, mips);
                mipsModule.addText(mipsInstr);
            } else {
                String mips = "lw " + register + ", " + temp;
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LW, mips);
                mipsModule.addText(mipsInstr);
            }
        } else {
            String temp = SymbolMIPSManager.getInstance().find(irString);
            String mips = "lw " + register + ", " + temp;
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LW, mips);
            mipsModule.addText(mipsInstr);
        }
    }

    public void loadAddress(String irString, String register, Function function) {
        if (irString.indexOf('@') == -1) {
            String temp = SymbolMIPSManager.getInstance().find(function.getToken() + "_" + irString);
            assert SymbolMIPSManager.getInstance().isPointer(function.getToken() + "_" + irString);
            if (SymbolMIPSManager.getInstance().isPara(function.getToken() + "_" + irString)) {
                String mips = "lw " + register + ", " + temp + "($sp)";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LW, mips);
                mipsModule.addText(mipsInstr);
            } else {
                String mips = "la " + register + ", " + temp + "($sp)";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LA, mips);
                mipsModule.addText(mipsInstr);
            }
        } else {
            String temp = SymbolMIPSManager.getInstance().find(irString);
            String mips = "la " + register + ", " + temp;
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LA, mips);
            mipsModule.addText(mipsInstr);
        }
    }

    public void register(ArrayList<String> irStrings, Function function) {//1 to 0
        String temp;
        if (irStrings.get(1).indexOf('%') == -1) {
            temp = SymbolMIPSManager.getInstance().find(irStrings.get(1));
        } else {
            assert irStrings.get(1).indexOf('@') == -1;
            temp = SymbolMIPSManager.getInstance().find(function.getToken() + "_" + irStrings.get(1));
        }
        SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), temp);
    }

    private int storeSp(String irString, Function function, int off) {//$t0
        String mips = "sw $t0, " + off + "($sp)";
        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
        mipsModule.addText(mipsInstr);
        SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irString, String.valueOf(off));
        off = off - 4;
        return off;
    }

    public void buildIRModuleMIPS() {
        ArrayList<Function> functions = new ArrayList<>();
        for (Value value : IRModule.getValues()) {
            if (value instanceof Function) {
                functions.add((Function) value);
            } else if (value instanceof GlobalDecl && !((GlobalDecl) value).isConst()) {
                buildGlobalDeclMIPS((GlobalDecl) value);
            } else if (value instanceof GlobalArrayDecl) {
                buildGlobalArrayDeclMIPS((GlobalArrayDecl) value);
            }
        }
        assert functions.get(functions.size() - 1).getToken().equals("main");
        buildFunctionMIPS(functions.get(functions.size() - 1));
        for (int i = 0; i < functions.size() - 1; i++) {
            buildFunctionMIPS(functions.get(i));
        }
    }

    public void buildGlobalDeclMIPS(GlobalDecl value) {
        String mips = value.getToken() + ": .word " + value.getInitVal();
        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.WORD, mips);
        mipsModule.addData(mipsInstr);
        SymbolMIPSManager.getInstance().register(value.getTemp(), value.getToken());
    }

    public void buildGlobalArrayDeclMIPS(GlobalArrayDecl value) {//register: <@a, a__GLOBAL_ARRAY__>
        StringBuilder initVal = new StringBuilder();
        ArrayList<String> stringList = parseGlobalArrayInitVal(value);
        for (String s : stringList) {
            initVal.append(s).append(" ");
        }
        String mips = value.getToken() + "__GLOBAL_ARRAY__" + ": .word " + initVal;
        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.WORD, mips);
        mipsModule.addData(mipsInstr);
        SymbolMIPSManager.getInstance().register(value.getTemp(), value.getToken() + "__GLOBAL_ARRAY__");
        SymbolMIPSManager.getInstance().addPointer(value.getToken() + "__GLOBAL_ARRAY__");
    }

    public void buildFunctionMIPS(Function function) {
        if (!function.getToken().equals("main")) {
            MIPSInstr funcLabel = new MIPSInstr(MIPSInstrType.LABEL, function.getToken() + ":");
            mipsModule.addText(funcLabel);
        }
        int spOff = -4;
        for (int i = 0; i < function.getBasicBlocks().size(); i++) {
            spOff = buildBasicBlockMIPS(function.getBasicBlocks().get(i), i, spOff, function);
        }
    }

    public int buildBasicBlockMIPS(BasicBlock basicBlock, int bbNo, int spOff, Function function) {
        int i = 0;
        if (bbNo == 0) {
            while (i < function.getParas().size() * 2) {
                boolean isPointer = function.getParas().get(i / 2) instanceof Pointer;
                spOff = buildParaMIPS(basicBlock.getIRInstrs().get(i + 1), i / 2, spOff, function, isPointer);
                i += 2;
            }
        } else {
            MIPSInstr bbLabel = new MIPSInstr(MIPSInstrType.LABEL, function.getToken() + "_" + basicBlock.getTemp() + ":");
            mipsModule.addText(bbLabel);
        }
        while (i < basicBlock.getIRInstrs().size()) {
            IRInstr irInstr = basicBlock.getIRInstrs().get(i);
            ArrayList<String> irStrings = parseIRInstr(irInstr);
            if (irInstr.getInstrType() == IRInstrType.ALLOCA) {
                spOff = buildAllocaMIPS(irStrings, irInstr, spOff, function);
            } else if (irInstr.getInstrType() == IRInstrType.LOAD) {
                spOff = buildLoadMIPS(irStrings, spOff, function);
            } else if (irInstr.getInstrType() == IRInstrType.STORE) {
                buildStoreMIPS(irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.ADD) {
                spOff = buildCalculateMIPS(IRInstrType.ADD, spOff, irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.SUB) {
                spOff = buildCalculateMIPS(IRInstrType.SUB, spOff, irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.MUL) {
                spOff = buildCalculateMIPS(IRInstrType.MUL, spOff, irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.SDIV) {
                spOff = buildCalculateMIPS(IRInstrType.SDIV, spOff, irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.SREM) {
                spOff = buildCalculateMIPS(IRInstrType.SREM, spOff, irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.ICMP) {
                if (basicBlock.getIRInstrs().get(i + 1).getInstrType() == IRInstrType.BR) {
                    buildBrMIPS(irStrings, irInstr, parseIRInstr(basicBlock.getIRInstrs().get(i + 1)), function);
                    i++;
                } else {
                    spOff = buildIcmpMIPS(irStrings, irInstr, spOff, function);
                }
            } else if (irInstr.getInstrType() == IRInstrType.ZEXT) {
                register(irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.BR) {
                buildBrMIPS(irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.RET) {
                buildRetMIPS(irStrings, function);
            } else if (irInstr.getInstrType() == IRInstrType.CALL) {
                String functionToken = parseCalledFunction(irInstr).substring(1);
                if (functionToken.equals("getint")) {
                    String mips = "li $v0, 5";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "syscall";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SYSCALL, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "sw $v0, " + spOff + "($sp)";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
                    SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), String.valueOf(spOff));
                    mipsModule.addText(mipsInstr);
                    spOff = spOff - 4;
                } else if (functionToken.equals("putch")) {
                    String label;
                    if (Integer.parseInt(irStrings.get(0)) == 10) {
                        label = "__str__new__line__";
                    } else {
                        String str = "\"";
                        str += (char) Integer.parseInt(irStrings.get(0));
                        while (basicBlock.getIRInstrs().get(i + 1).getInstrType() == IRInstrType.CALL
                                && parseCalledFunction(basicBlock.getIRInstrs().get(i + 1)).substring(1).equals("putch")
                                && Integer.parseInt(parseIRInstr(basicBlock.getIRInstrs().get(i + 1)).get(0)) != 10) {
                            str += (char) Integer.parseInt(parseIRInstr(basicBlock.getIRInstrs().get(i + 1)).get(0));
                            i++;
                        }
                        str += "\"";
                        label = "__str__" + mipsModule.newStrNo();
                        String mips = label + ": .asciiz " + str;
                        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ASCIIZ, mips);
                        mipsModule.addData(mipsInstr);
                    }
                    String mips = "la $a0, " + label;
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LA, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "li $v0, 4";
                    mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "syscall";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SYSCALL, mips);
                    mipsModule.addText(mipsInstr);
                } else if (functionToken.equals("putint")) {
                    load(irStrings.get(0), "$a0", function);
                    String mips = "li $v0, 1";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "syscall";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SYSCALL, mips);
                    mipsModule.addText(mipsInstr);
                } else {
                    spOff = buildCallMIPS(irStrings, irInstr, spOff, function);
                }
            } else if (irInstr.getInstrType() == IRInstrType.GEP) {
                spOff = buildGepMIPS(irStrings, irInstr, spOff, function);
            }
            i++;
        }
        return spOff;
    }

    public int buildParaMIPS(IRInstr storeIRInstr, int paraNo, int spOff, Function function, boolean isPointer) {
        ArrayList<String> store = parseIRInstr(storeIRInstr);
        int temp;
        if (paraNo < 4) {
            temp = spOff;
            String mips = "sw $a" + paraNo + ", " + temp + "($sp)";
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
            mipsModule.addText(mipsInstr);
            spOff = spOff - 4;
        } else {
            temp = (paraNo - 3) * 4;
        }
        SymbolMIPSManager.getInstance().register(function.getToken() + "_" + store.get(1), String.valueOf(temp));
        if (isPointer) {
            SymbolMIPSManager.getInstance().addPointer(function.getToken() + "_" + store.get(1));
        }
        return spOff;
    }

    public int buildAllocaMIPS(ArrayList<String> irStrings, IRInstr irInstr, int spOff, Function function) {
        int off = spOff;
        ArrayList<Integer> dims = parseArrayStruct(irInstr);
        if (dims.isEmpty()) {
            SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), String.valueOf(off));
        } else {
            int dim1 = dims.get(0);
            if (dims.size() == 2) {
                int dim2 = dims.get(1);
                off = off - 4 * dim1 * dim2 + 4;
                SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), String.valueOf(off));
                SymbolMIPSManager.getInstance().addPointer(function.getToken() + "_" + irStrings.get(0));
            } else {
                off = off - 4 * dim1 + 4;
                SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), String.valueOf(off));
                SymbolMIPSManager.getInstance().addPointer(function.getToken() + "_" + irStrings.get(0));
            }
        }
        off = off - 4;
        return off;
    }

    public int buildLoadMIPS(ArrayList<String> irStrings, int spOff, Function function) {
        if (SymbolMIPSManager.getInstance().isPointer(function.getToken() + "_" + irStrings.get(1))) {
            if (!irStrings.get(1).contains("__para")) {
                load(irStrings.get(1), "$t1", function);
                String mips = "lw $t0, 0($t1)";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
                mipsModule.addText(mipsInstr);
            } else {
                load(irStrings.get(1), "$t0", function);
                SymbolMIPSManager.getInstance().addPara(function.getToken() + "_" + irStrings.get(0));
            }
            String mips = "sw $t0, " + spOff + "($sp)";
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
            mipsModule.addText(mipsInstr);
            SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), String.valueOf(spOff));
            return spOff - 4;
        } else {
            register(irStrings, function);
            return spOff;
        }
    }

    public void buildStoreMIPS(ArrayList<String> irStrings, Function function) {
        load(irStrings.get(0), "$t0", function);
        String temp;
        if (irStrings.get(1).indexOf('@') == -1) {
            temp = SymbolMIPSManager.getInstance().find(function.getToken() + "_" + irStrings.get(1));
            if (SymbolMIPSManager.getInstance().isPointer(function.getToken() + "_" + irStrings.get(1))) {
                String mips = "lw $t1, " + temp + "($sp)";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LA, mips);
                mipsModule.addText(mipsInstr);
                mips = "sw $t0, 0($t1)";
                mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
                mipsModule.addText(mipsInstr);
            } else {
                String mips = "sw $t0, " + temp + "($sp)";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
                mipsModule.addText(mipsInstr);
            }
        } else {
            temp = SymbolMIPSManager.getInstance().find(irStrings.get(1));
            String mips = "sw $t0, " + temp;
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
            mipsModule.addText(mipsInstr);
        }
    }

    public void mulImm(String retRegister, String register, int immNum) {
        int neg = 0;
        if (immNum < 0) {
            neg = 1;
        }
        immNum = Math.abs(immNum);
        int power = (int) (Math.log(immNum) / Math.log(2));
        if ((immNum & (immNum - 1)) == 0) {//power of 2
            String mips = "sll " + retRegister + ", " + register + ", " + power;
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLL, mips);
            mipsModule.addText(mipsInstr);
        } else {//distance to power of 2 is lower than 4
            if (immNum > Math.pow(2, power) - (4 - neg) && immNum < Math.pow(2, power) + (4 - neg)) {
                String mips = "sll " + retRegister + ", " + register + ", " + power;
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLL, mips);
                mipsModule.addText(mipsInstr);
                for (int i = 0; i < Math.abs(Math.pow(2, power) - immNum); i++) {
                    mips = "addu " + retRegister + ", " + retRegister + ", " + register;
                    mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                    mipsModule.addText(mipsInstr);
                }
            } else if (immNum > Math.pow(2, power + 1) - (4 - neg) && immNum < Math.pow(2, power + 1) + (4 - neg)) {
                String mips = "sll " + retRegister + ", " + register + ", " + (power + 1);
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLL, mips);
                mipsModule.addText(mipsInstr);
                for (int i = 0; i < Math.abs(Math.pow(2, power + 1) - immNum); i++) {
                    mips = "addu " + retRegister + ", " + retRegister + ", " + register;
                    mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                    mipsModule.addText(mipsInstr);
                }
            } else {
                String mips = "mul " + retRegister + ", " + register + ", " + immNum;
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.MUL, mips);
                mipsModule.addText(mipsInstr);
            }
        }
        if (neg == 1) {
            String mips = "subu " + retRegister + ", $zero, " + retRegister;
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SUBU, mips);
            mipsModule.addText(mipsInstr);
        }
    }

    public int buildCalculateMIPS(IRInstrType irInstrType, int spOff, ArrayList<String> irStrings, Function function) {
        int imm = -1;
        if (irStrings.get(1).indexOf('%') == -1 && irStrings.get(1).indexOf('@') == -1) {
            imm = 1;
            load(irStrings.get(2), "$t1", function);
        } else if (irStrings.get(2).indexOf('%') == -1 && irStrings.get(2).indexOf('@') == -1) {
            imm = 2;
            load(irStrings.get(1), "$t1", function);
        } else {
            load(irStrings.get(1), "$t1", function);
            load(irStrings.get(2), "$t2", function);
        }
        if (irInstrType == IRInstrType.ADD) {
            if (imm != -1) {
                String mips = "addiu $t0, $t1, " + irStrings.get(imm);
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                mipsModule.addText(mipsInstr);
            } else {
                String mips = "addu $t0, $t1, $t2";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                mipsModule.addText(mipsInstr);
            }
        } else if (irInstrType == IRInstrType.SUB) {
            if (imm != -1) {
                if (imm == 2) {
                    String oppositeImm = String.valueOf(-Integer.parseInt(irStrings.get(imm)));
                    String mips = "addiu $t0, $t1, " + oppositeImm;
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                    mipsModule.addText(mipsInstr);
                } else {
                    String mips = "li $t2, " + irStrings.get(imm);
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "subu $t0, $t2, $t1";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SUBU, mips);
                    mipsModule.addText(mipsInstr);
                }
            } else {
                String mips = "subu $t0, $t1, $t2";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SUBU, mips);
                mipsModule.addText(mipsInstr);
            }
        } else if (irInstrType == IRInstrType.MUL) {
            if (imm != -1) {
                if (Integer.parseInt(irStrings.get(imm)) == 0) {
                    int off = spOff;
                    String mips = "sw $zero, " + off + "($sp)";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
                    mipsModule.addText(mipsInstr);
                    SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), String.valueOf(off));
                    off = off - 4;
                    return off;
                } else {
                    mulImm("$t0", "$t1", Integer.parseInt(irStrings.get(imm)));
                }
            } else {
                String mips = "mul $t0, $t1, $t2";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.MUL, mips);
                mipsModule.addText(mipsInstr);
            }
        } else if (irInstrType == IRInstrType.SDIV) {
            if (imm != -1) {
                String mips = "li $t2, " + irStrings.get(imm);
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                mipsModule.addText(mipsInstr);
                if (imm == 2) {
                    mips = "div $t1, $t2";
                    mipsInstr = new MIPSInstr(MIPSInstrType.DIV, mips);
                    mipsModule.addText(mipsInstr);
                } else {
                    mips = "div $t2, $t1";
                    mipsInstr = new MIPSInstr(MIPSInstrType.DIV, mips);
                    mipsModule.addText(mipsInstr);
                }
            } else {
                String mips = "div $t1, $t2";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.DIV, mips);
                mipsModule.addText(mipsInstr);
            }
            String mips = "mflo $t0";
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.MFLO, mips);
            mipsModule.addText(mipsInstr);
        } else if (irInstrType == IRInstrType.SREM) {
            if (imm != -1) {
                String mips = "li $t2, " + irStrings.get(imm);
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                mipsModule.addText(mipsInstr);
                if (imm == 2) {
                    mips = "div $t1, $t2";
                    mipsInstr = new MIPSInstr(MIPSInstrType.DIV, mips);
                    mipsModule.addText(mipsInstr);
                } else {
                    mips = "div $t2, $t1";
                    mipsInstr = new MIPSInstr(MIPSInstrType.DIV, mips);
                    mipsModule.addText(mipsInstr);
                }
            } else {
                String mips = "div $t1, $t2";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.DIV, mips);
                mipsModule.addText(mipsInstr);
            }
            String mips = "mfhi $t0";
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.MFHI, mips);
            mipsModule.addText(mipsInstr);
        }
        return storeSp(irStrings.get(0), function, spOff);
    }

    private void icmp(ArrayList<String> irStrings, IRInstr irInstr, Function function) {
        int imm = -1;
        if (irStrings.get(1).indexOf('%') == -1 && irStrings.get(1).indexOf('@') == -1) {
            imm = 1;
            load(irStrings.get(2), "$t1", function);
        } else if (irStrings.get(2).indexOf('%') == -1 && irStrings.get(2).indexOf('@') == -1) {
            imm = 2;
            load(irStrings.get(1), "$t1", function);
        } else {
            load(irStrings.get(1), "$t1", function);
            load(irStrings.get(2), "$t2", function);
        }
        Pattern pattern = Pattern.compile("icmp (\\w+) ");
        Matcher matcher = pattern.matcher(irInstr.getIR());
        String cond = "";
        if (matcher.find()) {
            cond = matcher.group(1);
        }
        if (imm != -1) {
            switch (cond) {
                case "eq": {
                    String oppositeImm = String.valueOf(-Integer.parseInt(irStrings.get(imm)));
                    String mips = "addiu $t0, $t1, " + oppositeImm;
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "sltiu $t0, $t0, 1";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SLTIU, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                case "ne": {
                    String oppositeImm = String.valueOf(-Integer.parseInt(irStrings.get(imm)));
                    String mips = "addiu $t0, $t1, " + oppositeImm;
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "sltu $t0, $zero, $t0";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SLTU, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                case "slt": {
                    if (imm == 2) {
                        if ((Integer.parseInt(irStrings.get(imm)) & 0xFFFF0000) != 0) {
                            String mips = "li $t2, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                            mipsModule.addText(mipsInstr);
                            mips = "slt $t0, $t1, $t2";
                            mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                            mipsModule.addText(mipsInstr);
                        } else {
                            String mips = "slti $t0, $t1, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLTI, mips);
                            mipsModule.addText(mipsInstr);
                        }
                    } else {
                        String mips = "li $t2, " + irStrings.get(imm);
                        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                        mipsModule.addText(mipsInstr);
                        mips = "slt $t0, $t2, $t1";
                        mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                        mipsModule.addText(mipsInstr);
                    }
                    break;
                }
                case "sgt": {
                    if (imm == 1) {
                        if ((Integer.parseInt(irStrings.get(imm)) & 0xFFFF0000) != 0) {
                            String mips = "li $t2, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                            mipsModule.addText(mipsInstr);
                            mips = "slt $t0, $t1, $t2";
                            mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                            mipsModule.addText(mipsInstr);
                        } else {
                            String mips = "slti $t0, $t1, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLTI, mips);
                            mipsModule.addText(mipsInstr);
                        }
                    } else {
                        String mips = "li $t2, " + irStrings.get(imm);
                        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                        mipsModule.addText(mipsInstr);
                        mips = "slt $t0, $t2, $t1";
                        mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                        mipsModule.addText(mipsInstr);
                    }
                    break;
                }
                case "sle": {
                    if (imm == 2) {
                        String mips = "sle $t0, $t1, " + irStrings.get(imm);
                        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLE, mips);
                        mipsModule.addText(mipsInstr);
                    } else {
                        if ((Integer.parseInt(irStrings.get(imm)) & 0xFFFF0000) != 0) {
                            String mips = "li $t2, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                            mipsModule.addText(mipsInstr);
                            mips = "slt $t0, $t1, $t2";
                            mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                            mipsModule.addText(mipsInstr);
                        } else {
                            String mips = "slti $t0, $t1, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLTI, mips);
                            mipsModule.addText(mipsInstr);
                        }
                        String mips = "li $t1, 1";
                        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                        mipsModule.addText(mipsInstr);
                        mips = "subu $t0, $t1, $t0";
                        mipsInstr = new MIPSInstr(MIPSInstrType.SUBU, mips);
                        mipsModule.addText(mipsInstr);
                    }
                    break;
                }
                case "sge": {
                    if (imm == 1) {
                        String mips = "sle $t0, $t1, " + irStrings.get(imm);
                        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLE, mips);
                        mipsModule.addText(mipsInstr);
                    } else {
                        if ((Integer.parseInt(irStrings.get(imm)) & 0xFFFF0000) != 0) {
                            String mips = "li $t2, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                            mipsModule.addText(mipsInstr);
                            mips = "slt $t0, $t1, $t2";
                            mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                            mipsModule.addText(mipsInstr);
                        } else {
                            String mips = "slti $t0, $t1, " + irStrings.get(imm);
                            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLTI, mips);
                            mipsModule.addText(mipsInstr);
                        }
                        String mips = "li $t1, 1";
                        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
                        mipsModule.addText(mipsInstr);
                        mips = "subu $t0, $t1, $t0";
                        mipsInstr = new MIPSInstr(MIPSInstrType.SUBU, mips);
                        mipsModule.addText(mipsInstr);
                    }
                    break;
                }
                default: {
                }
            }
        } else {
            switch (cond) {
                case "eq": {
                    String mips = "subu $t0, $t1, $t2";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SUBU, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "sltiu $t0, $t0, 1";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SLTIU, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                case "ne": {
                    String mips = "subu $t0, $t1, $t2";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SUBU, mips);
                    mipsModule.addText(mipsInstr);
                    mips = "sltu $t0, $zero, $t0";
                    mipsInstr = new MIPSInstr(MIPSInstrType.SLTU, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                case "slt": {
                    String mips = "slt $t0, $t1, $t2";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                case "sgt": {
                    String mips = "slt $t0, $t2, $t1";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLT, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                case "sle": {
                    String mips = "sle $t0, $t1, $t2";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLE, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                case "sge": {
                    String mips = "sle $t0, $t2, $t1";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLE, mips);
                    mipsModule.addText(mipsInstr);
                    break;
                }
                default: {
                }
            }
        }
    }

    public int buildIcmpMIPS(ArrayList<String> irStrings, IRInstr irInstr, int spOff, Function function) {
        icmp(irStrings, irInstr, function);
        return storeSp(irStrings.get(0), function, spOff);
    }

    public void buildBrMIPS(ArrayList<String> icmpStrings, IRInstr icmpInstr, ArrayList<String> brStrings, Function function) {
        assert icmpStrings.get(0).equals(brStrings.get(0));
        icmp(icmpStrings, icmpInstr, function);
        String labelTrue = function.getToken() + "_" + brStrings.get(1).substring(1);
        String labelFalse = function.getToken() + "_" + brStrings.get(2).substring(1);
        String mips = "beq $t0, $zero, " + labelFalse;
        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.BEQ, mips);
        mipsModule.addText(mipsInstr);
        mips = "j " + labelTrue;
        mipsInstr = new MIPSInstr(MIPSInstrType.J, mips);
        mipsModule.addText(mipsInstr);
    }

    public void buildBrMIPS(ArrayList<String> irStrings, Function function) {
        if (irStrings.size() == 1) {
            String label = function.getToken() + "_" + irStrings.get(0).substring(1);
            String mips = "j " + label;
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.J, mips);
            mipsModule.addText(mipsInstr);
        } else {
            String labelTrue = function.getToken() + "_" + irStrings.get(1).substring(1);
            String labelFalse = function.getToken() + "_" + irStrings.get(2).substring(1);
            String mips = "j ";
            int cond = Integer.parseInt(irStrings.get(0));
            if (cond == 0) {
                mips += labelFalse;
            } else {
                mips += labelTrue;
            }
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.J, mips);
            mipsModule.addText(mipsInstr);
        }
    }

    public int buildCallMIPS(ArrayList<String> irStrings, IRInstr irInstr, int spOff, Function function) {
        String label = parseCalledFunction(irInstr).substring(1);
        int off = spOff;
        int paraStart;
        if (irInstr.getIR().indexOf("call") == 0) {
            paraStart = 0;
        } else {
            paraStart = 1;
        }
        int raOff = off;
        String mips = "sw $ra, " + off + "($sp)";
        MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
        mipsModule.addText(mipsInstr);
        off = off - 4;
        for (int i = irStrings.size() - 1; i >= 4 + paraStart; i--) {
//            boolean la = false;
//            String temp = "";
//            if (irStrings.get(i).indexOf('@') != -1) {
//                temp = SymbolMIPSManager.getInstance().find(irStrings.get(i));
//                if (SymbolMIPSManager.getInstance().isPointer(temp)) {
//                    la = true;
//                }
//            } else if (irStrings.get(i).indexOf('%') != -1) {
//                temp = SymbolMIPSManager.getInstance().find(function.getToken() + "_" + irStrings.get(i));
//                if (!temp.matches("[\\+-]?\\d+") && SymbolMIPSManager.getInstance().isPointer(temp)) {//registered global
//                    la = true;
//                } else if (temp.matches("[\\+-]?\\d+")  //registered local
//                        && SymbolMIPSManager.getInstance().isPointer(function.getToken() + "_" + irStrings.get(i))) {
//                    la = true;
//                    temp = temp + "($sp)";
//                }
//            }
//            if (la) {
//                mips = "la $t0, " + temp;
//                mipsInstr = new MIPSInstr(MIPSInstrType.LA, mips);
//                mipsModule.addText(mipsInstr);
//            } else {
//                load(irStrings.get(i), "$t0", function);
//            }
            load(irStrings.get(i), "$t0", function);
            mips = "sw $t0, " + off + "($sp)";
            mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
            mipsModule.addText(mipsInstr);
            off = off - 4;
        }
        for (int i = Math.min(3 + paraStart, irStrings.size() - 1); i >= paraStart; i--) {
//            boolean la = false;
//            String temp = "";
//            if (irStrings.get(i).indexOf('@') != -1) {
//                temp = SymbolMIPSManager.getInstance().find(irStrings.get(i));
//                if (SymbolMIPSManager.getInstance().isPointer(temp)) {
//                    la = true;
//                }
//            } else if (irStrings.get(i).indexOf('%') != -1) {
//                temp = SymbolMIPSManager.getInstance().find(function.getToken() + "_" + irStrings.get(i));
//                if (!temp.matches("[\\+-]?\\d+") && SymbolMIPSManager.getInstance().isPointer(temp)) {
//                    la = true;
//                } else if (temp.matches("[\\+-]?\\d+")
//                        && SymbolMIPSManager.getInstance().isPointer(function.getToken() + "_" + irStrings.get(i))) {
//                    la = true;
//                    temp = temp + "($sp)";
//                }
//            }
//            if (la) {
//                mips = "la $a" + (i - paraStart) + ", " + temp;
//                mipsInstr = new MIPSInstr(MIPSInstrType.LA, mips);
//                mipsModule.addText(mipsInstr);
//            } else {
//                load(irStrings.get(i), "$a" + (i - paraStart), function);
//            }
            load(irStrings.get(i), "$a" + (i - paraStart), function);
        }
        mips = "addiu $sp, $sp, " + off;
        mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
        mipsModule.addText(mipsInstr);
        mips = "jal " + label;
        mipsInstr = new MIPSInstr(MIPSInstrType.JAL, mips);
        mipsModule.addText(mipsInstr);
        mips = "addiu $sp, $sp, " + (-off);
        mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
        mipsModule.addText(mipsInstr);
        mips = "lw $ra, " + raOff + "($sp)";
        mipsInstr = new MIPSInstr(MIPSInstrType.LW, mips);
        mipsModule.addText(mipsInstr);
        off = spOff;
        if (paraStart == 1) {
            mips = "sw $v0, " + off + "($sp)";
            mipsInstr = new MIPSInstr(MIPSInstrType.SW, mips);
            mipsModule.addText(mipsInstr);
            SymbolMIPSManager.getInstance().register(function.getToken() + "_" + irStrings.get(0), String.valueOf(off));
            off = off - 4;
        }
        return off;
    }

    public void buildRetMIPS(ArrayList<String> irStrings, Function function) {
        if (function.getToken().equals("main")) {
            String mips = "li $v0, 10";
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.LI, mips);
            mipsModule.addText(mipsInstr);
            mips = "syscall";
            mipsInstr = new MIPSInstr(MIPSInstrType.SYSCALL, mips);
            mipsModule.addText(mipsInstr);
        } else {
            if (!irStrings.isEmpty()) {
                load(irStrings.get(0), "$v0", function);
            }
            String mips = "jr $ra";
            MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.JR, mips);
            mipsModule.addText(mipsInstr);
        }
    }

    public int buildGepMIPS(ArrayList<String> irStrings, IRInstr irInstr, int spOff, Function function) {
        ArrayList<Integer> dims = parseArrayStruct(irInstr);
        if (dims.size() == 2 && irStrings.size() == 4) {//array with two 0 offset
            loadAddress(irStrings.get(1), "$t0", function);
        } else {
            loadAddress(irStrings.get(1), "$t1", function);
        }
        if (dims.size() == 0) {
            if (irStrings.get(2).indexOf('%') == -1 && irStrings.get(2).indexOf('@') == -1) {
                String mips = "addiu $t0, $t1, " + 4 * Integer.parseInt(irStrings.get(2));
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                mipsModule.addText(mipsInstr);
            } else {
                load(irStrings.get(2), "$t2", function);
                String mips = "sll $t3, $t2, 2";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.SLL, mips);
                mipsModule.addText(mipsInstr);
                mips = "addu $t0, $t1, $t3";
                mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                mipsModule.addText(mipsInstr);
            }
        } else if (dims.size() == 1) {
            if (irStrings.size() == 3) {//pointer to 1 dim array
                if (irStrings.get(2).indexOf('%') == -1 && irStrings.get(2).indexOf('@') == -1) {
                    String mips = "addiu $t0, $t1, " + 4 * Integer.parseInt(irStrings.get(2)) * dims.get(0);
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                    mipsModule.addText(mipsInstr);
                } else {
                    load(irStrings.get(2), "$t2", function);
                    mulImm("$t3", "$t2", 4 * dims.get(0));
                    String mips = "addu $t0, $t1, $t3";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                    mipsModule.addText(mipsInstr);
                }
                SymbolMIPSManager.getInstance().addPara(function.getToken() + "_" + irStrings.get(0));
            } else {//1 dim array, first offset is 0
                if (irStrings.get(3).indexOf('%') == -1 && irStrings.get(3).indexOf('@') == -1) {
                    String mips = "addiu $t0, $t1, " + 4 * Integer.parseInt(irStrings.get(3));
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                    mipsModule.addText(mipsInstr);
                } else {
                    load(irStrings.get(3), "$t2", function);
                    mulImm("$t3", "$t2", 4);
                    String mips = "addu $t0, $t1, $t3";
                    MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                    mipsModule.addText(mipsInstr);
                }
            }
        } else if (irStrings.size() == 5) {//2 dim array, first offset is 0
            if (irStrings.get(3).indexOf('%') == -1 && irStrings.get(3).indexOf('@') == -1
                    && irStrings.get(4).indexOf('%') == -1 && irStrings.get(4).indexOf('@') == -1) {
                String mips = "addiu $t0, $t1, " + (4 * dims.get(1) * Integer.parseInt(irStrings.get(3))
                        + 4 * Integer.parseInt(irStrings.get(4)));
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                mipsModule.addText(mipsInstr);
            } else if (irStrings.get(4).indexOf('%') == -1 && irStrings.get(4).indexOf('@') == -1) {
                load(irStrings.get(3), "$t2", function);
                mulImm("$t3", "$t2", 4 * dims.get(1));
                String mips = "addu $t0, $t1, $t3";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                mipsModule.addText(mipsInstr);
                mips = "addiu $t0, $t0," + 4 * Integer.parseInt(irStrings.get(4));
                mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                mipsModule.addText(mipsInstr);
            } else if (irStrings.get(3).indexOf('%') == -1 && irStrings.get(3).indexOf('@') == -1) {
                String mips = "addiu $t0, $t1, " + 4 * dims.get(1) * Integer.parseInt(irStrings.get(3));
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDIU, mips);
                mipsModule.addText(mipsInstr);
                load(irStrings.get(4), "$t2", function);
                mulImm("$t3", "$t2", 4);
                mips = "addu $t0, $t0, $t3";
                mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                mipsModule.addText(mipsInstr);
            } else {
                load(irStrings.get(3), "$t2", function);
                mulImm("$t3", "$t2", 4 * dims.get(1));
                String mips = "addu $t0, $t1, $t3";
                MIPSInstr mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                mipsModule.addText(mipsInstr);
                load(irStrings.get(4), "$t2", function);
                mulImm("$t3", "$t2", 4);
                mips = "addu $t0, $t0, $t3";
                mipsInstr = new MIPSInstr(MIPSInstrType.ADDU, mips);
                mipsModule.addText(mipsInstr);
            }
        }
        SymbolMIPSManager.getInstance().addPointer(function.getToken() + "_" + irStrings.get(0));
        return storeSp(irStrings.get(0), function, spOff);
    }
}
