package integration.test.fixtures.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import integration.test.fixtures.VmInfo;
import integration.test.fixtures.payload.ReporterTestInput;
import integration.test.fixtures.payload.ReporterTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ModulePath;

public class SplPolicyTestTask extends AbstractExecutionTask<ReporterTestInput, ReporterTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public ReporterTestOutput run(ReporterTestInput input) throws Exception {

        reporter.debug(input.testMessage());

        VmInfo info = new VmInfo();
        info.init();

        System.out.println("pid = " + info.rtPid);
        System.out.println("uptime = " + info.rtUptime + " ms");

        ModulePath mp = new ModulePath(Cascara.getModulePath());
        for (String cls : mp.getClasses()) {
            if (cls.contains("ProviderA")) {
                System.out.println("PROVIDER A on module path");
            }

            // System.out.println(cls);
        }

        System.out.println(System.getProperty("jdk.module.path"));

        try(Stream<Path> jarFiles = Files.walk(Cascara.getModulePath())) {
            for (Path jarFile : jarFiles.toList()) {
                System.out.println("> " + jarFile);
            }
        }

        // System.out.println(Cascara.getHomePath());

        return new ReporterTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input.testMessage() +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
