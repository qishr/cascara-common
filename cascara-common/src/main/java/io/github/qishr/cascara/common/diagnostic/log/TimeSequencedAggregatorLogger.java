package io.github.qishr.cascara.common.diagnostic.log;

import java.util.Comparator;
import java.util.concurrent.*;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;

public class TimeSequencedAggregatorLogger implements Logger {

    private final Logger delegate;
    private final PriorityBlockingQueue<Diagnostic> buffer;
    private final ScheduledExecutorService scheduler;
    private final long windowMillis;

    public TimeSequencedAggregatorLogger(Logger delegate, long windowMillis) {
        this.delegate = delegate;
        this.windowMillis = windowMillis;
        this.buffer = new PriorityBlockingQueue<>(
            100,
            Comparator.comparing(Diagnostic::getTimestamp)
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // Periodically drain events older than windowMillis
        this.scheduler.scheduleAtFixedRate(this::flushBuffered, windowMillis, windowMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void log(Diagnostic diagnostic) {
        buffer.add(diagnostic);
    }

    private synchronized void flushBuffered() {
        java.time.Instant cutoff = java.time.Instant.now().minusMillis(windowMillis);
        while (!buffer.isEmpty() && buffer.peek().getTimestamp().isBefore(cutoff)) {
            delegate.log(buffer.poll());
        }
    }

    @Override
    public void flush() {
        while (!buffer.isEmpty()) {
            delegate.log(buffer.poll());
        }
        delegate.flush();
    }
}