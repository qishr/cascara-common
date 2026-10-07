package test.interfaces.task;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import test.interfaces.payload.ReporterTestInput;
import test.interfaces.payload.ReporterTestOutput;
import test.interfaces.VmInfo;

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
