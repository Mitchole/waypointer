package com.waypointer.sailing;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointPathfinder;
import java.util.Arrays;
import java.util.Collections;
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
    private com.waypointer.service.PlayerPosition playerPosition;

    // Lunar Isle area target tile.
    private static final int DEST = WorldPointPacker.pack(2151, 3880, 0);

    private SailingBoats.BoatInfo boat(int slot, SailingDock dock)
    {
        return new SailingBoats.BoatInfo(slot, "Boat " + slot, dock);
    }

    @Before
    public void setUp()
    {
        playerPosition = org.mockito.Mockito.mock(com.waypointer.service.PlayerPosition.class);
        orch = new SailingJourneyOrchestrator(boats, bridge, pin, pathfinder, client, playerPosition);
        orch.setFeedback(feedback);
        lenient().when(bridge.isAvailable()).thenReturn(true);
        lenient().when(pin.isEnabled()).thenReturn(false);
    }

    @Test
    public void unavailableChartPlotterToastsAndDoesNothing()
    {
        when(bridge.isAvailable()).thenReturn(false);
        orch.start(DEST, "Lunar Isle", false);
        verify(feedback).show(anyString(), eq(true));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void alreadyAboardHandsOffImmediately()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", false);
        verify(bridge).chart(2151, 3880);
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void onLandWithNoBoatsToasts()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(boats.mooredBoats()).thenReturn(Collections.emptyList());
        orch.start(DEST, "Lunar Isle", false);
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
        orch.start(DEST, "Lunar Isle", false);
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
        orch.start(DEST, "Lunar Isle", false);
        verify(feedback).show(anyString(), eq(true));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void boardingWhileWalkingHandsOff()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(boats.mooredBoats()).thenReturn(Collections.singletonList(boat(1, SailingDock.LUNAR_ISLE)));
        orch.start(DEST, "Lunar Isle", false);
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
        orch.start(DEST, "Lunar Isle", false); // -> SAILING
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(playerPosition.current()).thenReturn(new WorldPoint(2151, 3880, 0));
        orch.onGameTick(new GameTick());
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    @Test
    public void cancelWhileWalkingClearsLandPath()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(boats.mooredBoats()).thenReturn(Collections.singletonList(boat(1, SailingDock.LUNAR_ISLE)));
        orch.start(DEST, "Lunar Isle", false);
        orch.cancel();
        verify(pathfinder).clearPath();
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    @Test
    public void logoutResets()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", false);
        GameStateChanged g = new GameStateChanged();
        g.setGameState(GameState.LOGIN_SCREEN);
        orch.onGameStateChanged(g);
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    @Test
    public void switchingTargetsClearsPriorLeg()
    {
        // Journey A: already aboard -> SAILING (bridge.chart to A).
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", false);
        // Journey B: now on land with a moored boat.
        when(boats.isBoarded()).thenReturn(false);
        when(boats.mooredBoats()).thenReturn(java.util.Collections.singletonList(boat(1, SailingDock.LUNAR_ISLE)));
        orch.start(WorldPointPacker.pack(2150, 3530, 0), "B", false);
        // The prior SAILING leg must have been cleared before the new land leg started.
        verify(bridge).clear();
        verify(pathfinder).requestPath(anyInt(), anyString());
    }

    @Test
    public void pinnedBoatNameDriftToasts()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(pin.isEnabled()).thenReturn(true);
        when(pin.slot()).thenReturn(3);
        when(pin.name()).thenReturn("Old Name"); // boat(3,...) is named "Boat 3" -> drift
        when(boats.boat(3)).thenReturn(boat(3, SailingDock.LUNAR_ISLE));
        orch.start(DEST, "Lunar Isle", false);
        verify(feedback).show(anyString(), eq(true));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void pinnedBoatWithMatchingNameStartsTheJourney()
    {
        when(boats.isBoarded()).thenReturn(false);
        when(pin.isEnabled()).thenReturn(true);
        when(pin.slot()).thenReturn(3);
        when(pin.name()).thenReturn("Boat 3");
        when(boats.boat(3)).thenReturn(boat(3, SailingDock.PORT_SARIM));
        orch.start(DEST, "Lunar Isle", false);
        verify(feedback, never()).show(anyString(), eq(true));
        verify(pathfinder).requestPath(eq(WorldPointPacker.pack(SailingDock.PORT_SARIM.getNavigationLocation())), anyString());
        org.junit.Assert.assertEquals(DEST, orch.activeDestination());
    }

    @Test
    public void cancelWhileSailingClearsChart()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", false); // -> SAILING
        orch.cancel();
        verify(bridge).clear();
        org.junit.Assert.assertEquals(com.waypointer.model.WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    private void leaveBoat()
    {
        when(boats.isBoarded()).thenReturn(false);
        VarbitChanged e = new VarbitChanged();
        e.setVarbitId(VarbitID.SAILING_BOARDED_BOAT);
        orch.onVarbitChanged(e);
    }

    @Test
    public void disembarkWarns()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", false);
        leaveBoat();
        verify(feedback).show("Sailing cancelled - you left the boat.", true);
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    @Test
    public void routeStepDisembarkIsSilent()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", true);
        leaveBoat();
        verify(feedback, never()).show(anyString(), eq(true));
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }

    @Test
    public void activeNameFollowsTheJourney()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", false);
        org.junit.Assert.assertEquals("Lunar Isle", orch.activeName());
        orch.cancel();
        org.junit.Assert.assertNull(orch.activeName());
    }

    @Test
    public void arrivalUsesTheBoatRadius()
    {
        when(boats.isBoarded()).thenReturn(true);
        orch.start(DEST, "Lunar Isle", false);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(playerPosition.current()).thenReturn(new WorldPoint(2157, 3874, 0));
        orch.onGameTick(new GameTick());
        org.junit.Assert.assertEquals(WorldPointPacker.UNDEFINED, orch.activeDestination());
    }
}
