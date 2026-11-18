package integration.test.fixtures.task;

import java.nio.file.Path;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;

public class SplRegisterJarTestTask extends AbstractExecutionTask<String, Boolean> {
    private static final Reporter REPORTER = Reporter.forClass(SplRegisterJarTestTask.class);

    @Override
    public Boolean run(String input) throws Exception {

        REPORTER.debug(input);

        System.out.println("Task: registering JAR");
        Path jarPath = Cascara.instance().getPath(input);
        SPL.getRoot().registerJar(jarPath);
        System.out.println("Task: OK");

        return true;
    }
}
