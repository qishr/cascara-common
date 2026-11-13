package integration.test.fixtures.task;

import java.util.List;

import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public class ThrowableTestTask extends AbstractExecutionTask<String, String> {
    GlobalReporter reporter = GlobalReporter.forClass(ThrowableTestTask.class);

    @Override
    public String run(String throwableName) throws Throwable {
        Class<?> throwableClass = Class.forName(throwableName);
        List<Pair<Class<?>,Object>> params = List.of(
            new Pair<>(String.class, "test-exception")
        );
        throw (Throwable) ReflectionUtils.createInstance(throwableClass, params);
    }
}
