package integration.test.fixtures.payload;

public class StringTaskInput {
    private String testMessage;

    // TODO: Remove this
    public StringTaskInput() {}

    public StringTaskInput(String msg) {
        this.testMessage = msg;
    }

    public String getString() {
        return testMessage;
    }
}
