package com.waypointer.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

// Reusable list of Runnable listeners with safe iteration. A listener throwing doesn't stop
// later listeners. Backed by CopyOnWriteArrayList so iteration is snapshot-stable even when
// listeners add/remove themselves during fire(). Use add()/remove() with a stable lambda you
// can pass back; for anonymous lambdas use subscribe() and hold the returned Subscription.
@Slf4j
public final class Listeners
{
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public void add(Runnable r) { listeners.add(r); }
    public void remove(Runnable r) { listeners.remove(r); }

    // Registers r and returns a token whose close() removes it. Use with anonymous lambdas
    // so the caller doesn't need to keep a reference to the original.
    public Subscription subscribe(Runnable r)
    {
        listeners.add(r);
        return () -> listeners.remove(r);
    }

    public void fire()
    {
        for (Runnable r : listeners)
        {
            try { r.run(); }
            catch (Exception e) { log.warn("Listener threw", e); }
        }
    }

    @FunctionalInterface
    public interface Subscription extends AutoCloseable
    {
        @Override void close();
    }
}
