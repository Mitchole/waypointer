package com.waypointer.service;

import com.waypointer.util.Listeners;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Owns the {@link DebouncedSaver} + listener {@link Listeners.Subscription} lifecycle shared by
 * {@link WaypointStore} and {@link RouteStore}: {@link #enable} subscribes the saver to mutation
 * events, {@link #disable} detaches and cancels any pending write, {@link #flush} forces a
 * synchronous write. {@code enable} is idempotent.
 */
final class PersistenceBinding<T>
{
    private final Listeners listeners;
    private DebouncedSaver<T> saver;
    private Listeners.Subscription saveSub;

    PersistenceBinding(Listeners listeners)
    {
        this.listeners = listeners;
    }

    void enable(JsonSnapshotSink<T> sink, ScheduledExecutorService exec, Duration debounce,
        Supplier<T> valueSupplier)
    {
        if (saveSub != null) return;
        this.saver = new DebouncedSaver<>(sink, exec, debounce, valueSupplier);
        this.saveSub = listeners.subscribe(saver::schedule);
    }

    void disable()
    {
        if (saveSub != null)
        {
            saveSub.close();
            saveSub = null;
        }
        if (saver != null) saver.cancelPending();
    }

    void flush()
    {
        if (saver != null) saver.flush();
    }
}
