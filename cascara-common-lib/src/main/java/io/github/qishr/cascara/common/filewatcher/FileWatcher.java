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


package io.github.qishr.cascara.common.filewatcher;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.attribute.BasicFileAttributes;

public class FileWatcher {

    private record FileWatchSpec(Path targetFileName, Runnable callback) {}

    private static Set<FileWatcher> allWatchers = new HashSet<>();

    private Map<FileSystem, WatchService> watchServices = new ConcurrentHashMap<>();

    /// Maps a WatchKey (from a registered path) to the user's Runnable callback
    private final Map<WatchKey, Set<Runnable>> keysToCallbacks = new ConcurrentHashMap<>();
    private final Map<WatchKey, Set<FileWatchSpec>> keysToSpecs = new ConcurrentHashMap<>();
    private final Map<WatchKey, Set<FileChangeHandler>> keysToHandlers = new ConcurrentHashMap<>();

    /// Executor to run the WatchService polling loop in the background
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SPL-FileWatcher");
        thread.setDaemon(true); // Allow JVM to exit cleanly
        return thread;
    });

    /// Flag to control the watcher thread's state
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    /// WatchEvent Kinds used for file monitoring
    private static final WatchEvent.Kind<?>[] WATCH_KINDS = new WatchEvent.Kind<?>[] {
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    };

    public FileWatcher() {
        executor.submit(this::watchLoop);
        allWatchers.add(this);
    }

    /// Simple delegate, as true file-specific watching is complicated.
    public void watchFile(Path path, Runnable onEvent) throws IOException {
        Path directory = path.getParent();
        if (directory == null) {
             throw new IllegalArgumentException("Cannot watch root file without a parent directory.");
        }

        WatchService watchService = getWatchServiceFor(path.getFileSystem());

        // 1. Register the directory key
        WatchKey key = directory.register(watchService, WATCH_KINDS);

        // 2. Create the specification
        Path fileName = path.getFileName();
        FileWatchSpec spec = new FileWatchSpec(fileName, onEvent);

        // 3. Store the spec against the directory key
        keysToSpecs.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(spec);
    }

    /// Watch a specific directory and run onEvent when any event occurs.
    public void watchDirectory(Path path, Runnable onEvent) throws IOException {
        WatchService watchService = getWatchServiceFor(path.getFileSystem());
        WatchKey key = path.register(watchService, WATCH_KINDS);
        keysToCallbacks.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(onEvent);
    }

    /// Watch a specific directory and receive granular event details via a callback.
    public void watchDirectory(Path path, FileChangeHandler handler) throws IOException {
        WatchService watchService = getWatchServiceFor(path.getFileSystem());
        WatchKey key = path.register(watchService, WATCH_KINDS);
        keysToHandlers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    public void watchDirectory(Path path, FileChangeHandler handler, String... extensions) throws IOException {
        watchDirectory(path, new FilteredFileHandler(handler, extensions));
    }

    /// Watch a specific directory and all subdirectories, and run onEvent when any event occurs.
    /// This registers the current tree and dynamically registers new subdirectories.
    public void watchDirectoryRecursively(Path path, Runnable onEvent) throws IOException {
        // Use the recursive helper method
        registerTree(path, onEvent);
    }

    /// Start watching for file changes.
    public void resumeWatching() {
        paused.set(false);
    }

    /// Stop watching for file changes until `resumeWatching()` is called.
    public void pauseWatching() {
        paused.set(true);
    }

    /// Stop watching all currently watched files and directories.
    public void clear() {
        running.set(false);

        // Cancel all keys to stop OS events immediately
        keysToCallbacks.keySet().forEach(WatchKey::cancel);

        keysToCallbacks.clear();
        keysToHandlers.clear();
        keysToSpecs.clear();

        try {
            for (WatchService watchService : watchServices.values()) {
                watchService.close();
            }
        } catch (IOException e) {
            // Log or handle closure error
        }

        executor.shutdownNow();
    }

    /// Clears all file watchers globally
    public static void clearAll() {
        for (FileWatcher watcher : allWatchers) {
            watcher.clear();
        }
    }

    private void watchLoop() {
        while (running.get()) {
            if (paused.get()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            for (WatchService watchService : watchServices.values()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException | ClosedWatchServiceException e) {
                    return;
                }

                Path dirPath = (Path) key.watchable();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    Path fileName = (Path) event.context();
                    Path fullPath = dirPath.resolve(fileName);
                    FileChangeType type = mapKind(kind);

                    // 1. Notify all Granular Handlers (Set-based)
                    Set<FileChangeHandler> handlers = keysToHandlers.get(key);
                    if (handlers != null) {
                        handlers.forEach(handler -> handler.handle(type, fullPath));
                    }

                    // 2. Notify all Legacy/Simple Callbacks (Set-based)
                    Set<Runnable> callbacks = keysToCallbacks.get(key);
                    if (callbacks != null) {
                        callbacks.forEach(Runnable::run);
                    }

                    // 3. Notify Specific File Specs
                    Set<FileWatchSpec> specs = keysToSpecs.get(key);
                    if (specs != null) {
                        specs.stream()
                            .filter(spec -> spec.targetFileName().equals(fileName))
                            .forEach(spec -> spec.callback().run());
                    }
                }

                if (!key.reset()) {
                    keysToSpecs.remove(key);
                    keysToHandlers.remove(key);
                    keysToCallbacks.remove(key);
                }
            }
        }
    }

    private FileChangeType mapKind(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) return FileChangeType.CREATED;
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) return FileChangeType.DELETED;
        return FileChangeType.MODIFIED;
    }

    /// Recursively registers a directory and all subdirectories with the WatchService.
    private void registerTree(Path startDir, Runnable onEvent) throws IOException {
        WatchService watchService = getWatchServiceFor(startDir.getFileSystem());
        Files.walkFileTree(startDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watchService, WATCH_KINDS);
                // Multi-subscriber support for recursive watches
                keysToCallbacks.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(onEvent);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private WatchService getWatchServiceFor(FileSystem fs) {
        return watchServices.computeIfAbsent(fs, key -> {
            try {
                return fs.newWatchService();
            } catch (IOException e) {
                // TODO: Exception handling
                return null;
            }
        });
    }
}