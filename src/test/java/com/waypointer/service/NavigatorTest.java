package com.waypointer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.sailing.ChartPlotterBridge;
import com.waypointer.sailing.SailingBoatPin;
import com.waypointer.sailing.SailingBoats;
import com.waypointer.sailing.SailingJourneyOrchestrator;
import com.waypointer.sailing.WaterMap;
import com.waypointer.service.Navigator.Router;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NavigatorTest
{
    @Mock private WaypointPathfinder pathfinder;
    @Mock private SailingJourneyOrchestrator orchestrator;
    @Mock private ChartPlotterBridge bridge;
    @Mock private SailingBoats boats;
    @Mock private WaterMap waterMap;
    @Mock private WaypointerConfig config;
    @Mock private ClientThread clientThread;

    private Navigator navigator;
    // True while a runnable handed to the client thread mock is running.
    private boolean onClientThread;

    private static final int LAND = WorldPointPacker.pack(3222, 3218, 0);
    private static final int SEA = WorldPointPacker.pack(3051, 3195, 0);

    @Before
    public void setUp()
    {
        navigator = new Navigator(pathfinder, orchestrator, bridge, boats, waterMap, config, clientThread);
        lenient().doAnswer(inv ->
        {
            boolean outer = onClientThread;
            onClientThread = true;
            try { inv.<Runnable>getArgument(0).run(); }
            finally { onClientThread = outer; }
            return null;
        }).when(clientThread).invoke(any(Runnable.class));
        lenient().when(config.sailingEnabled()).thenReturn(true);
        lenient().when(bridge.isAvailable()).thenReturn(true);
        lenient().when(boats.isBoarded()).thenReturn(false);
        lenient().when(waterMap.isAtSea(WorldPointPacker.unpack(LAND))).thenReturn(false);
        lenient().when(waterMap.isAtSea(WorldPointPacker.unpack(SEA))).thenReturn(true);
        lenient().when(orchestrator.activeDestination()).thenReturn(WorldPointPacker.UNDEFINED);
        lenient().when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.UNDEFINED);
    }

    @Test
    public void chooseTable()
    {
        for (int bits = 0; bits < 16; bits++)
        {
            boolean sailing = (bits & 1) != 0;
            boolean plotter = (bits & 2) != 0;
            boolean boarded = (bits & 4) != 0;
            boolean atSea = (bits & 8) != 0;
            Router expected;
            if (!sailing || !plotter) expected = Router.SHORTEST_PATH;
            else if (boarded) expected = Router.CHART_PLOTTER;
            else if (atSea) expected = Router.DOCK_JOURNEY;
            else expected = Router.SHORTEST_PATH;
            assertEquals("sailing=" + sailing + " plotter=" + plotter + " boarded=" + boarded + " atSea=" + atSea,
                expected, Navigator.choose(sailing, plotter, boarded, atSea));
        }
    }

    @Test
    public void landTargetOnLandRequestsShortestPathOnly()
    {
        navigator.go(LAND, "Varrock");
        verify(pathfinder).requestPath(LAND, "Varrock");
        verify(orchestrator, never()).start(anyInt(), anyString(), anyBoolean());
        verify(orchestrator, never()).cancel();
    }

    @Test
    public void aboardSendsAnyTargetToOrchestrator()
    {
        when(boats.isBoarded()).thenReturn(true);
        assertEquals(Router.CHART_PLOTTER, navigator.routerFor(LAND));
        navigator.go(LAND, "Port Sarim");
        verify(orchestrator).start(LAND, "Port Sarim", false);
        navigator.go(SEA, "Open sea");
        verify(orchestrator).start(SEA, "Open sea", false);
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void seaTargetOnLandStartsDockJourney()
    {
        assertEquals(Router.DOCK_JOURNEY, navigator.routerFor(SEA));
        navigator.go(SEA, "Open sea");
        verify(orchestrator).start(SEA, "Open sea", false);
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void goStepFlagsTheJourneyAsARouteStep()
    {
        navigator.goStep(SEA, "Step 2");
        verify(orchestrator).start(SEA, "Step 2", true);
    }

    @Test
    public void switchingRouterClearsTheOther()
    {
        when(orchestrator.activeDestination()).thenReturn(SEA);
        navigator.go(LAND, "Varrock");
        InOrder order = inOrder(orchestrator, pathfinder);
        order.verify(orchestrator).cancel();
        order.verify(pathfinder).requestPath(LAND, "Varrock");
    }

    @Test
    public void standalonePathIsClearedBeforeASeaJourney()
    {
        when(pathfinder.getActiveTarget()).thenReturn(LAND);
        navigator.go(SEA, "Open sea");
        InOrder order = inOrder(orchestrator, pathfinder);
        order.verify(pathfinder).clearPath();
        order.verify(orchestrator).start(SEA, "Open sea", false);
    }

    @Test
    public void chartPlotterDisabledMidJourneyFallsBack()
    {
        when(orchestrator.activeDestination()).thenReturn(SEA);
        lenient().when(boats.isBoarded()).thenReturn(true); // aboard, but never read once the plotter is gone
        when(bridge.isAvailable()).thenReturn(false);
        navigator.go(LAND, "Next");
        verify(orchestrator).cancel();
        verify(pathfinder).requestPath(LAND, "Next");
        verify(orchestrator, never()).start(anyInt(), anyString(), anyBoolean());
    }

    @Test
    public void sailingSettingOffMidJourneyFallsBack()
    {
        when(orchestrator.activeDestination()).thenReturn(SEA);
        when(config.sailingEnabled()).thenReturn(false);
        navigator.go(SEA, "Next");
        verify(orchestrator).cancel();
        verify(pathfinder).requestPath(SEA, "Next");
        verify(orchestrator, never()).start(anyInt(), anyString(), anyBoolean());
    }

    @Test
    public void stopClearsBoth()
    {
        when(orchestrator.activeDestination()).thenReturn(SEA);
        navigator.stop();
        verify(orchestrator).cancel();
        verify(pathfinder).clearPath();
    }

    @Test
    public void activeTargetPrefersOrchestrator()
    {
        when(pathfinder.getActiveTarget()).thenReturn(LAND);
        lenient().when(pathfinder.getActiveName()).thenReturn("Dock");
        assertEquals(LAND, navigator.activeTarget());
        assertEquals("Dock", navigator.activeName());

        when(orchestrator.activeDestination()).thenReturn(SEA);
        when(orchestrator.activeName()).thenReturn("Open sea");
        assertEquals(SEA, navigator.activeTarget());
        assertEquals("Open sea", navigator.activeName());
    }

    @Test
    public void arrivedUsesSixTilesAboardThreeOnFoot()
    {
        int target = WorldPointPacker.pack(3000, 3000, 0);
        WorldPoint fiveAway = new WorldPoint(3005, 3005, 0);
        WorldPoint twoAway = new WorldPoint(3002, 3000, 0);

        when(boats.isBoarded()).thenReturn(true);
        assertTrue(navigator.arrived(target, fiveAway));
        assertTrue(navigator.arrived(target, new WorldPoint(3006, 2994, 0)));
        assertFalse(navigator.arrived(target, new WorldPoint(3007, 3000, 0)));

        when(boats.isBoarded()).thenReturn(false);
        assertFalse(navigator.arrived(target, fiveAway));
        assertTrue(navigator.arrived(target, twoAway));
        assertFalse(navigator.arrived(target, null));
    }

    @Test
    public void handoverToastShownOncePerSession()
    {
        // A real orchestrator, so the handover it performs drives the toast.
        SailingBoats realBoats = mock(SailingBoats.class);
        ChartPlotterBridge realBridge = mock(ChartPlotterBridge.class);
        SailingJourneyOrchestrator.Feedback feedback = mock(SailingJourneyOrchestrator.Feedback.class);
        Client client = mock(Client.class);
        PlayerPosition position = mock(PlayerPosition.class);
        when(client.getGameState()).thenReturn(net.runelite.api.GameState.LOGGED_IN);
        when(position.isAboard()).thenReturn(true);
        when(position.current()).thenReturn(new net.runelite.api.coords.WorldPoint(2000, 2000, 0));
        SailingJourneyOrchestrator real = new SailingJourneyOrchestrator(realBoats, realBridge,
            mock(SailingBoatPin.class), pathfinder, client, position);
        real.setFeedback(feedback);
        when(realBridge.isAvailable()).thenReturn(true);
        when(realBoats.isBoarded()).thenReturn(true);
        Navigator nav = new Navigator(pathfinder, real, realBridge, realBoats, waterMap, config, clientThread);

        String toast = "Course sent to Chart Plotter. If nothing draws, turn on its charted line.";
        // The course, and with it the toast, goes out on the next tick aboard.
        nav.go(SEA, "First");
        real.onGameTick(new net.runelite.api.events.GameTick());
        nav.go(LAND, "Second");
        real.onGameTick(new net.runelite.api.events.GameTick());
        verify(feedback, times(1)).show(toast, false);

        real.resetSession();
        nav.go(SEA, "Third");
        real.onGameTick(new net.runelite.api.events.GameTick());
        verify(feedback, times(2)).show(toast, false);
        verify(feedback, never()).show(anyString(), eq(true));
    }

    @Test
    public void noActiveTargetWhenBothIdle()
    {
        assertEquals(WorldPointPacker.UNDEFINED, navigator.activeTarget());
        assertNull(navigator.activeName());
    }

    @Test
    public void resolveRouterIsSynchronousWithoutChartPlotter() throws Exception
    {
        when(bridge.isAvailable()).thenReturn(false);
        AtomicReference<Router> got = new AtomicReference<>();
        navigator.resolveRouter(SEA, got::set);
        assertEquals(Router.SHORTEST_PATH, got.get());
        verifyNoInteractions(clientThread);
        verifyNoInteractions(boats);
    }

    @Test
    public void resolveRouterWithSailingOffNeverTouchesBoats()
    {
        when(config.sailingEnabled()).thenReturn(false);
        AtomicReference<Router> got = new AtomicReference<>();
        navigator.resolveRouter(SEA, got::set);
        assertEquals(Router.SHORTEST_PATH, got.get());
        navigator.go(SEA, "Sea");
        verify(pathfinder).requestPath(SEA, "Sea");
        verifyNoInteractions(clientThread);
        verifyNoInteractions(boats);
    }

    @Test
    public void resolveRouterHopsToClientThreadWhenChartPlotterActive() throws Exception
    {
        ClientThread capturing = mock(ClientThread.class);
        List<Runnable> queued = new ArrayList<>();
        doAnswer(inv -> queued.add(inv.getArgument(0))).when(capturing).invoke(any(Runnable.class));
        Navigator nav = new Navigator(pathfinder, orchestrator, bridge, boats, waterMap, config, capturing);

        AtomicReference<Router> got = new AtomicReference<>();
        nav.resolveRouter(SEA, got::set);
        assertEquals(1, queued.size());
        verifyNoInteractions(boats);
        assertNull(got.get());

        queued.get(0).run();
        verify(boats).isBoarded();
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(Router.DOCK_JOURNEY, got.get());
    }

    @Test
    public void goWithRouterDoesNotRecompute()
    {
        AtomicReference<Router> got = new AtomicReference<>();
        when(bridge.isAvailable()).thenReturn(false);
        navigator.resolveRouter(SEA, got::set);
        assertEquals(Router.SHORTEST_PATH, got.get());

        // Chart Plotter comes back while the caller is still deciding; its answer stands.
        // Lenient because go(..., router) must not read it at all.
        lenient().when(bridge.isAvailable()).thenReturn(true);
        navigator.go(SEA, "Sea", got.get());
        verify(pathfinder).requestPath(SEA, "Sea");
        verify(orchestrator, never()).start(anyInt(), anyString(), anyBoolean());
        verifyNoInteractions(boats);
    }

    @Test
    public void seaDispatchRunsOnClientThread()
    {
        doAnswer(inv ->
        {
            assertTrue("orchestrator.start must run on the client thread", onClientThread);
            return null;
        }).when(orchestrator).start(anyInt(), anyString(), anyBoolean());
        when(boats.isBoarded()).thenAnswer(inv ->
        {
            assertTrue("isBoarded must run on the client thread", onClientThread);
            return true;
        });

        navigator.go(SEA, "Sea", Router.DOCK_JOURNEY);
        navigator.go(SEA, "Sea");
        navigator.goStep(LAND, "Step");
        verify(orchestrator, times(2)).start(SEA, "Sea", false);
        verify(orchestrator).start(LAND, "Step", true);
    }
}
