package MIPSBuilder;

import Type.MIPSInstrType;

import java.util.ArrayList;

public class MIPSModule {
    private ArrayList<MIPSInstr> data;
    private ArrayList<MIPSInstr> text;
    private static int strNo;

    public MIPSModule() {
        this.data = new ArrayList<>();
        this.text = new ArrayList<>();
        strNo = 0;
        String mips = "__str__new__line__: .asciiz \"\\n\"";
        this.data.add(new MIPSInstr(MIPSInstrType.ASCIIZ, mips));
    }

    public void addData(MIPSInstr mipsInstr) {
        this.data.add(mipsInstr);
    }

    public void addText(MIPSInstr mipsInstr) {
        this.text.add(mipsInstr);
    }

    public int newStrNo() {
        int ans = strNo;
        strNo++;
        return ans;
    }

    public ArrayList<String> getMIPS() {
        ArrayList<String> ans = new ArrayList<>();
        ans.add(".data");
        for (MIPSInstr mipsInstr : data) {
            ans.add("\t" + mipsInstr.getMIPS());
        }
        ans.add(".text");
        for (int i = 0; i < text.size(); i++) {
            MIPSInstr mipsInstr = text.get(i);
            if (mipsInstr.getInstrType() == MIPSInstrType.J
                    && text.get(i + 1).getInstrType() == MIPSInstrType.LABEL) {
                String jLabel = mipsInstr.getMIPS().substring(2);
                String label = text.get(i + 1).getMIPS().strip().substring(0, text.get(i + 1).getMIPS().length() - 1);
                if (jLabel.equals(label)) {
                    continue;
                }
            }
            if (mipsInstr.getInstrType() != MIPSInstrType.LABEL) {
                ans.add("\t" + mipsInstr.getMIPS());
            } else {
                ans.add(mipsInstr.getMIPS());
            }
        }
        return ans;
    }

}
