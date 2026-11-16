package integration.test.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.payload.MessageEnumTestInput;
import integration.test.fixtures.task.LocalizerTestTask;
import integration.test.fixtures.task.MessageEnumTestTask;
import integration.test.fixtures.task.ThrowableTestTask;
import integration.test.jar.JarSignatureValidTests;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.common.i18n.DetailedMessage;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;

public class IsolatedInternationalizationTests extends ExecTestBase {

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
    }

    @Test
    void test_overridesEnumMessage() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(LocalizerTestTask.class, String.class);
        exec.setExceptionForwarding(true);

        exec.setJvmConfig(new JvmConfig()
            .setModuleName(ThrowableTestTask.class.getModule().getName())
            .setModulePath(getModulePath())
            .setSystemProperty("cascara.lang.locale", "fr-FR")
            .setSystemProperty("cascara.lang.localize-exceptions", "true")
            .setTimeout(Duration.ofSeconds(20))
        );

        DetailedMessage dm = new DetailedMessage(GenericMessage.CLASS_CAST_EXCEPTION, "Class1", "Class2");
        Response<String> response = exec.run(dm);
        debug(response);
        assertNotNull(response);
        assertEquals("La classe Class1 ne peut pas être lancée sur Class2", response.getPayload());
    }

    @Test
    void test_overridesEnumMessage_NonJPMS() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(LocalizerTestTask.class, String.class);
        exec.setExceptionForwarding(true);

        exec.setJvmConfig(new JvmConfig()
            .setClassPath(getModulePath())
            .setSystemProperty("cascara.lang.locale", "fr-FR")
            .setSystemProperty("cascara.lang.localize-exceptions", "true")
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(true)
        );

        DetailedMessage dm = new DetailedMessage(GenericMessage.CLASS_CAST_EXCEPTION, "Class1", "Class2");
        Response<String> response = exec.run(dm);
        debug(response);
        assertNotNull(response);
        assertEquals("La classe Class1 ne peut pas être lancée sur Class2", response.getPayload());
    }


    // Build a non-modular JAR with localization resources and get
    // the isolated JVM to load that, and format a message with
    // the JAR's resources
    @Test
    void test_overridesEnumMessage_NonJPMS_customJar() throws Throwable {
        IsolatedExecutor exec = IsolatedExecutor.forTask(MessageEnumTestTask.class, String.class)
            .setExceptionForwarding(true)
            .setDiagnosticForwarding(true);
            // .setIpcDebug(true)

        SyntheticJar testJar = SyntheticJar.forUseIn(IsolatedInternationalizationTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addDependencies("cascara-common")
            .addSources("code/i18n/module-info.txt", "code/i18n/TestMessage.txt")
            .addResource("META-INF/es-ES.properties", """
                meta.lang = es-ES
                meta.title = Español
                provider-a.test-message.test-101 = Mensaje de prueba
                """)
            .packageJar();

        String classPath = getModulePath() + File.pathSeparator + testJar.getPath();

        exec.setJvmConfig(new JvmConfig()
            .setClassPath(classPath)
            .setSystemProperty("cascara.lang.locale", "es-ES")
            .setSystemProperty("cascara.lang.localize-exceptions", "true")
            .setTimeout(Duration.ofSeconds(20))
            // .setDebug(true)
        );

        MessageEnumTestInput input = new MessageEnumTestInput();
        input.messageEnumFqcn = "com.example.providera.TestMessage";
        input.translationPath = "/META-INF/es-ES.properties";

        Response<String> response = exec.run(input);

        debug(response);
        assertNotNull(response);
        assertEquals("Mensaje de prueba", response.getPayload());
    }
}
