package integration.test.serialization;

import org.junit.jupiter.api.BeforeEach;

import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;

public abstract class SerializerTestBase {
    protected Serializer<?> serializer;

    @BeforeEach
    void setUp() {

        serializer = ProcessorFactory.system().createSerializer("application/json");

        // For diagnostics
        serializer.setProviderFactoryEnabled(true);
    }
}
