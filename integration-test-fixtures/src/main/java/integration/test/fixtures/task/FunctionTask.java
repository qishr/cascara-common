package integration.test.fixtures.task;

import integration.test.fixtures.payload.FunctionInput;
import io.github.qishr.cascara.common.exec.AbstractExecutionTask;

public class FunctionTask extends AbstractExecutionTask<FunctionInput, String> {
    @Override
    public String run(FunctionInput input) throws Exception {
        System.out.println("f = " + input.f);
        return "";
    }
}
