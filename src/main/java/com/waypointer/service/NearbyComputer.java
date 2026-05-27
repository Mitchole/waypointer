package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.util.Listeners;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

/**
 * Computes the N closest non-pinned waypoints to the player's current tile.
 * Throttled to fire its listener at most once every TICK_INTERVAL ticks (~3.6 s),
 * but also recomputes immediately when the underlying store or pathfinder state
 * changes (via store / pathfinder subscriptions wrapped in clientThread.invoke
 * because those callbacks may run on the EDT).
 */
@Slf4j
@Singleton
public class NearbyComputer
{
    static final int TICK_INTERVAL = 6;

    private final Client client;
    private final ClientThread clientThread;
    private final WaypointStore store;
    private final WaypointPathfinder pathfinder;
    private final WaypointerConfig config;
    private final Listeners listeners = new Listeners();

    private int tickCounter = 0;
    private List<Waypoint> lastResult = Collections.emptyList();
    private List<UUID> lastIds = Collections.emptyList();

    private Listeners.Subscription storeSub;
    private Listeners.Subscription pathSub;

    @Inject
    public NearbyComputer(Client client, ClientThread clientThread, WaypointStore store,
        WaypointPathfinder pathfinder, WaypointerConfig config)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.store = store;
        this.pathfinder = pathfinder;
        this.config = config;
        this.storeSub = store.subscribe(() -> clientThread.invoke(this::recomputeAndFireIfChanged));
        this.pathSub = pathfinder.subscribe(() -> clientThread.invoke(this::recomputeAndFireIfChanged));
    }

    public List<Waypoint> getCurrent() { return lastResult; }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    @Subscribe
    public void onGameTick(GameTick e)
    {
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;
        recomputeAndFireIfChanged();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"waypointer".equals(e.getGroup())) return;
        String k = e.getKey();
        if ("showNearbySection".equals(k) || "nearbyCount".equals(k)
            || "nearbySamePlaneOnly".equals(k))
        {
            recomputeAndFireIfChanged();
        }
    }

    public void dispose()
    {
        if (storeSub != null) { storeSub.close(); storeSub = null; }
        if (pathSub != null) { pathSub.close(); pathSub = null; }
    }

    void recomputeAndFireIfChanged()
    {
        List<Waypoint> next = compute();
        List<UUID> nextIds = idsOf(next);
        if (nextIds.equals(lastIds)) return;
        lastIds = nextIds;
        lastResult = next;
        listeners.fire();
    }

    /** Pure compute against the current state of client + store + config + pathfinder. */
    List<Waypoint> compute()
    {
        if (!config.showNearbySection()) return Collections.emptyList();
        if (client.getGameState() != GameState.LOGGED_IN) return Collections.emptyList();

        Player p = client.getLocalPlayer();
        if (p == null) return Collections.emptyList();
        WorldPoint loc = p.getWorldLocation();
        if (loc == null) return Collections.emptyList();

        int px = loc.getX();
        int py = loc.getY();
        int pPlane = loc.getPlane();
        int n = config.nearbyCount();
        boolean samePlaneOnly = config.nearbySamePlaneOnly();
        int activeTarget = pathfinder.getActiveTarget();

        List<Waypoint> waypoints = store.getLibrary().getWaypoints();
        List<Scored> scored = new ArrayList<>(waypoints.size());
        for (Waypoint w : waypoints)
        {
            if (w.isPinned()) continue;
            int packed = w.getPackedWorldPoint();
            if (packed == activeTarget) continue;
            int wPlane = WorldPointPacker.getPlane(packed);
            if (samePlaneOnly && wPlane != pPlane) continue;
            int wx = WorldPointPacker.getX(packed);
            int wy = WorldPointPacker.getY(packed);
            int dx = Math.abs(wx - px);
            int dy = Math.abs(wy - py);
            int dist = Math.max(dx, dy);
            scored.add(new Scored(w, dist));
        }
        if (scored.isEmpty()) return Collections.emptyList();

        scored.sort(Comparator.comparingInt(s -> s.dist));
        int take = Math.min(n, scored.size());
        List<Waypoint> out = new ArrayList<>(take);
        for (int i = 0; i < take; i++) out.add(scored.get(i).w);
        return Collections.unmodifiableList(out);
    }

    private static List<UUID> idsOf(List<Waypoint> ws)
    {
        if (ws.isEmpty()) return Collections.emptyList();
        List<UUID> out = new ArrayList<>(ws.size());
        for (Waypoint w : ws) out.add(w.getId());
        return Collections.unmodifiableList(out);
    }

    private static final class Scored
    {
        final Waypoint w;
        final int dist;
        Scored(Waypoint w, int dist) { this.w = w; this.dist = dist; }
    }
}
