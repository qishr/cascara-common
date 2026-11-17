package integration.test.fixtures.task;

import integration.test.fixtures.payload.SplRegisterClassTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;

public class SplRegisterClassTestTask extends AbstractExecutionTask<String, SplRegisterClassTestOutput> {
    private static final Reporter REPORTER = Reporter.forClass(SplRegisterClassTestTask.class);

    @Override
    public SplRegisterClassTestOutput run(String input) throws Exception {

        REPORTER.debug(input);

        try {
            String className = input;
            REPORTER.debug("className: " + className);
            Class<?> cls = Class.forName(className);
            SPL spl = SPL.getRoot();
            spl.registerClass(cls);
        } catch (Exception e) {
            System.err.println("SplRegisterClassTestTask: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return new SplRegisterClassTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input +
            ".\nLevel = " + REPORTER.getLevel()
        );
    }
}
