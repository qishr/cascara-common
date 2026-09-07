package io.github.qishr.cascara.common.lang.util;

public class SourceBufferOptions extends LanguageOptions<SourceBufferOptions> {
    private boolean supportsSimd;
    private boolean strictAsciiMode;
    private boolean trackPosition;

    public boolean supportsSimd() { return supportsSimd; }

    public SourceBufferOptions setSupportsSimd(boolean b) {
        supportsSimd = b;
        return this;
    }

    public boolean strictAsciiMode() {
        return strictAsciiMode;
    }

    public SourceBufferOptions setStrictAsciiMode(boolean b) {
        strictAsciiMode = b;
        return this;
    }

    public boolean trackPosition() {
        return trackPosition;
    }

    public SourceBufferOptions setTrackPosition(boolean b) {
        trackPosition = b;
        return this;
    }
}
