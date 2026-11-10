package com.waypointer.ui;

import com.google.gson.Gson;
import com.waypointer.WaypointerConfig;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.sailing.SailingBoatPin;
import com.waypointer.sailing.SailingBoats;
import com.waypointer.sailing.SailingJourneyOrchestrator;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandNavigator;
import com.waypointer.service.Navigator;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.util.Listeners;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WaypointerPanelTest
{
    /**
     * Regression. WaypointerPanel must report height 0 from getPreferredSize and
     * getMinimumSize regardless of body content. Without that cap, PluginPanel passes
     * the layout-computed height through; ClientUI uses it as frame.getPreferredSize and
     * resizes the JFrame to fit. On Windows that unmaximizes the window the moment a
     * category is expanded, drops the game canvas to a new size, and fires
     * "Graphics reset!" in the injected client.
     */
    @Test
    public void reportedHeightIsZero()
    {
        WaypointerPanel panel = newPanel();
        assertEquals(0, panel.getPreferredSize().height);
        assertEquals(0, panel.getMinimumSize().height);
    }

    @Test
    public void addByCoordinatesOpensCaptureFormForTypedTile()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        WaypointerPanel panel = newPanel(store);
        panel.openAddByCoordinates();
        InlineInputForm form = panel.coordinatesFormForTest();
        assertTrue(form.isVisible());
        assertEquals("x, y or x, y, plane", form.getPlaceholder());

        form.setText("2301, 3690, 1");
        form.clickSubmit();

        assertFalse(form.isVisible());
        assertTrue(panel.captureFormForTest().isVisible());
        panel.captureFormForTest().clickSave();
        com.waypointer.model.Waypoint saved = store.getWaypointsInCategory(store.getUncategorized().getId()).get(0);
        assertEquals(com.waypointer.model.WorldPointPacker.pack(2301, 3690, 1),
            saved.getPackedWorldPoint());
    }

    @Test
    public void addByCoordinatesShowsInlineErrorAndStaysOpen()
    {
        WaypointerPanel panel = newPanel();
        panel.openAddByCoordinates();
        InlineInputForm form = panel.coordinatesFormForTest();

        form.setText("2301");
        form.clickSubmit();

        assertTrue(form.isVisible());
        assertEquals("Enter coordinates as x, y or x, y, plane.", form.getErrorText());
        assertFalse(panel.captureFormForTest().isVisible());
    }

    private static WaypointerPanel newPanel()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        return newPanel(store);
    }

    @Test
    public void boatPickerHiddenWithoutChartPlotter()
    {
        // Sailing is on (the default) but Chart Plotter is not running.
        WaypointerConfig config = baseConfig();
        when(config.sailingEnabled()).thenReturn(true);
        Navigator navigator = mock(Navigator.class);
        when(navigator.shortestPathOnly()).thenReturn(true);
        when(navigator.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);

        WaypointerPanel panel = newPanel(emptyStore(), config, navigator, client, clientThread);

        assertFalse(panel.sailingControlsForTest().isVisible());
        verify(clientThread, never()).invoke(any(java.util.function.BooleanSupplier.class));
    }

    @Test
    public void boatPickerShownWithChartPlotter()
    {
        Navigator navigator = mock(Navigator.class);
        when(navigator.shortestPathOnly()).thenReturn(false);
        when(navigator.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));

        WaypointerPanel panel = newPanel(emptyStore(), baseConfig(), navigator, mock(Client.class),
            mock(ClientThread.class));

        assertTrue(panel.sailingControlsForTest().isVisible());
    }

    @Test
    public void pinnedWaypointOpensOneEditor()
    {
        WaypointStore store = emptyStore();
        com.waypointer.model.Waypoint w = store.createWaypoint(
            com.waypointer.model.WorldPointPacker.pack(3164, 3487, 0), "Grand Exchange",
            store.getUncategorized().getId());
        store.setWaypointPinned(w.getId(), true);
        Navigator navigator = mock(Navigator.class);
        when(navigator.shortestPathOnly()).thenReturn(true);
        when(navigator.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        WaypointerPanel panel = newPanel(store, baseConfig(), navigator, mock(Client.class),
            mock(ClientThread.class));

        panel.expandForTest(store.getWaypointById(w.getId()));

        // The waypoint has a row under Pinned and one in its category; only one gets the form.
        assertEquals(2, count(panel, WaypointRow.class));
        assertEquals(1, count(panel, InlineEditPanel.class));
    }

    private static int count(java.awt.Container parent, Class<?> type)
    {
        int n = 0;
        for (java.awt.Component c : parent.getComponents())
        {
            if (type.isInstance(c)) n++;
            if (c instanceof java.awt.Container) n += count((java.awt.Container) c, type);
        }
        return n;
    }

    private static WaypointStore emptyStore()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        return store;
    }

    private static WaypointerConfig baseConfig()
    {
        WaypointerConfig config = mock(WaypointerConfig.class);
        when(config.categoryCollapsedJson()).thenReturn("{}");
        when(config.shortestPathBannerDismissed()).thenReturn(true);
        when(config.landmarkSelectionJson()).thenReturn("");
        return config;
    }

    private static WaypointerPanel newPanel(WaypointStore store)
    {
        WaypointPathfinder pathfinder = pathfinder();
        return newPanel(store, baseConfig(), pathfinder, LandNavigator.over(pathfinder),
            mock(Client.class), mock(ClientThread.class));
    }

    private static WaypointerPanel newPanel(WaypointStore store, WaypointerConfig config, Navigator navigator,
        Client client, ClientThread clientThread)
    {
        return newPanel(store, config, pathfinder(), navigator, client, clientThread);
    }

    private static WaypointPathfinder pathfinder()
    {
        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.isAvailable()).thenReturn(true);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        return pathfinder;
    }

    private static WaypointerPanel newPanel(WaypointStore store, WaypointerConfig config,
        WaypointPathfinder pathfinder, Navigator navigator, Client client, ClientThread clientThread)
    {
        WaypointStorePersistence persistence = mock(WaypointStorePersistence.class);
        when(persistence.isRefusingSaves()).thenReturn(false);

        NearestLandmarkBar nearestLandmarkBar = new NearestLandmarkBar(
            mock(BboxIndex.class),
            pathfinder,
            navigator,
            mock(Client.class),
            mock(ClientThread.class),
            mock(SpriteManager.class),
            config,
            new Gson(),
            mock(com.waypointer.service.PlayerPosition.class));

        SailingJourneyOrchestrator sailingOrchestrator = mock(SailingJourneyOrchestrator.class);
        return new WaypointerPanel(
            store,
            mock(WaypointCapture.class),
            pathfinder,
            config,
            new CollapseStateCodec(new Gson()),
            persistence,
            mock(SpriteManager.class),
            null,
            null,
            nearestLandmarkBar,
            client,
            clientThread,
            mock(WildernessConfirmGate.class),
            mock(WaypointShareCodec.class),
            navigator,
            sailingOrchestrator,
            mock(SailingBoats.class),
            mock(SailingBoatPin.class));
    }
}
