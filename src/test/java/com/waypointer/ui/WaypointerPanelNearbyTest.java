package com.waypointer.ui;

import com.google.gson.Gson;
import com.waypointer.WaypointerConfig;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.NearbyComputer;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.util.Listeners;
import java.awt.Component;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WaypointerPanelNearbyTest
{
    @Test
    public void rendersNearbySectionWhenComputerHasResults()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        Waypoint w = store.createWaypoint(
            WorldPointPacker.pack(3200, 3200, 0), "near", store.getUncategorized().getId());

        NearbyComputer computer = mock(NearbyComputer.class);
        when(computer.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(computer.getCurrent()).thenReturn(List.of(w));

        WaypointerPanel panel = newPanel(store, computer);
        panel.rebuild();

        assertNotNull("NearbySection should be present in panel body",
            findNearbySection(panel));
    }

    @Test
    public void hidesNearbySectionWhenComputerEmpty()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        NearbyComputer computer = mock(NearbyComputer.class);
        when(computer.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(computer.getCurrent()).thenReturn(Collections.emptyList());

        WaypointerPanel panel = newPanel(store, computer);
        panel.rebuild();

        assertNull("NearbySection should not render when computer returns empty",
            findNearbySection(panel));
    }

    @Test
    public void hidesNearbySectionWhileSearchFilterActive()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        Waypoint w = store.createWaypoint(
            WorldPointPacker.pack(3200, 3200, 0), "near", store.getUncategorized().getId());

        NearbyComputer computer = mock(NearbyComputer.class);
        when(computer.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(computer.getCurrent()).thenReturn(List.of(w));

        WaypointerPanel panel = newPanel(store, computer);
        panel.setFilterForTest("anything");
        panel.rebuild();

        assertNull("NearbySection should be suppressed while a search filter is active",
            findNearbySection(panel));
    }

    /**
     * Walks the JPanel tree rooted at the panel and returns the first NearbySection it finds,
     * or null. Doesn't recurse into NearbySection itself.
     */
    private static NearbySection findNearbySection(java.awt.Container root)
    {
        for (Component c : root.getComponents())
        {
            if (c instanceof NearbySection) return (NearbySection) c;
            if (c instanceof java.awt.Container)
            {
                NearbySection deep = findNearbySection((java.awt.Container) c);
                if (deep != null) return deep;
            }
        }
        return null;
    }

    private static WaypointerPanel newPanel(WaypointStore store, NearbyComputer computer)
    {
        WaypointerConfig config = mock(WaypointerConfig.class);
        when(config.categoryCollapsedJson()).thenReturn("{}");
        when(config.shortestPathBannerDismissed()).thenReturn(true);

        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.isAvailable()).thenReturn(true);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.UNDEFINED);

        WaypointStorePersistence persistence = mock(WaypointStorePersistence.class);
        when(persistence.isRefusingSaves()).thenReturn(false);

        NearestLandmarkBar bar = new NearestLandmarkBar(
            mock(BboxIndex.class), pathfinder,
            mock(Client.class), mock(ClientThread.class), mock(SpriteManager.class));

        return new WaypointerPanel(
            store,
            mock(WaypointCapture.class),
            pathfinder,
            config,
            new CollapseStateCodec(new Gson()),
            mock(WaypointShareCodec.class),
            persistence,
            mock(SpriteManager.class),
            null,
            null,
            bar,
            new LibraryJsonCodec(new Gson()),
            mock(Client.class),
            mock(ClientThread.class),
            mock(WildernessConfirmGate.class),
            computer);
    }
}
