package integration.test.fixtures.beans;

import java.util.List;

import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceProvider;

public class SPLStatus implements SPLStatusMBean {
    private static final GlobalReporter REPORTER = GlobalReporter.forClass(SPLStatus.class);

    public SPLStatus() {
    }

    @Override
    public List<String> getRegisteredServiceProviders() {
        REPORTER.info(GenericMessage.INFO, "getRegisteredServiceProviders");
        return SPL.getRoot()
            .findAllProviders(ServiceProvider.class)
            .stream()
            .map(provider -> provider.getTypeName()) // + "(" + provider.getTitle() + ")")
            .sorted()
            .toList();
    }

    @Override
    public void test() {
        SPL.getRoot();
        REPORTER.info(GenericMessage.INFO, "test successful");
    }
}