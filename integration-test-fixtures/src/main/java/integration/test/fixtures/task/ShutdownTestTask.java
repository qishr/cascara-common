package integration.test.fixtures.task;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class ShutdownTestTask extends AbstractExecutionTask<String, String> {
    private static final Reporter REPORTER = Reporter.forClass(ShutdownTestTask.class);

    @Override
    public String run(String input) throws Exception {
        // TimeSequencedAggregatorLogger aggregator = new TimeSequencedAggregatorLogger(new ConsoleLogger(), 800);
        // GlobalReporter.globalInstance().addLogger(aggregator);
        // // GlobalReporter.globalInstance().setSystemOutputEnabled(false);

        // TODO

        // TimeSequencedAggregatorLogger aggregator = new TimeSequencedAggregatorLogger(
        //     new Logger() {
        //         @Override public Logger log(Diagnostic diagnostic) {
        //             aggregate.add(diagnostic);
        //             return this;
        //         }

        //         @Override public Logger flush() {
        //             return this;
        //         }

        //         @Override public Logger setFormatConfig(FormatConfig config) {
        //             return this;
        //         }
        //     },
        //      800
        // );
        // GlobalReporter.globalInstance().addLogger(aggregator);
        // GlobalReporter.globalInstance().setSystemOutputEnabled(false);

        REPORTER.debug(input);

        // displayHomeContents();
        return "";
    }
}
