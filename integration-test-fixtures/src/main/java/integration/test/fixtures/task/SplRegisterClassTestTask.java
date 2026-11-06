package integration.test.fixtures.task;

import integration.test.fixtures.payload.StringTaskInput;
import integration.test.fixtures.payload.SplRegisterClassTestOutput;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ModulePath;

public class SplRegisterClassTestTask extends AbstractExecutionTask<StringTaskInput, SplRegisterClassTestOutput> {
    GlobalReporter reporter = GlobalReporter.forClass(GlobalReporterTestTask.class);

    @Override
    public SplRegisterClassTestOutput run(StringTaskInput input) throws Exception {

        reporter.debug(input.getString());

        // VmInfo info = new VmInfo();
        // info.init();

        // System.out.println("pid = " + info.rtPid);
        // System.out.println("uptime = " + info.rtUptime + " ms");

        ModulePath mp = new ModulePath(Cascara.instance().getModulePath());
        for (String cls : mp.getClasses()) {
            if (cls.contains("ProviderA")) {
                System.out.println("PROVIDER A on module path");
            }
            // System.out.println(cls);
        }

        try {
            String className = input.getString();
            reporter.debug("className: " + className);
            Class<?> cls = Class.forName(className);
            SPL spl = SPL.getRoot();
            spl.registerClass(cls);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        // System.out.println(System.getProperty("jdk.module.path"));

        // try(Stream<Path> jarFiles = Files.walk(Cascara.instance().getModulePath())) {
        //     for (Path jarFile : jarFiles.toList()) {
        //         System.out.println("> " + jarFile);
        //     }
        // }

        // System.out.println(Cascara.instance().getHomePath());

        return new SplRegisterClassTestOutput(
            42,
            "Hello from the isolated test process.\n" +
            "Input was " + input.getString() +
            ".\nLevel = " + reporter.getLevel()
        );
    }
}
