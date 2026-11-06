package integration.test.fixtures.task;

import integration.test.fixtures.payload.StringTaskInput;
import integration.test.fixtures.payload.SplRegisterClassTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;

public class SplRegisterClassTestTask extends AbstractExecutionTask<StringTaskInput, SplRegisterClassTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public SplRegisterClassTestOutput run(StringTaskInput input) throws Exception {

        reporter.debug(input.getString());

        try {
            String className = input.getString();
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
            "Input was " + input.getString() +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
