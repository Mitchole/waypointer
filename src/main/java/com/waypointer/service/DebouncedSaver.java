package com.waypointer.service;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * Write-behind buffer. A mutation calls {@link #schedule()}, which snapshots the value to JSON on
 * the calling thread and defers the disk write by a fixed window; a burst collapses into a single
 * write. {@link #flush()} forces an immediate synchronous write. Generic over a
 * {@link JsonSnapshotSink} so the same buffer serves the waypoint library and the route library.
 */
@Slf4j
class DebouncedSaver<T>
{
    private final JsonSnapshotSink<T> sink;
    private final ScheduledExecutorService scheduler;
    private final Duration debounce;
    private final Supplier<T> valueSupplier;
    private volatile ScheduledFuture<?> pendingSave;

    DebouncedSaver(
        JsonSnapshotSink<T> sink,
        ScheduledExecutorService scheduler,
        Duration debounce,
        Supplier<T> valueSupplier)
    {
        this.sink = sink;
        this.scheduler = scheduler;
        this.debounce = debounce;
        this.valueSupplier = valueSupplier;
    }

    void schedule()
    {
        cancelPending();
        final String json = sink.serialize(valueSupplier.get());
        pendingSave = scheduler.schedule(
            () -> {
                boolean ok = sink.writeBlocking(json);
                if (!ok) log.warn("Snapshot save failed");
            },
            debounce.toMillis(),
            TimeUnit.MILLISECONDS);
    }

    void flush()
    {
        cancelPending();
        boolean ok = sink.saveBlocking(valueSupplier.get());
        if (!ok) log.warn("Snapshot save failed");
    }

    void cancelPending()
    {
        ScheduledFuture<?> p = pendingSave;
        if (p != null && !p.isDone()) p.cancel(false);
    }
}
