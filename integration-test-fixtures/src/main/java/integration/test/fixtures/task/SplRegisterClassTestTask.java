package integration.test.fixtures.task;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;

public class SplRegisterClassTestTask extends AbstractExecutionTask<String, Boolean> {
    private static final Reporter REPORTER = Reporter.forClass(SplRegisterClassTestTask.class);

    @Override
    public Boolean run(String input) throws Exception {

        REPORTER.debug(input);

        System.out.println("Regsitering class");

        String className = input;
        REPORTER.debug("className: " + className);
        Class<?> cls = Class.forName(className);
        SPL spl = SPL.getRoot();
        spl.registerClass(cls);
        System.out.println("OK");

        return true;
    }
}
