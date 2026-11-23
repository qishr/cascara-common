// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package io.github.qishr.cascara.common.diagnostic.log;

import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatConfig;

public class TimeSequencedAggregatorLogger implements Logger, AutoCloseable {

    private final Logger delegate;
    private final PriorityBlockingQueue<Diagnostic> buffer;
    private final ScheduledExecutorService scheduler;
    private final long windowMillis;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TimeSequencedAggregatorLogger(Logger delegate, long windowMillis) {
        this.delegate = delegate;
        this.windowMillis = windowMillis;
        this.buffer = new PriorityBlockingQueue<>(
            100,
            Comparator.comparing(t -> t.getTimestamp())
        );

        // Use daemon thread so the executor never prevents normal JVM exit
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cascara-time-sequenced-logger");
            t.setDaemon(true);
            return t;
        });

        // Periodically drain events older than windowMillis
        this.scheduler.scheduleAtFixedRate(this::flushBuffered, windowMillis, windowMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public TimeSequencedAggregatorLogger log(Diagnostic diagnostic) {
        if (!closed.get()) {
            buffer.add(diagnostic);
        } else {
            // Fallback: If logger is already closed, log directly to delegate
            delegate.log(diagnostic);
        }
        return this;
    }

    private synchronized void flushBuffered() {
        java.time.Instant cutoff = java.time.Instant.now().minusMillis(windowMillis);
        while (!buffer.isEmpty()) {
            Diagnostic head = buffer.peek();
            if (head != null && head.getTimestamp().isBefore(cutoff)) {
                delegate.log(buffer.poll());
            } else {
                break;
            }
        }
    }

    @Override
    public synchronized TimeSequencedAggregatorLogger flush() {
        while (!buffer.isEmpty()) {
            Diagnostic diag = buffer.poll();
            if (diag != null) {
                delegate.log(diag);
            }
        }
        delegate.flush();
        return this;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // 1. Stop accepting new periodic executions
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // 2. Perform final drain of remaining buffered diagnostics
            flush();
        }
    }

    public TimeSequencedAggregatorLogger setFormatConfig(LogFormatConfig config) {
        return this;
    }
}

// package io.github.qishr.cascara.common.diagnostic.log;

// import java.util.Comparator;
// import java.util.concurrent.*;

// import io.github.qishr.cascara.common.diagnostic.Diagnostic;
// import io.github.qishr.cascara.common.diagnostic.format.FormatConfig;

// public class TimeSequencedAggregatorLogger implements Logger {

//     private final Logger delegate;
//     private final PriorityBlockingQueue<Diagnostic> buffer;
//     private final ScheduledExecutorService scheduler;
//     private final long windowMillis;

//     public TimeSequencedAggregatorLogger(Logger delegate, long windowMillis) {
//         this.delegate = delegate;
//         this.windowMillis = windowMillis;
//         this.buffer = new PriorityBlockingQueue<>(
//             100,
//             Comparator.comparing(t -> t.getTimestamp())
//         );
//         this.scheduler = Executors.newSingleThreadScheduledExecutor();

//         // Periodically drain events older than windowMillis
//         this.scheduler.scheduleAtFixedRate(this::flushBuffered, windowMillis, windowMillis, TimeUnit.MILLISECONDS);
//     }

//     @Override
//     public TimeSequencedAggregatorLogger log(Diagnostic diagnostic) {
//         buffer.add(diagnostic);
//         return this;
//     }

//     private synchronized void flushBuffered() {
//         java.time.Instant cutoff = java.time.Instant.now().minusMillis(windowMillis);
//         while (!buffer.isEmpty() && buffer.peek().getTimestamp().isBefore(cutoff)) {
//             delegate.log(buffer.poll());
//         }
//     }

//     @Override
//     public TimeSequencedAggregatorLogger flush() {
//         while (!buffer.isEmpty()) {
//             delegate.log(buffer.poll());
//         }
//         delegate.flush();
//         return this;
//     }

//     public TimeSequencedAggregatorLogger setFormatConfig(FormatConfig config) {
//         return this;
//     }

// }