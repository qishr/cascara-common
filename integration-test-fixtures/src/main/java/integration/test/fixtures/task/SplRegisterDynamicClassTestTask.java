package integration.test.fixtures.task;

import java.lang.reflect.Proxy;
import java.util.Map;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceProvider;

public class SplRegisterDynamicClassTestTask extends AbstractExecutionTask<String, Boolean> {
    private static final Reporter REPORTER = Reporter.forClass(SplRegisterClassTestTask.class);

    @Override
    public Boolean run(String input) throws Exception {

        REPORTER.debug(input);

        System.out.println("Regsitering class");

        // Create a dynamic proxy instance with a working handler so SPL's property checks pass
        ServiceProvider proxyInstance = (ServiceProvider) Proxy.newProxyInstance(
            SplRegisterDynamicClassTestTask.class.getClassLoader(),
                new Class<?>[]{ ServiceProvider.class },
                (proxy, method, args) -> {
                    if ("getServiceProperties".equals(method.getName())) {
                        return Map.of(); // Return empty properties map cleanly
                    }
                    return null;
                }
        );

        // Register the class of the dynamic proxy
        Class<?> dynamicClass = proxyInstance.getClass();

        SPL spl = SPL.getRoot();
        spl.registerClass(dynamicClass);
        System.out.println("OK");

        return true;
    }
}
