package integration.test.fixtures.task;

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.service.SPLConfig;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.Config.Setting;

public class CascaraConfigTask extends AbstractExecutionTask<Void, Map<String,String>> {
    // private static final Reporter REPORTER = Reporter.forClass(CascaraConfigTask.class);

    @Override
    public Map<String,String> run(Void ignore) throws Exception {
        Cascara cascara = Cascara.get();

        Map<String,String> settings = new HashMap<>();

        settings.put("cascara.properties", cascara.getPropertiesPath().toString());
        settings.put("cascara.spl.properties", cascara.getSPLPropertiesPath().toString());

        settings.put("cascara.content-types.path", cascara.getContentTypesPath().toString());

        settings.put("cascara.schema.path", cascara.getSchemasPath().toString());
        settings.put("cascara.shared.path", cascara.getSharedPath().toString());
        settings.put("cascara.module.path", cascara.getModulePath().toString());

        for (Setting s : SPLConfig.get().getAllSettings()) {
            settings.put(s.getName(), s.asString());
        }

        return settings;
    }
}
