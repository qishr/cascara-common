package integration.test.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.payload.FunctionInput;
import integration.test.fixtures.payload.ReporterTestOutput;
import integration.test.fixtures.task.FunctionTask;
import integration.test.fixtures.task.GlobalReporterTestTask;
import integration.test.fixtures.task.LocalizerTestTask;
import integration.test.fixtures.task.ThrowableTestTask;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.common.i18n.DetailedMessage;
import io.github.qishr.cascara.test.common.junit.extension.AssertReportedExtension;

public class UnserializableTests extends ExecTestBase {
    @RegisterExtension
    static final AssertReportedExtension assertReported = new AssertReportedExtension();

    @BeforeEach
    protected void setUp() {
        super.setUp();
    }

    // @FunctionalInterface
    // public static interface InnerConfigTests {
    //     Object f(Object x);
    // }


    @Test
    void test_config() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(FunctionTask.class, String.class)
            .setExceptionForwarding(true)
            .setDiagnosticForwarding(true);

        exec.setJvmConfig(new JvmConfig()
            .setModuleName(GlobalReporterTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
        );

        FunctionInput input = new FunctionInput();

        // InnerConfigTests f = (x) -> { return x; };

        Function<?,?> f = (x) -> { return x; };

        input.f = f;

        Response<String> response = exec.run(input);


        debug(response);
        assertNotNull(response);
    }
}
