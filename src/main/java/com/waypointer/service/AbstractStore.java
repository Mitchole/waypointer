package com.waypointer.service;

import com.waypointer.util.Listeners;

/**
 * Storage skeleton shared by {@link WaypointStore} and {@link RouteStore}: the live in-memory
 * library reference, listener pub/sub, and write-through persistence wiring. Subclasses define
 * their own notification semantics ({@link #notifyChanged()}) and may hook post-bootstrap setup
 * ({@link #afterBootstrap()}). Domain-specific concerns (undo, batching, view caches) stay in the
 * subclasses.
 *
 * @param <L> the library type held by the store
 */
abstract class AbstractStore<L>
{
    /**
     * Live in-memory library reference. {@code volatile} for safe publication of the reference to
     * reader threads (e.g. the render thread's NPC-name snapshot reads in WaypointStore);
     * multi-step mutations remain confined to the mutating thread.
     */
    protected volatile L library;

    private final Listeners listeners = new Listeners();
    private Listeners.Subscription saveSub;

    protected AbstractStore(L initial)
    {
        this.library = initial;
    }

    // Live in-memory library. NOT a defensive copy; callers that mutate it bypass listeners and
    // risk corrupting state. Read-only outside of test fixtures.
    public L getLibrary() { return library; }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    /** Test seam: number of currently-subscribed listeners. */
    public int listenerCountForTest() { return listeners.size(); }

    /**
     * Swap in a freshly-loaded library, run any subclass post-load setup, then notify. Notifying
     * lets a listener attached before bootstrap (e.g. a panel built by Guice before the plugin's
     * startUp() calls bootstrap) re-render against the new data.
     */
    public final void bootstrap(L lib)
    {
        this.library = lib;
        afterBootstrap();
        notifyChanged();
    }

    /** Hook for subclass setup after a bootstrap library swap, before listeners fire. Default no-op. */
    protected void afterBootstrap() {}

    /** Subclass notification semantics. Called on every mutation and after bootstrap. */
    protected abstract void notifyChanged();

    /** Raw listener fire. The primitive the subclass {@code notifyChanged} logic builds on. */
    protected void fire() { listeners.fire(); }

    /**
     * Subscribe a write-through saver fired synchronously on every mutation. Idempotent. The saver
     * should read the live library (e.g. {@code () -> persistence.save(getLibrary())}); a later
     * {@link #bootstrap(Object)} that swaps the library is picked up automatically.
     */
    public void enablePersistence(Runnable saver)
    {
        if (saveSub != null) return;
        saveSub = listeners.subscribe(saver);
    }

    /**
     * Detach the saver. Idempotent. Called from {@link com.waypointer.WaypointerPlugin#shutDown()}
     * so plugin reload cycles do not stack savers.
     */
    public void disablePersistence()
    {
        if (saveSub != null)
        {
            saveSub.close();
            saveSub = null;
        }
    }
}
