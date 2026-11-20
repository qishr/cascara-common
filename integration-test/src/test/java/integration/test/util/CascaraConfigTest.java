package integration.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.task.CascaraConfigTask;
import integration.test.fixtures.task.ReporterConfigTask;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.common.util.Cascara;

@Isolated
public class CascaraConfigTest extends ExecTestBase {
    @Test
    void test_defaultCascaraConfig(@TempDir Path tempDir) throws Throwable {
        // 1. Create a Cascara properties file
        String cascaraProperties = """
            """;
        Path cascaraPropertiesPath = Cascara.get().getCascaraPropertiesPath();
        Files.createDirectories(cascaraPropertiesPath.getParent());
        Files.writeString(cascaraPropertiesPath, cascaraProperties);
        syncVfs();

        // 2. Set up isolated executor
        IsolatedExecutor exec = IsolatedExecutor.forTask(CascaraConfigTask.class, Map.class)
            .setDiagnosticForwarding(true)
            .setJvmConfig(
                new JvmConfig()
                    .setTimeout(Duration.ofSeconds(120))
                    .setModuleName(ReporterConfigTask.class.getModule().getName())
                    .setModulePath(getModulePath())
                    .setEnv(EV_CASCARA_HOME, getPhysicalHomePath())
            );

        // 3. Run the test task
        Response<Map<String,String>> response = exec.run(null);
        debug(response);
        Map<String,String> settings = response.getPayload();

        // for (Entry<String,String> entry : settings.entrySet()) {
        //     System.out.println(entry.getKey() + " = " + entry.getValue());
        // }

        assertEquals("/.cascara/shared/cascara.properties", settings.get("cascara.properties"));
        assertEquals("/.cascara/cascara.spl.properties", settings.get("cascara.spl.properties"));
        assertEquals("/.cascara/shared", settings.get("cascara.shared.path"));
        assertEquals("/.cascara/modulepath", settings.get("cascara.module.path"));
        assertEquals("/.cascara/shared/content-types.yaml", settings.get("cascara.content-types.path"));
        assertEquals("/.cascara/shared/schemas", settings.get("cascara.schema.path"));
    }

    // @Disabled
    @Test
    void test_customSplPath(@TempDir Path tempDir) throws Throwable {
        // 1. Create a Cascara properties file
        String cascaraProperties = """
            """;
        Path cascaraPropertiesPath = Cascara.get().getCascaraPropertiesPath();
        Files.createDirectories(cascaraPropertiesPath.getParent());
        Files.writeString(cascaraPropertiesPath, cascaraProperties);

        // 2. Create an SPL properties file
        Path splPropertiesPath = cascaraPropertiesPath.getParent().resolve("custom-spl.properties");
        Files.writeString(splPropertiesPath,"""
            boot-info-to-console = true
            """
        );

        syncVfs();

        // 3. Set up isolated executor
        IsolatedExecutor exec = IsolatedExecutor.forTask(CascaraConfigTask.class, Map.class)
            .setDiagnosticForwarding(true)
            .setJvmConfig(
                new JvmConfig()
                    .setTimeout(Duration.ofSeconds(120))
                    .setModuleName(ReporterConfigTask.class.getModule().getName())
                    .setModulePath(getModulePath())
                    .setEnv(EV_CASCARA_HOME, getPhysicalHomePath())
                    .setEnv("CASCARA_SPL_PROPERTIES", splPropertiesPath)
            );

        // 4. Run the test task
        Response<Map<String,String>> response = exec.run(null);
        debug(response);
        Map<String,String> settings = response.getPayload();

        // for (Entry<String,String> entry : settings.entrySet()) {
        //     System.out.println(entry.getKey() + " = " + entry.getValue());
        // }

        // The SPL properties file path should be picked up from CASCARA_SPL_PROPERTIES
        assertEquals("/.cascara/shared/custom-spl.properties", settings.get("cascara.spl.properties"));

        // The SPL properties file specifies this:
        assertEquals("true", settings.get("cascara.spl.boot-info-to-console"));
    }
}
