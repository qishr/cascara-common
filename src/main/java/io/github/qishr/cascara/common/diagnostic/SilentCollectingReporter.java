package io.github.qishr.cascara.common.diagnostic;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class SilentCollectingReporter extends AbstractReporter<SilentCollectingReporter>  {

    private boolean hasErrors;

    public SilentCollectingReporter() {
        // Nothing to see here
    }

    public boolean hasErrors() { return hasErrors; }

    @Override
    protected SilentCollectingReporter self() { return this; }

    @Override
    public boolean collectsProblems() {
        // Returning true here means the parser won't throw exceptions
        return true;
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    //
    //
    //

    @Override
    protected void report(Diagnostic diagnostic) {
        if (this.level.compareTo(diagnostic.getLevel()) >= 0) {
            if (getDiagnosticCollector() != null) {
                getDiagnosticCollector().accept(diagnostic);
            }
        }

        if (getProblemCollector() != null && isProblem(level)) {
            getProblemCollector().accept(diagnostic);
        }

        if (diagnostic.getLevel() == Level.ERROR) {
            hasErrors = true;
        }
    }

    @Override
    protected void writeString(Diagnostic diagnostic) {
    }

}
