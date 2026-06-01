package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.util.Listeners;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

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
    private final Listeners listeners = new Listeners();

    @Nullable private java.util.UUID draftRouteId;
    private int waypointCounter;

    /** Production constructor: reads the player tile from the client. */
    @Inject
    public RouteRecorder(RouteStore store, Client client)
    {
        this(store, () -> {
            Player p = client.getLocalPlayer();
            return p == null ? null : p.getWorldLocation();
        });
    }

    /** Test constructor: tile supplied directly. */
    RouteRecorder(RouteStore store, Supplier<WorldPoint> playerLocation)
    {
        this.store = store;
        this.playerLocation = playerLocation;
    }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public boolean isRecording() { return draftRouteId != null; }

    @Nullable public java.util.UUID getDraftRouteId() { return draftRouteId; }

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
        if (draftRouteId == null) return;
        WorldPoint wp = playerLocation.get();
        if (wp == null) return;
        int packed = WorldPointPacker.pack(wp);
        store.addWaypointStep(draftRouteId, packed, "Waypoint " + (++waypointCounter), null);
        listeners.fire();
    }

    public void addManualStep(String text)
    {
        if (draftRouteId == null || text == null || text.trim().isEmpty()) return;
        store.addManualStep(draftRouteId, text.trim());
        listeners.fire();
    }

    public void stopAndSave()
    {
        draftRouteId = null;
        listeners.fire();
    }

    java.util.UUID getDraftRouteIdForTest() { return draftRouteId; }
}
