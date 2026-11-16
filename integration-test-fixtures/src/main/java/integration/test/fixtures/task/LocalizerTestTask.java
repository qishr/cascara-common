package integration.test.fixtures.task;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.i18n.DetailedMessage;
import io.github.qishr.cascara.common.i18n.Localizer;

public class LocalizerTestTask extends AbstractExecutionTask<DetailedMessage, String> {
    Reporter reporter = Reporter.forClass(LocalizerTestTask.class);

    @Override
    public String run(DetailedMessage dm) throws Throwable {

        Localizer localizer = Localizer.instance();

        System.out.println("System locale: " + localizer.getSystemLocale());
        System.out.println("Active locale: " + localizer.getActiveLocale());

        String string = localizer.format(dm);
        System.out.println("String: " + string);

        return string;
    }
}
