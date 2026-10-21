package io.github.qishr.cascara.common.diagnostic.format;

import io.github.qishr.cascara.common.annotation.Experimental;

@Experimental
public class FormatOptions {
    private boolean prefixEveryLine = true;
    private boolean includeStackTrace = false;
    private boolean includeProblemCodes = false;
    private boolean incluedTime = true;
    private boolean incluedPid = true;
    private boolean incluedThread = true;
    private boolean incluedSource = true;

    public FormatOptions() {}

    public boolean prefixEveryLine() {
        return prefixEveryLine;
    }

    public FormatOptions setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return this;
    }

    public boolean includeStackTrace() {
        return includeStackTrace;
    }

    public FormatOptions setIncludeStackTrace(boolean b) {
        includeStackTrace = b;
        return this;
    }

    public boolean includeProblemCodes() {
        return includeProblemCodes;
    }

    public FormatOptions setIncludeProblemCodes(boolean b) {
        includeProblemCodes = b;
        return this;
    }

    public boolean incluedTime() {
        return incluedTime;
    }

    public FormatOptions setIncludeTime(boolean b) {
        incluedTime = b;
        return this;
    }

    public boolean incluedPid() {
        return incluedPid;
    }

    public FormatOptions setIncluedPid(boolean b) {
        incluedPid = b;
        return this;
    }

    public boolean incluedThread() {
        return incluedThread;
    }

    public FormatOptions setIncluedThread(boolean b) {
        incluedThread = b;
        return this;
    }

    public boolean incluedSource() {
        return incluedSource;
    }

    public FormatOptions setIncluedSource(boolean b) {
        incluedSource = b;
        return this;
    }
}
