package com.waypointer.sailing;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointPathfinder;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SailingJourneyOrchestratorTest
{
    @Mock private SailingBoats boats;
    @Mock private ChartPlotterBridge bridge;
    @Mock private SailingBoatPin pin;
    @Mock private WaypointPathfinder pathfinder;
    @Mock private Client client;
    @Mock private SailingJourneyOrchestrator.Feedback feedback;

    private SailingJourneyOrchestrator orch;

    // Lunar Isle area target tile.
    private static final int DEST = WorldPointPacker.pack(2151, 3880, 0);

    private Waypoint waypoint()
    {
        Waypoint w = new Waypoint();
        w.setId(UUID.randomUUID());
        w.setName("Lunar Isle");
        w.setPackedWorldPoint(DEST);
        return w;
    }

    private SailingBoats.BoatInfo boat(int slot, SailingDock dock)
    {
        return new SailingBoats.BoatInfo(slot, "Boat " + slot, dock);
    }

    @Before
    public void setUp()
    {
        orch = new SailingJourneyOrchestrator(boats, bridge, pin, pathfinder, client);
        orch.setFeedback(feedback);
        lenient().when(bridge.isAvailable()).thenReturn(true);
        lenient().when(pin.isEnabled()).thenReturn(false);
    }

    @Test
    public void unavailableChartPlotterToastsAndDoesNothing()
    {
        when(bridge.isAvailable()).thenReturn(false);
        orch.start(waypoint());
        verify(feedback).show(anyString(), eq(true));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void alreadyAboardHandsOffImmediately()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(waypoint());
        verify(bridge).chart(2151, 3880);
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void onLandWithNoBoatsToasts()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(boats.mooredBoats()).thenReturn(Collections.emptyList());
        orch.start(waypoint());
        verify(feedback).show(anyString(), eq(true));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void onLandPicksNearestDockToDestination()
    {
        when(boats.isBoarded()).thenReturn(false);
        // Port Sarim (far from Lunar Isle) vs Lunar Isle dock (id 31, at the destination).
        when(boats.mooredBoats()).thenReturn(Arrays.asList(
            boat(1, SailingDock.PORT_SARIM), boat(2, SailingDock.LUNAR_ISLE)));
        orch.start(waypoint());
        int lunarDock = WorldPointPacker.pack(SailingDock.LUNAR_ISLE.getNavigationLocation());
        verify(pathfinder).requestPath(eq(lunarDock), anyString());
    }

    @Test
    public void pinnedBoatNotDockedToasts()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(pin.isEnabled()).thenReturn(true);
        when(pin.slot()).thenReturn(3);
        when(boats.boat(3)).thenReturn(null);
        orch.start(waypoint());
        verify(feedback).show(anyString(), eq(true));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void boardingWhileWalkingHandsOff()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(boats.mooredBoats()).thenReturn(Collections.singletonList(boat(1, SailingDock.LUNAR_ISLE)));
        orch.start(waypoint());
        // Now the player boards.
        when(boats.isBoarded()).thenReturn(true);
        VarbitChanged e = new VarbitChanged();
        e.setVarbitId(VarbitID.SAILING_BOARDED_BOAT);
        orch.onVarbitChanged(e);
        verify(pathfinder).clearPath();
        verify(bridge).chart(2151, 3880);
    }

    @Test
    public void arrivalResets()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(waypoint()); // -> SAILING
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        Player p = org.mockito.Mockito.mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(p);
        when(p.getWorldLocation()).thenReturn(new WorldPoint(2151, 3880, 0));
        orch.onGameTick(new GameTick());
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    @Test
    public void cancelWhileWalkingClearsLandPath()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(boats.mooredBoats()).thenReturn(Collections.singletonList(boat(1, SailingDock.LUNAR_ISLE)));
        orch.start(waypoint());
        orch.cancel();
        verify(pathfinder).clearPath();
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    @Test
    public void logoutResets()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(waypoint());
        GameStateChanged g = new GameStateChanged();
        g.setGameState(GameState.LOGIN_SCREEN);
        orch.onGameStateChanged(g);
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }
}
