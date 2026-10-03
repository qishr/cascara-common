package test.task;

import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import test.interfaces.ReporterTestInput;
import test.interfaces.ReporterTestOutput;

// TODO: this could run as standalone or as test task

public class GlobalReporterTestTask extends AbstractExecutionTask<ReporterTestInput, ReporterTestOutput> {
    @Override
    public ReporterTestOutput run(ReporterTestInput input) throws Exception {
        return new ReporterTestOutput(
            42, "Hello from the isolated test process. Input was " + input.testMessage()
        );
    }
}


// GlobalReporter.globalInstance().setLineConsumer(System.out::println);
// GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);
// reporter.debug("test-env-msg");




        // // GlobalReporter initialized cleanly from startup Env / System Properties
        // GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

        // List<String> captured = new ArrayList<>();
        // GlobalReporter.globalInstance().setLineConsumer(captured::add);

        // reporter.debug(input.testMessage());

        // return new ReporterTestOutput(
        //     captured.size(),
        //     captured.isEmpty() ? null : captured.getFirst()
        // );
