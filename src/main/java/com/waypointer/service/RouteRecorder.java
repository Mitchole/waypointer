package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

/**
 * Appends the player's current tile to a route as a waypoint step, naming it with the same
 * smart default the waypoint capture flow uses ({@link WaypointCapture#defaultName(int)}).
 * The tile read touches {@link Client}, which asserts the client thread, so the read runs there.
 */
@Singleton
public class RouteRecorder
{
    private final RouteStore store;
    private final Supplier<WorldPoint> playerLocation;
    // Runs the tile read on the client thread in production; runs inline in tests.
    private final Consumer<Runnable> clientThreadRunner;
    // Maps a packed tile to a default step label (nearest landmark, else coords).
    private final IntFunction<String> nameForPacked;

    /** Production constructor: reads the player tile on the client thread, names via capture. */
    @Inject
    public RouteRecorder(RouteStore store, Client client, ClientThread clientThread,
        WaypointCapture capture)
    {
        this(store, () -> {
            Player p = client.getLocalPlayer();
            return p == null ? null : p.getWorldLocation();
        }, clientThread::invoke, capture::defaultName);
    }

    /** Test constructor: tile and naming supplied directly, captured inline (no client thread). */
    RouteRecorder(RouteStore store, Supplier<WorldPoint> playerLocation,
        IntFunction<String> nameForPacked)
    {
        this(store, playerLocation, Runnable::run, nameForPacked);
    }

    private RouteRecorder(RouteStore store, Supplier<WorldPoint> playerLocation,
        Consumer<Runnable> clientThreadRunner, IntFunction<String> nameForPacked)
    {
        this.store = store;
        this.playerLocation = playerLocation;
        this.clientThreadRunner = clientThreadRunner;
        this.nameForPacked = nameForPacked;
    }

    /**
     * Append the player's current tile as an auto-named waypoint step to {@code routeId}. The
     * store mutation drives the UI refresh, so no listener fire here.
     */
    public void addCurrentLocationTo(UUID routeId)
    {
        if (routeId == null) return;
        clientThreadRunner.accept(() -> {
            WorldPoint wp = playerLocation.get();
            if (wp == null) return;
            int packed = WorldPointPacker.pack(wp);
            store.addWaypointStep(routeId, packed, nameForPacked.apply(packed), null);
        });
    }
}
