package io.github.qishr.cascara.common.diagnostic.report;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class ClassReporter extends AbstractReporter<ClassReporter> {

    ClassReporter() {}

    ClassReporter(String source) {
        this.source = source;
        this.level = GlobalReporter.globalInstance().level;
    }

    /// {@inheritDoc}
    @Override
    protected ClassReporter self() { return this; }

    /// {@inheritDoc}
    @Override
    public ClassReporter setLevel(Level level) {
        if (!GlobalReporter.globalInstance().allowApiOverride) {
            throw new UnsupportedOperationException("This reporter does not allow setting its level via the API");
        }
        return setLevelInternal(level);
    }

    /// Central reporting method. All other reporting methods call this.
    @Override
    public void report(Diagnostic diagnostic) {
        if (this.level.includes(diagnostic.getLevel())) {
            GlobalReporter.globalInstance().report(diagnostic);
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean collectsProblems() {
        return GlobalReporter.globalInstance().problemConsumer != null;
    }

    ClassReporter setLevelInternal(Level level) {
        this.level = level;
        return this;
    }
}
