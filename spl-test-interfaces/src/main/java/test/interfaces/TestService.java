package test.interfaces;

import io.github.qishr.cascara.common.annotation.NoAutoRegistration;
import io.github.qishr.cascara.common.service.ServiceProvider;

@NoAutoRegistration
public interface TestService extends ServiceProvider {
    String getName();
}