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

    private static WaypointerPanel newPanel(WaypointStore store)
    {

        WaypointerConfig config = mock(WaypointerConfig.class);
        when(config.categoryCollapsedJson()).thenReturn("{}");
        when(config.shortestPathBannerDismissed()).thenReturn(true);
        when(config.landmarkSelectionJson()).thenReturn("");

        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.isAvailable()).thenReturn(true);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));

        WaypointStorePersistence persistence = mock(WaypointStorePersistence.class);
        when(persistence.isRefusingSaves()).thenReturn(false);

        Navigator navigator = LandNavigator.over(pathfinder);

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
            mock(Client.class),
            mock(ClientThread.class),
            mock(WildernessConfirmGate.class),
            mock(WaypointShareCodec.class),
            navigator,
            sailingOrchestrator,
            mock(SailingBoats.class),
            mock(SailingBoatPin.class));
    }
}
