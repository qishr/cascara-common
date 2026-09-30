package test.interfaces;

import java.util.UUID;

import io.github.qishr.cascara.common.annotation.NoAutoRegistration;
import io.github.qishr.cascara.common.service.ServiceProvider;

@NoAutoRegistration
public interface DemoSingleton extends ServiceProvider {
    int getInitCount();
    UUID getUuid();
}
