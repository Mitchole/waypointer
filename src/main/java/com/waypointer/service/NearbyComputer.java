package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.Waypoint;
import com.waypointer.util.Listeners;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
        // Empty stub - real compute lands in Task 4. For now treat result as always empty,
        // so the test in Task 3 can assert getCurrent() == [].
        List<Waypoint> next = Collections.emptyList();
        List<UUID> nextIds = Collections.emptyList();
        if (nextIds.equals(lastIds)) return;
        lastIds = nextIds;
        lastResult = next;
        listeners.fire();
    }
}
