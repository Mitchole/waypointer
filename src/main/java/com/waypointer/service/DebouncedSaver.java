package com.waypointer.service;

import com.waypointer.model.Library;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Write-behind buffer for the waypoint library. A mutation calls {@link #schedule()}, which
 * snapshots the library to JSON on the calling thread and defers the disk write by a fixed
 * window; a burst of mutations collapses into a single write. {@link #flush()} forces an
 * immediate synchronous write of the current state.
 *
 * <p>Collaborator of {@link WaypointStore}, which owns the listener subscription that drives
 * {@link #schedule()}. This class is not wired to the EventBus and holds no listener tokens.
 */
@Slf4j
class DebouncedSaver
{
    private final WaypointStorePersistence persistence;
    private final ScheduledExecutorService scheduler;
    private final Duration debounce;
    private final Supplier<Library> librarySupplier;
    private volatile ScheduledFuture<?> pendingSave;

    DebouncedSaver(
        WaypointStorePersistence persistence,
        ScheduledExecutorService scheduler,
        Duration debounce,
        Supplier<Library> librarySupplier)
    {
        this.persistence = persistence;
        this.scheduler = scheduler;
        this.debounce = debounce;
        this.librarySupplier = librarySupplier;
    }

    /**
     * Snapshots the library to JSON on the calling thread (mutations happen here, so iteration is
     * safe) and schedules the frozen bytes to be written after the debounce window, replacing any
     * write still pending. Serializing off the scheduler thread avoids racing gson iteration
     * against a parallel mutation.
     */
    void schedule()
    {
        cancelPending();
        final String json = persistence.serialize(librarySupplier.get());
        pendingSave = scheduler.schedule(
            () -> {
                boolean ok = persistence.writeBlocking(json);
                if (!ok) log.warn("Library save failed");
            },
            debounce.toMillis(),
            TimeUnit.MILLISECONDS);
    }

    /** Cancels any pending debounced write, then writes the current library synchronously. */
    void flush()
    {
        cancelPending();
        boolean ok = persistence.saveBlocking(librarySupplier.get());
        if (!ok) log.warn("Library save failed");
    }

    /** Cancels a pending debounced write if one is scheduled. Idempotent. */
    void cancelPending()
    {
        ScheduledFuture<?> p = pendingSave;
        if (p != null && !p.isDone()) p.cancel(false);
    }
}
