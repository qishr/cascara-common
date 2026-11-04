package integration.test.fixtures.task;

import integration.test.fixtures.VmInfo;
import integration.test.fixtures.payload.ReporterTestInput;
import integration.test.fixtures.payload.ReporterTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class GlobalReporterTestTask extends AbstractExecutionTask<ReporterTestInput, ReporterTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public ReporterTestOutput run(ReporterTestInput input) throws Exception {

        reporter.debug(input.testMessage());

        VmInfo info = new VmInfo();
        info.init();

        System.out.println("pid = " + info.rtPid);
        System.out.println("uptime = " + info.rtUptime + " ms");

        return new ReporterTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input.testMessage() +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
