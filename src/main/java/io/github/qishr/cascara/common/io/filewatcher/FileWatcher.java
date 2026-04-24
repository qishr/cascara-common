package io.github.qishr.cascara.common.io.filewatcher;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.file.attribute.BasicFileAttributes;

public class FileWatcher {

    private record FileWatchSpec(Path targetFileName, Runnable callback) {}

    private final WatchService watcher;
    // Maps a WatchKey (from a registered path) to the user's Runnable callback
    private final Map<WatchKey, Set<Runnable>> keysToCallbacks = new ConcurrentHashMap<>();
    private final Map<WatchKey, Set<FileWatchSpec>> keysToSpecs = new ConcurrentHashMap<>();
    private final Map<WatchKey, Set<FileChangeHandler>> keysToHandlers = new ConcurrentHashMap<>();

    // Executor to run the WatchService polling loop in the background
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Flag to control the watcher thread's state
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    // WatchEvent Kinds used for file monitoring
    private static final WatchEvent.Kind<?>[] WATCH_KINDS = new WatchEvent.Kind<?>[] {
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    };

    public FileWatcher() throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        executor.submit(this::watchLoop);
    }

    // --- Public API Methods ---

    // Simple delegate, as true file-specific watching is complicated.
    public void watchFile(Path path, Runnable onEvent) throws IOException {
        Path directory = path.getParent();
        if (directory == null) {
             throw new IllegalArgumentException("Cannot watch root file without a parent directory.");
        }

        // 1. Register the directory key
        WatchKey key = directory.register(watcher, WATCH_KINDS);

        // 2. Create the specification
        Path fileName = path.getFileName();
        FileWatchSpec spec = new FileWatchSpec(fileName, onEvent);

        // 3. Store the spec against the directory key
        keysToSpecs.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(spec);
    }

    /// Watch a specific directory and run onEvent when any event occurs.
    public void watchDirectory(Path path, Runnable onEvent) throws IOException {
        WatchKey key = path.register(watcher, WATCH_KINDS);
        keysToCallbacks.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(onEvent);
    }

    /// Watch a specific directory and receive granular event details via a callback.
    public void watchDirectory(Path path, FileChangeHandler handler) throws IOException {
        WatchKey key = path.register(watcher, WATCH_KINDS);
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
            watcher.close();
        } catch (IOException e) {
            // Log or handle closure error
        }

        executor.shutdownNow();
    }

    //
    // Core Watch Loop
    //

    // private void watchLoop() {
    //     while (running.get()) {
    //         if (paused.get()) {
    //             try {
    //                 Thread.sleep(100);
    //             } catch (InterruptedException e) {
    //                 Thread.currentThread().interrupt();
    //             }
    //             continue;
    //         }

    //         WatchKey key;
    //         try {
    //             key = watcher.take();
    //         } catch (InterruptedException | ClosedWatchServiceException e) {
    //             return;
    //         }

    //         Path dirPath = (Path) key.watchable(); // The directory being watched

    //         for (WatchEvent<?> event : key.pollEvents()) {
    //             // if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

    //             // Path fileName = (Path) event.context();
    //             // Path fullPath = dirPath.resolve(fileName);
    //             // FileChangeType type = mapKind(event.kind());

    //             // // 1. Check for specific file specs (your existing logic)
    //             // Set<FileWatchSpec> specs = keysToSpecs.get(key);
    //             // if (specs != null) {
    //             //     specs.stream()
    //             //          .filter(s -> s.targetFileName().equals(fileName))
    //             //          .forEach(s -> s.callback().run());
    //             // }

    //             // // 2. Check for granular directory handlers (the new logic)
    //             // FileChangeHandler handler = keysToHandlers.get(key);
    //             // if (handler != null) {
    //             //     handler.handle(type, fullPath);
    //             // }

    //             // // 3. Keep existing Runnable callbacks for backward compatibility
    //             // Runnable simpleCallback = keysToCallbacks.get(key);
    //             // if (simpleCallback != null) simpleCallback.run();
    //             WatchEvent.Kind<?> kind = event.kind();
    //             if (kind == StandardWatchEventKinds.OVERFLOW) continue;

    //             Path fileName = (Path) event.context();
    //             Path fullPath = dirPath.resolve(fileName);

    //             // 1. Notify Granular Handlers (ModuleService, etc.)
    //             var granularHandler = keysToHandlers.get(key);
    //             if (granularHandler != null) {
    //                 granularHandler.handle(mapKind(kind), fullPath);
    //             }

    //             // 2. Notify Legacy/Simple Callbacks
    //             Runnable simpleCallback = keysToCallbacks.get(key);
    //             if (simpleCallback != null) {
    //                 simpleCallback.run();
    //             }

    //             // 3. Notify Specific File Specs (The "targetFileName" check)
    //             Set<FileWatchSpec> specs = keysToSpecs.get(key);
    //             if (specs != null) {
    //                 specs.stream()
    //                      .filter(spec -> spec.targetFileName().equals(fileName))
    //                      .forEach(spec -> spec.callback().run());
    //             }
    //         }

    //         if (!key.reset()) {
    //             keysToSpecs.remove(key);
    //             keysToHandlers.remove(key);
    //             keysToCallbacks.remove(key);
    //         }
    //     }
    // }

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

            WatchKey key;
            try {
                key = watcher.take();
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

    private FileChangeType mapKind(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) return FileChangeType.CREATED;
        if (kind == StandardWatchEventKinds.ENTRY_DELETE) return FileChangeType.DELETED;
        return FileChangeType.MODIFIED;
    }

    //
    // Directory Registration Helper
    //

    // Recursively registers a directory and all subdirectories with the WatchService.
    // Recursively registers a directory and all subdirectories with the WatchService.
    private void registerTree(Path startDir, Runnable onEvent) throws IOException {
        Files.walkFileTree(startDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watcher, WATCH_KINDS);
                // Multi-subscriber support for recursive watches
                keysToCallbacks.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(onEvent);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}