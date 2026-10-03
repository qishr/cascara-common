package test.task;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import test.interfaces.ReporterTestInput;
import test.interfaces.ReporterTestOutput;

public class GlobalReporterTestTask extends AbstractExecutionTask<ReporterTestInput, ReporterTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public ReporterTestOutput run(ReporterTestInput input) throws Exception {

        reporter.debug(input.testMessage());

        return new ReporterTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input.testMessage() +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
