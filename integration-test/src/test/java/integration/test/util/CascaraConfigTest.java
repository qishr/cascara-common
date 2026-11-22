package integration.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.task.CascaraConfigTask;
import integration.test.fixtures.task.ReporterConfigTask;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.test.common.util.TestCascara;

public class CascaraConfigTest extends ExecTestBase {
    @Test
    void test_defaultCascaraConfig() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            // 1. Create a Cascara properties file
            String cascaraProperties = """
                """;
            Path cascaraPropertiesPath = cascara.getHomePath().resolve("shared/cascara.properties");
            Files.createDirectories(cascaraPropertiesPath.getParent());
            Files.writeString(cascaraPropertiesPath, cascaraProperties);

            // 2. Set up isolated executor
            IsolatedExecutor exec = IsolatedExecutor.forTask(CascaraConfigTask.class, Map.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setTimeout(Duration.ofSeconds(120))
                        .setModuleName(ReporterConfigTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                );

            // 3. Run the test task
            Response<Map<String,String>> response = exec.run(null);
            debug(response);
            Map<String,String> settings = response.getPayload();

            assertEquals(cascaraPropertiesPath.toString(), settings.get("cascara.properties"));
            assertEquals(cascara.getHomePath().resolve("cascara.spl.properties").toString(), settings.get("cascara.spl.properties"));

            assertEquals(cascara.getHomePath().resolve("shared").toString(), settings.get("cascara.shared.path"));
            assertEquals(cascara.getHomePath().resolve("modulepath").toString(), settings.get("cascara.module.path"));
            assertEquals(cascara.getHomePath().resolve("shared/schemas").toString(), settings.get("cascara.schema.path"));
            assertEquals(cascara.getHomePath().resolve("shared/content-types.yaml").toString(), settings.get("cascara.content-types.path"));
        }
    }

    @Test
    void test_customSplPath() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            // 1. Create a Cascara properties file
            String cascaraProperties = """
                """;
            Path cascaraPropertiesPath = cascara.getHomePath().resolve("shared/cascara.properties");
            Files.createDirectories(cascaraPropertiesPath.getParent());
            Files.writeString(cascaraPropertiesPath, cascaraProperties);

            // 2. Create an SPL properties file
            // Path splPropertiesPath = cascaraPropertiesPath.getParent().resolve("custom-spl.properties");
            Path splPropertiesPath = cascara.getHomePath().resolve("custom-spl.properties");
            Files.writeString(splPropertiesPath,"""
                boot-info-to-console = true
                """
            );

            // 3. Set up isolated executor
            IsolatedExecutor exec = IsolatedExecutor.forTask(CascaraConfigTask.class, Map.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setTimeout(Duration.ofSeconds(120))
                        .setModuleName(ReporterConfigTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        .setEnv("CASCARA_SPL_PROPERTIES", splPropertiesPath)
                );

            // 4. Run the test task
            Response<Map<String,String>> response = exec.run(null);
            debug(response);
            Map<String,String> settings = response.getPayload();

            // The SPL properties file path should be picked up from CASCARA_SPL_PROPERTIES
            assertEquals(splPropertiesPath.toString(), settings.get("cascara.spl.properties"));

            // The SPL properties file specifies this:
            assertEquals("true", settings.get("cascara.spl.boot-info-to-console"));
        }
    }
}
