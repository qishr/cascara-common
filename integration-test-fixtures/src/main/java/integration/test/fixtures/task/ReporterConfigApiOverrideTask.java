package integration.test.fixtures.task;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class ReporterConfigApiOverrideTask extends AbstractExecutionTask<String, String> {
    private static final Reporter REPORTER = Reporter.forClass(ReporterConfigTask.class);

    @Override
    public String run(String input) throws Exception {
        REPORTER.setLevel(Level.TRACE);
        REPORTER.debug(input);
        return "";
    }
}
