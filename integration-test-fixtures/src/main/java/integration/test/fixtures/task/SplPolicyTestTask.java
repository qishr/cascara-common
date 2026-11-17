package integration.test.fixtures.task;

import integration.test.fixtures.payload.ReporterTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class SplPolicyTestTask extends AbstractExecutionTask<String, ReporterTestOutput> {
    private static final Reporter REPORTER = Reporter.forClass(SplPolicyTestTask.class);

    @Override
    public ReporterTestOutput run(String input) throws Exception {

        REPORTER.debug(input);

        // displayVfsContents();

        return new ReporterTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input +
            ".\nLevel = " + REPORTER.getLevel()
        );
    }
}
