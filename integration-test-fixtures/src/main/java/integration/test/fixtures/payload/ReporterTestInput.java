package integration.test.fixtures.payload;

public class ReporterTestInput {
    private String testMessage;

    // TODO: Remove this
    public ReporterTestInput() {}

    public ReporterTestInput(String msg) {
        this.testMessage = msg;
    }

    public String testMessage() {
        return testMessage;
    }
}
