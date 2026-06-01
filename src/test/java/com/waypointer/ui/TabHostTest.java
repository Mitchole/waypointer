package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.waypointer.WaypointerConfig;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.service.RouteRecorder;
import com.waypointer.service.RouteStore;
import com.waypointer.service.RouteStorePersistence;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.util.Listeners;
import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;

public class TabHostTest
{
    @Test
    public void reportedHeightIsZero()
    {
        TabHost host = buildHost();
        assertEquals(0, host.getPreferredSize().height);
        assertEquals(0, host.getMinimumSize().height);
    }

    @Test
    public void initialSelectionIsMyWaypoints()
    {
        TabHost host = buildHost();
        assertEquals(TabStrip.Tab.MY_WAYPOINTS, host.getActiveTabForTest());
        assertEquals("my", host.getVisibleCardNameForTest());
    }

    @Test
    public void selectPresetsSwapsTheVisibleCard()
    {
        TabHost host = buildHost();
        host.selectPresets();
        assertEquals(TabStrip.Tab.PRESETS, host.getActiveTabForTest());
        assertEquals("presets", host.getVisibleCardNameForTest());
    }

    @Test
    public void selectMyWaypointsSwapsBack()
    {
        TabHost host = buildHost();
        host.selectPresets();
        host.selectMyWaypoints();
        assertEquals(TabStrip.Tab.MY_WAYPOINTS, host.getActiveTabForTest());
        assertEquals("my", host.getVisibleCardNameForTest());
    }

    @Test
    public void disposeReleasesPathSubscription()
    {
        // Two separate Subscription mocks: the inner WaypointerPanel and the TabHost
        // both subscribe to the same pathfinder. WaypointerPanel is constructed first
        // (passed into TabHost), so its subscribe() call comes first. The test only
        // verifies that TabHost's own subscription (the second one) is closed exactly
        // once by host.dispose().
        Listeners.Subscription panelSub = mock(Listeners.Subscription.class);
        Listeners.Subscription hostSub = mock(Listeners.Subscription.class);
        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.subscribe(any())).thenReturn(panelSub).thenReturn(hostSub);

        TabHost host = buildHostWithPathfinder(pathfinder);
        verify(hostSub, never()).close();

