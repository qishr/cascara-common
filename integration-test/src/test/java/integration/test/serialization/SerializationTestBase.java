package integration.test.serialization;

import org.junit.jupiter.api.BeforeEach;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.processor.ProcessorFactory;

public abstract class SerializationTestBase {
    protected GlobalReporter REPORTER;
    protected Serializer<?> serializer;

    @BeforeEach
    void setUp() {
        REPORTER = GlobalReporter.forClass(getClass());
        REPORTER.setLevel(Level.DEBUG);

        serializer = ProcessorFactory.system().createSerializer("application/json");
    }
}
