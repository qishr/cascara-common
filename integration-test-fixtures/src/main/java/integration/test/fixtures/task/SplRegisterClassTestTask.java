package integration.test.fixtures.task;

import integration.test.fixtures.payload.SplRegisterClassTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;

public class SplRegisterClassTestTask extends AbstractExecutionTask<String, SplRegisterClassTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public SplRegisterClassTestOutput run(String input) throws Exception {

        reporter.debug(input);

        try {
            String className = input;
            reporter.debug("className: " + className);
            Class<?> cls = Class.forName(className);
            SPL spl = SPL.getRoot();
            spl.registerClass(cls);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return new SplRegisterClassTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