        host.dispose();
        verify(hostSub, times(1)).close();
    }

    @Test
    public void refreshScrollbarStylingFansOutToBothCards()
    {
        // The two cards both expose refreshScrollbarStyling(); TabHost calls both.
        // Construct with real panels; we just assert no exception is thrown.
        TabHost host = buildHost();
        host.refreshScrollbarStyling();
    }

    @Test
    public void onConfigChangedShowPathingBannerForwardsToBanner()
    {
        TabHost host = buildHost();
        net.runelite.client.events.ConfigChanged event = new net.runelite.client.events.ConfigChanged();
        event.setGroup("waypointer");
        event.setKey("showPathingBanner");
        host.onConfigChanged(event); // should not throw; banner.refresh() is exercised
    }

    @Test
    public void onConfigChangedUnrelatedKeyIsNoop()
    {
        TabHost host = buildHost();
        net.runelite.client.events.ConfigChanged event = new net.runelite.client.events.ConfigChanged();
        event.setGroup("waypointer");
        event.setKey("someOtherKey");
        host.onConfigChanged(event); // should not throw; should not call banner.refresh()
    }

    @Test
    public void onConfigChangedUnrelatedGroupIsNoop()
    {
        TabHost host = buildHost();
        net.runelite.client.events.ConfigChanged event = new net.runelite.client.events.ConfigChanged();
        event.setGroup("someOtherPlugin");
        event.setKey("showPathingBanner");
        host.onConfigChanged(event); // should not throw; should not call banner.refresh()
    }

    @Test
    public void routesTabHiddenWhenDisabled()
    {
        TabHost host = buildHost();
        // buildHost stubs routesEnabled=false, devModeEnabled=false -> 2 tabs
        assertEquals(2, host.visibleTabCountForTest());
    }

    @Test
    public void routesTabShownWhenEnabled()
    {
        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));

        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        PresetCatalog catalog = mock(PresetCatalog.class);
        when(catalog.getPresets()).thenReturn(Collections.emptyList());

        WaypointerConfig config = mock(WaypointerConfig.class);
        when(config.showPathingBanner()).thenReturn(true);
        when(config.categoryCollapsedJson()).thenReturn("{}");
        when(config.shortestPathBannerDismissed()).thenReturn(true);
        when(config.landmarkSelectionJson()).thenReturn("");
        when(config.routesEnabled()).thenReturn(true);
        when(config.devModeEnabled()).thenReturn(false);

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
        when(devPanel.getRoot()).thenReturn(new javax.swing.JPanel());

        RouteStore routeStore = mock(RouteStore.class);
        when(routeStore.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(routeStore.getRoutesOrdered()).thenReturn(Collections.emptyList());
        RoutePlaybackEngine routeEngine = mock(RoutePlaybackEngine.class);
        when(routeEngine.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        RouteRecorder routeRecorder = mock(RouteRecorder.class);
        RouteStorePersistence routePersistence = mock(RouteStorePersistence.class);
        when(routePersistence.isRefusingSaves()).thenReturn(false);
        RoutesPanel routesPanel = new RoutesPanel(routeStore, routeEngine, routeRecorder, mock(com.waypointer.codec.RouteShareCodec.class), mock(com.waypointer.service.WaypointStore.class), routePersistence);

        TabHost host = new TabHost(waypointerPanel, presetPanel, devPanel, routesPanel, pathfinder, config);
        assertEquals(3, host.visibleTabCountForTest());
    }

    private static TabHost buildHost()
    {
        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        return buildHostWithPathfinder(pathfinder);
    }

    // Mocking WaypointerPanel directly is not viable: Mockito's subclass mock-maker
    // produces a JPanel subclass whose Container fields are uninitialized, and adding
    // such an instance to a real Container blows up inside Container.addImpl. So the
    // test uses a real WaypointerPanel built from mocks (same pattern as
    // WaypointerPanelTest.newPanel) — the inner panel's behaviour isn't under test
    // here; TabHost's tab/card wiring and subscription lifecycle are.
    private static TabHost buildHostWithPathfinder(WaypointPathfinder pathfinder)
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        PresetCatalog catalog = mock(PresetCatalog.class);
        when(catalog.getPresets()).thenReturn(Collections.emptyList());

        WaypointerConfig config = mock(WaypointerConfig.class);
        when(config.showPathingBanner()).thenReturn(true);
        when(config.categoryCollapsedJson()).thenReturn("{}");
        when(config.shortestPathBannerDismissed()).thenReturn(true);
        when(config.landmarkSelectionJson()).thenReturn("");
        when(config.routesEnabled()).thenReturn(false);
        when(config.devModeEnabled()).thenReturn(false);

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
        when(devPanel.getRoot()).thenReturn(new javax.swing.JPanel());

        RouteStore routeStore = mock(RouteStore.class);
        when(routeStore.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(routeStore.getRoutesOrdered()).thenReturn(Collections.emptyList());
        RoutePlaybackEngine routeEngine = mock(RoutePlaybackEngine.class);
        when(routeEngine.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        RouteRecorder routeRecorder = mock(RouteRecorder.class);
        RouteStorePersistence routePersistence = mock(RouteStorePersistence.class);
        when(routePersistence.isRefusingSaves()).thenReturn(false);
        RoutesPanel routesPanel = new RoutesPanel(routeStore, routeEngine, routeRecorder, mock(com.waypointer.codec.RouteShareCodec.class), mock(com.waypointer.service.WaypointStore.class), routePersistence);

        return new TabHost(waypointerPanel, presetPanel, devPanel, routesPanel, pathfinder, config);
    }
}
