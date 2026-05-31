package com.waypointer.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.waypointer.WaypointerConfig;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.util.Listeners;
import java.util.Collections;
import javax.swing.JPanel;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DevModeConfigToggleTest
{
    @Test
    public void devOffShowsTwoTabs()
    {
        TabHost host = newHostWith(false);
        assertEquals(2, host.visibleTabCountForTest());
    }

    @Test
    public void devOnShowsThreeTabs()
    {
        TabHost host = newHostWith(true);
        assertEquals(3, host.visibleTabCountForTest());
    }

    @Test
    public void flipOffWhileDevActiveSelectsMyWaypoints()
    {
        WaypointerConfig cfg = newConfig(true);
        TabHost host = buildHostWith(cfg);
        host.selectDev();

        when(cfg.devModeEnabled()).thenReturn(false);
        host.onDevModeChangedForTest();

        assertEquals(TabStrip.Tab.MY_WAYPOINTS, host.getActiveTabForTest());
        assertEquals(2, host.visibleTabCountForTest());
    }

    private static TabHost newHostWith(boolean devEnabled)
    {
        return buildHostWith(newConfig(devEnabled));
    }

    private static WaypointerConfig newConfig(boolean devEnabled)
    {
        WaypointerConfig cfg = mock(WaypointerConfig.class);
        when(cfg.devModeEnabled()).thenReturn(devEnabled);
        when(cfg.showPathingBanner()).thenReturn(true);
        when(cfg.categoryCollapsedJson()).thenReturn("{}");
        when(cfg.shortestPathBannerDismissed()).thenReturn(true);
        when(cfg.landmarkSelectionJson()).thenReturn("");
        return cfg;
    }

    // Same pattern as TabHostTest: WaypointerPanel cannot be Mockito-mocked because
    // the subclass mock-maker produces a JPanel with uninitialized Container fields,
    // which blows up when added to the cards container. Build a real one from mocks.
    private static TabHost buildHostWith(WaypointerConfig config)
    {
        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));

        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        PresetCatalog catalog = mock(PresetCatalog.class);
        when(catalog.getPresets()).thenReturn(Collections.emptyList());

        WaypointStorePersistence persistence = mock(WaypointStorePersistence.class);
        when(persistence.isRefusingSaves()).thenReturn(false);

        NearestLandmarkBar nearestLandmarkBar = new NearestLandmarkBar(
            mock(BboxIndex.class),
            pathfinder,
            mock(Client.class),
            mock(ClientThread.class),
            mock(SpriteManager.class),
            config,
            new Gson());

        WaypointerPanel waypointerPanel = new WaypointerPanel(
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
            mock(LibraryJsonCodec.class));
        PresetBrowserPanel presetPanel =
            new PresetBrowserPanel(catalog, store, mock(SpriteManager.class));

        DevPanel devPanel = mock(DevPanel.class);
        when(devPanel.getRoot()).thenReturn(new JPanel());

        return new TabHost(waypointerPanel, presetPanel, devPanel, pathfinder, config);
    }
}
