package integration.test.fixtures.task;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class ReporterConfigTask extends AbstractExecutionTask<String, String> {
    private static final Reporter REPORTER = Reporter.forClass(ReporterConfigTask.class);

    @Override
    public String run(String input) throws Exception {
        REPORTER.debug(input);
        // displayHomeContents();
        return "";
    }
}
