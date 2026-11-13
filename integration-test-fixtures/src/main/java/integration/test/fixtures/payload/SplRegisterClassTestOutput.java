package integration.test.fixtures.payload;

public class SplRegisterClassTestOutput {
    private int n;
    private String s;

    // TODO: Remove this
    public SplRegisterClassTestOutput() {}

    public SplRegisterClassTestOutput(int n, String s) {
        this.n = n;
        this.s = s;
    }

    public int n() { return n; }
    public String s() { return s; }
}
