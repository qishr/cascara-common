package integration.test.fixtures.task;

import integration.test.fixtures.VmInfo;
import integration.test.fixtures.payload.ReporterTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class GlobalReporterTestTask extends AbstractExecutionTask<String, ReporterTestOutput> {
    Reporter reporter = Reporter.forClass(GlobalReporterTestTask.class);

    @Override
    public ReporterTestOutput run(String input) throws Exception {
        reporter.debug(input);

        VmInfo info = new VmInfo();
        info.init();

        System.out.println("pid = " + info.rtPid);
        System.out.println("uptime = " + info.rtUptime + " ms");

        return new ReporterTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
