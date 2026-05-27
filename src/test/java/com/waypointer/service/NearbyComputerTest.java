package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.util.Listeners;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NearbyComputerTest
{
    private Client client;
    private ClientThread clientThread;
    private WaypointStore store;
    private WaypointPathfinder pathfinder;
    private WaypointerConfig config;
    private NearbyComputer computer;

    @Before
    public void setUp()
    {
        client = mock(Client.class);
        clientThread = mock(ClientThread.class);
        // Run any Runnable passed to clientThread.invoke synchronously so subscription
        // callbacks (store/path) actually execute in tests.
        doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(clientThread).invoke(any(Runnable.class));

        store = new WaypointStore();
        store.bootstrap(new Library());

        pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.UNDEFINED);

        config = mock(WaypointerConfig.class);
        when(config.showNearbySection()).thenReturn(true);
        when(config.nearbyCount()).thenReturn(4);
        when(config.nearbySamePlaneOnly()).thenReturn(true);

        // Default: logged in at (3200, 3200, 0)
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        Player p = mock(Player.class);
        when(p.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        when(client.getLocalPlayer()).thenReturn(p);

        computer = new NearbyComputer(client, clientThread, store, pathfinder, config);
    }

    private static void addWaypoint(WaypointStore s, int x, int y, int plane, String name)
    {
        s.createWaypoint(WorldPointPacker.pack(x, y, plane),
            name, s.getUncategorized().getId());
    }

    private static List<UUID> idsOf(List<Waypoint> ws)
    {
        return ws.stream().map(Waypoint::getId).collect(java.util.stream.Collectors.toList());
    }

    private void fireTicks(int n)
    {
        for (int i = 0; i < n; i++) computer.onGameTick(new GameTick());
    }

    // --- Logged-out / disabled / empty paths ---

    @Test
    public void returnsEmptyWhenNotLoggedIn()
    {
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        addWaypoint(store, 3201, 3201, 0, "near");
        fireTicks(6);
        assertTrue(computer.getCurrent().isEmpty());
    }

    @Test
    public void returnsEmptyWhenFeatureDisabled()
    {
        when(config.showNearbySection()).thenReturn(false);
        addWaypoint(store, 3201, 3201, 0, "near");
        fireTicks(6);
        assertTrue(computer.getCurrent().isEmpty());
    }

    @Test
    public void returnsEmptyWhenLibraryEmpty()
    {
        fireTicks(6);
        assertTrue(computer.getCurrent().isEmpty());
    }

    // --- Distance + selection ---

    @Test
    public void returnsNClosestByChebyshevAscending()
    {
        // Player at (3200, 3200, 0). N=4. Add 5 waypoints at increasing Chebyshev distances.
        addWaypoint(store, 3201, 3200, 0, "d1");   // dist 1
        addWaypoint(store, 3200, 3202, 0, "d2");   // dist 2
        addWaypoint(store, 3203, 3203, 0, "d3");   // dist 3 (max(3,3))
        addWaypoint(store, 3204, 3200, 0, "d4");   // dist 4
        addWaypoint(store, 3205, 3205, 0, "d5");   // dist 5 -- should NOT make the cut
        fireTicks(6);
        List<Waypoint> got = computer.getCurrent();
        assertEquals(4, got.size());
        assertEquals("d1", got.get(0).getName());
        assertEquals("d2", got.get(1).getName());
        assertEquals("d3", got.get(2).getName());
        assertEquals("d4", got.get(3).getName());
    }

    @Test
    public void chebyshevUsesMaxOfDxDy()
    {
        // Player at (3200, 3200, 0). Two waypoints: A at dx=5,dy=0 (Chebyshev 5),
        // B at dx=4,dy=4 (Chebyshev 4). B must rank before A.
        addWaypoint(store, 3205, 3200, 0, "A");
        addWaypoint(store, 3204, 3204, 0, "B");
        fireTicks(6);
        List<Waypoint> got = computer.getCurrent();
        assertEquals("B", got.get(0).getName());
        assertEquals("A", got.get(1).getName());
    }

    // --- Exclusions ---

    @Test
    public void excludesPinnedWaypoints()
    {
        addWaypoint(store, 3201, 3200, 0, "pinnedNear");
        addWaypoint(store, 3203, 3200, 0, "far");
        UUID pinnedId = store.getLibrary().getWaypoints().get(0).getId();
        store.setWaypointPinned(pinnedId, true);
        fireTicks(6);
        List<Waypoint> got = computer.getCurrent();
        assertEquals(1, got.size());
        assertEquals("far", got.get(0).getName());
    }

    @Test
    public void excludesActivePathTarget()
    {
        addWaypoint(store, 3201, 3200, 0, "near");
        addWaypoint(store, 3210, 3200, 0, "farButOnly");
        int nearPacked = store.getLibrary().getWaypoints().get(0).getPackedWorldPoint();
        when(pathfinder.getActiveTarget()).thenReturn(nearPacked);
        fireTicks(6);
        List<Waypoint> got = computer.getCurrent();
        assertEquals(1, got.size());
        assertEquals("farButOnly", got.get(0).getName());
    }

    @Test
    public void samePlaneOnlyExcludesOtherPlanes()
    {
        // Player on plane 0.
        addWaypoint(store, 3201, 3200, 0, "samePlane");
        addWaypoint(store, 3201, 3200, 1, "otherPlane");
        fireTicks(6);
        List<Waypoint> got = computer.getCurrent();
        assertEquals(1, got.size());
        assertEquals("samePlane", got.get(0).getName());
    }

    @Test
    public void samePlaneOnlyOffIncludesOtherPlanes()
    {
        when(config.nearbySamePlaneOnly()).thenReturn(false);
        addWaypoint(store, 3201, 3200, 0, "samePlane");
        addWaypoint(store, 3201, 3200, 1, "otherPlane");
        fireTicks(6);
        List<Waypoint> got = computer.getCurrent();
        assertEquals(2, got.size());
    }

    // --- Throttle ---

    @Test
    public void throttleSuppressesListenerForFirstFiveTicks()
    {
        addWaypoint(store, 3201, 3200, 0, "near");
        AtomicInteger fires = new AtomicInteger();
        computer.subscribe(fires::incrementAndGet);

        // Reset baseline from subscription wiring.
        int baseline = fires.get();
        fireTicks(5);
        assertEquals("first 5 ticks must not fire (counter increments only)",
            baseline, fires.get());
    }

    @Test
    public void throttleFiresOnSixthTickIfResultChanged()
    {
        // Settle initial state: two waypoints, 'a' (dist 1) closer than 'b' (dist 10).
        addWaypoint(store, 3201, 3200, 0, "a");
        addWaypoint(store, 3210, 3200, 0, "b");
        fireTicks(6);
        assertEquals("a", computer.getCurrent().get(0).getName());

        AtomicInteger fires = new AtomicInteger();
        computer.subscribe(fires::incrementAndGet);

        // Move the player so 'b' is now closer than 'a'. The computer has no listener on
        // player location, so this change is only picked up on the next throttled tick.
        Player p2 = mock(Player.class);
        when(p2.getWorldLocation()).thenReturn(new WorldPoint(3210, 3200, 0));
        when(client.getLocalPlayer()).thenReturn(p2);

        fireTicks(5);
        assertEquals("throttle suppresses recompute for ticks 1-5 despite player movement",
            0, fires.get());
        fireTicks(1);
        assertEquals("6th tick recomputes and fires because the closest waypoint changed",
            1, fires.get());
        assertEquals("b", computer.getCurrent().get(0).getName());
    }

    @Test
    public void stableResultDoesNotFireOnSubsequentThrottleWindows()
    {
        addWaypoint(store, 3201, 3200, 0, "near");
        AtomicInteger fires = new AtomicInteger();
        // First fire-window (6 ticks) populates lastIds.
        fireTicks(6);
        computer.subscribe(fires::incrementAndGet);
        // 12 more ticks, no movement, no library change. Compute happens at ticks 12 and 18
        // (relative to start) but produces identical IDs; listener must not fire.
        fireTicks(12);
        assertEquals(0, fires.get());
    }

    // --- External invalidation ---

    @Test
    public void configChangeRecomputesImmediately()
    {
        // Seed and let one throttle window populate the cache.
        addWaypoint(store, 3201, 3200, 0, "a");
        addWaypoint(store, 3202, 3200, 0, "b");
        addWaypoint(store, 3203, 3200, 0, "c");
        addWaypoint(store, 3204, 3200, 0, "d");
        addWaypoint(store, 3205, 3200, 0, "e");
        fireTicks(6);
        assertEquals(4, computer.getCurrent().size());

        AtomicInteger fires = new AtomicInteger();
        computer.subscribe(fires::incrementAndGet);

        // Now shrink the count to 3 via a ConfigChanged. Expect synchronous fire.
        when(config.nearbyCount()).thenReturn(3);
        ConfigChanged e = new ConfigChanged();
        e.setGroup("waypointer");
        e.setKey("nearbyCount");
        computer.onConfigChanged(e);

        assertEquals(1, fires.get());
        assertEquals(3, computer.getCurrent().size());
    }

    @Test
    public void storeMutationRecomputesImmediately()
    {
        addWaypoint(store, 3201, 3200, 0, "a");
        fireTicks(6);
        AtomicInteger fires = new AtomicInteger();
        computer.subscribe(fires::incrementAndGet);

        // Pinning the only nearby waypoint removes it from the result.
        UUID aId = store.getLibrary().getWaypoints().get(0).getId();
        store.setWaypointPinned(aId, true);

        assertEquals(1, fires.get());
        assertTrue(computer.getCurrent().isEmpty());
    }

    @Test
    public void disposeClosesSubscriptions()
    {
        addWaypoint(store, 3201, 3200, 0, "a");
        fireTicks(6);
        AtomicInteger fires = new AtomicInteger();
        computer.subscribe(fires::incrementAndGet);

        computer.dispose();

        // Mutating the store after dispose must not fire the computer's listener anymore.
        addWaypoint(store, 3202, 3200, 0, "b");
        assertEquals(0, fires.get());
    }

    @Test
    public void configKeyOutsideAllowlistDoesNotRecompute()
    {
        addWaypoint(store, 3201, 3200, 0, "a");
        fireTicks(6);
        AtomicInteger fires = new AtomicInteger();
        computer.subscribe(fires::incrementAndGet);

        ConfigChanged e = new ConfigChanged();
        e.setGroup("waypointer");
        e.setKey("showWildernessGlyph");   // not a nearby key
        computer.onConfigChanged(e);

        assertEquals(0, fires.get());
    }

    @Test
    public void configChangeInOtherGroupIgnored()
    {
        addWaypoint(store, 3201, 3200, 0, "a");
        fireTicks(6);
        AtomicInteger fires = new AtomicInteger();
        computer.subscribe(fires::incrementAndGet);

        ConfigChanged e = new ConfigChanged();
        e.setGroup("shortestpath");
        e.setKey("nearbyCount");   // right key, wrong group
        computer.onConfigChanged(e);

        assertEquals(0, fires.get());
    }
}
