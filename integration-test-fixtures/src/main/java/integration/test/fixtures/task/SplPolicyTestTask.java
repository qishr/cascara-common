package integration.test.fixtures.task;

import integration.test.fixtures.payload.ReporterTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class SplPolicyTestTask extends AbstractExecutionTask<String, ReporterTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public ReporterTestOutput run(String input) throws Exception {

        reporter.debug(input);

        // displayVfsContents();

        return new ReporterTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
