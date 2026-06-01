package com.waypointer.service;

import com.waypointer.codec.SnapshotCodec;
import com.waypointer.util.Listeners;
import com.waypointer.util.Listeners.Subscription;
import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

// Generic in-memory working set of dev-mode overrides backed by a single JSON file, with
// debounced persistence and pub/sub on mutation. Subclasses hold the domain mutators and
// supply the snapshot type, its codec, a deep-copy for the undo buffer, and an optional
// post-decode normalisation hook.
abstract class OverridesStore<S>
{
    protected S snapshot;
    protected S undoBuffer = null;
    private final Listeners listeners = new Listeners();
    private final OverridePersistence persistence;
    private final SnapshotCodec<S> codec;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    OverridesStore(Path dir, String fileName, SnapshotCodec<S> codec,
        ScheduledExecutorService scheduler, S empty)
    {
        this.persistence = new OverridePersistence(dir, fileName);
        this.codec = codec;
        this.scheduler = scheduler;
        this.snapshot = empty;
    }

    public void loadFromDisk()
    {
        snapshot = codec.decode(persistence.loadOrEmpty());
        afterDecode(snapshot);
        listeners.fire();
    }

    public boolean flushBlocking()
    {
        return persistence.writeBlocking(codec.encode(snapshot));
    }

    public S getSnapshot() { return snapshot; }

    public Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public boolean undoLast()
    {
        if (undoBuffer == null) return false;
        snapshot = undoBuffer;
        undoBuffer = null;
        listeners.fire();
        scheduleSave();
        return true;
    }

    // Stops the debounce scheduler. The store singletons live for the JVM, but this gives tests
    // and any future non-singleton use a clean shutdown of the daemon thread.
    public void close()
    {
        scheduler.shutdownNow();
    }

    // Snapshot the current state into the undo buffer. Subclass mutators call this before
    // mutating, then call fire() + scheduleSave() once the change is applied.
    protected void beginMutation()
    {
        undoBuffer = deepCopy(snapshot);
    }

    protected void fire()
    {
        listeners.fire();
    }

    protected void scheduleSave()
    {
        // compareAndSet coalesces a burst into a single scheduled flush without a check-then-act
        // race: the scheduler callback (which may run off the calling thread) clears the flag.
        if (!dirty.compareAndSet(false, true)) return;
        scheduler.schedule(() -> {
            dirty.set(false);
            flushBlocking();
        }, 500, TimeUnit.MILLISECONDS);
    }

    // Deep copy used for the undo buffer so a later mutation cannot alias prior state.
    protected abstract S deepCopy(S src);

    // Hook to normalise a freshly decoded snapshot (e.g. dedupe). Default no-op.
    protected void afterDecode(S decoded) {}
}
