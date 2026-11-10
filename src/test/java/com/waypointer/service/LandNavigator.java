package com.waypointer.service;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.sailing.ChartPlotterBridge;
import com.waypointer.sailing.SailingBoats;
import com.waypointer.sailing.SailingJourneyOrchestrator;
import com.waypointer.sailing.WaterMap;
import net.runelite.client.callback.ClientThread;

/** A real Navigator with sailing off and no journey running, so every request reaches the
 *  given pathfinder exactly as it did before the navigator existed. */
public final class LandNavigator
{
    private LandNavigator() { }

    public static Navigator over(WaypointPathfinder pathfinder)
    {
        SailingJourneyOrchestrator orchestrator = mock(SailingJourneyOrchestrator.class);
        lenient().when(orchestrator.activeDestination()).thenReturn(WorldPointPacker.UNDEFINED);
        return new Navigator(pathfinder, orchestrator, mock(ChartPlotterBridge.class),
            mock(SailingBoats.class), mock(WaterMap.class), mock(WaypointerConfig.class),
            mock(ClientThread.class));
    }
}
