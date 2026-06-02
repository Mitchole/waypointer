package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.util.Listeners;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

/**
 * Captures a route by playing through it: {@link #markCurrentLocation()} appends a waypoint step
 * at the player's tile; {@link #addManualStep(String)} appends a text step. The draft is an
 * ordinary {@link Route} created in the {@link RouteStore}, so the editor edits the same object.
 */
@Singleton
public class RouteRecorder
{
    private final RouteStore store;
    private final Supplier<WorldPoint> playerLocation;
    // Runs the tile read on the client thread in production; runs inline in tests.
    private final Consumer<Runnable> clientThreadRunner;
    private final Listeners listeners = new Listeners();

    @Nullable private UUID draftRouteId;
    private int waypointCounter;

    /** Production constructor: reads the player tile on the client thread. */
    @Inject
    public RouteRecorder(RouteStore store, Client client, ClientThread clientThread)
    {
        this(store, () -> {
            Player p = client.getLocalPlayer();
            return p == null ? null : p.getWorldLocation();
        }, clientThread::invoke);
    }

    /** Test constructor: tile supplied directly, captured inline (no client thread). */
    RouteRecorder(RouteStore store, Supplier<WorldPoint> playerLocation)
    {
        this(store, playerLocation, Runnable::run);
    }

    private RouteRecorder(RouteStore store, Supplier<WorldPoint> playerLocation,
        Consumer<Runnable> clientThreadRunner)
    {
        this.store = store;
        this.playerLocation = playerLocation;
        this.clientThreadRunner = clientThreadRunner;
    }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public boolean isRecording() { return draftRouteId != null; }

    @Nullable public UUID getDraftRouteId() { return draftRouteId; }

    public void start(String name)
    {
        if (draftRouteId != null) return;
        Route r = store.createRoute(name == null || name.trim().isEmpty() ? "Recorded route" : name.trim());
        draftRouteId = r.getId();
        waypointCounter = 0;
        listeners.fire();
    }

    public void markCurrentLocation()
    {
        final UUID draft = draftRouteId;
        if (draft == null) return;
        // The tile read touches Client, which asserts the client thread; run it there.
        clientThreadRunner.accept(() -> {
            WorldPoint wp = playerLocation.get();
            if (wp == null) return;
            int packed = WorldPointPacker.pack(wp);
            store.addWaypointStep(draft, packed, "Waypoint " + (++waypointCounter), null);
            listeners.fire();
        });
    }

    public void addManualStep(String text)
    {
        if (draftRouteId == null || text == null || text.trim().isEmpty()) return;
        store.addManualStep(draftRouteId, text.trim());
        listeners.fire();
    }

    /**
     * Append the player's current tile as a waypoint step to an arbitrary route. Used by the
     * editor's "mark current location" action (independent of recording). Reads Client on the
     * client thread; the store mutation drives the UI refresh, so no recorder listener fire here.
     */
    public void addCurrentLocationTo(UUID routeId)
    {
        if (routeId == null) return;
        clientThreadRunner.accept(() -> {
            WorldPoint wp = playerLocation.get();
            if (wp == null) return;
            store.addWaypointStep(routeId, WorldPointPacker.pack(wp), "Waypoint", null);
        });
    }

    public void stopAndSave()
    {
        draftRouteId = null;
        listeners.fire();
    }

    UUID getDraftRouteIdForTest() { return draftRouteId; }
}
