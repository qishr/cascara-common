package io.github.qishr.cascara.common.diagnostic.format;

import io.github.qishr.cascara.common.annotation.Experimental;

@Experimental
public class FormatConfig {
    private boolean prefixEveryLine = true;
    private boolean includeStackTrace = false;
    private boolean includeProblemCodes = false;
    private boolean incluedTime = true;
    private boolean incluedPid = true;
    private boolean incluedThread = true;
    private boolean incluedSource = true;

    private boolean useSimpleClassName = false;

    public FormatConfig() {}

    public boolean prefixEveryLine() {
        return prefixEveryLine;
    }

    public FormatConfig setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return this;
    }

    public boolean includeStackTrace() {
        return includeStackTrace;
    }

    public FormatConfig setIncludeStackTrace(boolean b) {
        includeStackTrace = b;
        return this;
    }

    public boolean includeProblemCodes() {
        return includeProblemCodes;
    }

    public FormatConfig setIncludeProblemCodes(boolean b) {
        includeProblemCodes = b;
        return this;
    }

    public boolean incluedTime() {
        return incluedTime;
    }

    public FormatConfig setIncludeTime(boolean b) {
        incluedTime = b;
        return this;
    }

    public boolean incluedPid() {
        return incluedPid;
    }

    public FormatConfig setIncluedPid(boolean b) {
        incluedPid = b;
        return this;
    }

    public boolean incluedThread() {
        return incluedThread;
    }

    public FormatConfig setIncluedThread(boolean b) {
        incluedThread = b;
        return this;
    }

    public boolean incluedSource() {
        return incluedSource;
    }

    public FormatConfig setIncluedSource(boolean b) {
        incluedSource = b;
        return this;
    }

    public boolean useSimpleClassName() {
        return useSimpleClassName;
    }

    public FormatConfig setUseSimpleClassName(boolean b) {
        useSimpleClassName = b;
        return this;
    }
}
