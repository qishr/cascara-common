package integration.test.fixtures.task;

import integration.test.fixtures.payload.StringTaskInput;
import integration.test.fixtures.payload.ReporterTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class SplPolicyTestTask extends AbstractExecutionTask<StringTaskInput, ReporterTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public ReporterTestOutput run(StringTaskInput input) throws Exception {

        reporter.debug(input.getString());

        // displayVfsContents();

        return new ReporterTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input.getString() +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
