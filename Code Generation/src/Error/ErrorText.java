package Error;

public class ErrorText {
    private final int lineNum;
    private final char id;

    public ErrorText(int lineNum, char id) {
        this.lineNum = lineNum;
        this.id = id;
    }

    @Override
    public String toString() {
        return lineNum + " " + id;
    }
}
