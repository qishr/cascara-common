package io.github.qishr.cascara.common.diagnostic;

public class Diagnostic {
    public enum Level {
        ERROR(1),
        WARNING(2),
        INFO(3),
        DEBUG(4),
        TRACE(5);

        private int level;

        Level(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
}
