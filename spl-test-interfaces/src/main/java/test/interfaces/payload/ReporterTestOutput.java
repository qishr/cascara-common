package test.interfaces.payload;

public class ReporterTestOutput {
    private int n;
    private String s;

    // TODO: Remove this
    public ReporterTestOutput() {}

    public ReporterTestOutput(int n, String s) {
        this.n = n;
        this.s = s;
    }

    public int n() { return n; }
    public String s() { return s; }
}
