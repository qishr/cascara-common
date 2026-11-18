package integration.test.fixtures.task;

import java.lang.reflect.Proxy;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceProvider;

public class SplRegisterProxyClassTestTask extends AbstractExecutionTask<String, Boolean> {
    private static final Reporter REPORTER = Reporter.forClass(SplRegisterClassTestTask.class);

    @Override
    public Boolean run(String input) throws Exception {

        REPORTER.debug(input);

        System.out.println("Regsitering class");

        @SuppressWarnings("deprecation")
        Class<?> proxyClass = Proxy.getProxyClass(
            SplRegisterProxyClassTestTask.class.getClassLoader(),
            ServiceProvider.class
        );

        SPL spl = SPL.getRoot();
        spl.registerClass(proxyClass);
        System.out.println("OK");

        return true;
    }
}
