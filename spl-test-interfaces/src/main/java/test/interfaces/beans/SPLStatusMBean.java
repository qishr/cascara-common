package test.interfaces.beans;

import java.util.List;


public interface SPLStatusMBean {
    // New operation to retrieve registered ServiceProvider implementations
    List<String> getRegisteredServiceProviders();

    void test();
}