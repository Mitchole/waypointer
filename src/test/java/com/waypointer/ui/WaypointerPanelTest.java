package com.waypointer.ui;

import com.google.gson.Gson;
import com.waypointer.WaypointerConfig;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.util.Listeners;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
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

    private static WaypointerPanel newPanel()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        WaypointerConfig config = mock(WaypointerConfig.class);
        when(config.categoryCollapsedJson()).thenReturn("{}");
        when(config.shortestPathBannerDismissed()).thenReturn(true);

        WaypointPathfinder pathfinder = mock(WaypointPathfinder.class);
        when(pathfinder.isAvailable()).thenReturn(true);
        when(pathfinder.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));

        WaypointStorePersistence persistence = mock(WaypointStorePersistence.class);
        when(persistence.isRefusingSaves()).thenReturn(false);

        return new WaypointerPanel(
            store,
            mock(WaypointCapture.class),
            pathfinder,
            config,
            new CollapseStateCodec(new Gson()),
            mock(WaypointerNavigator.class),
            mock(WaypointShareCodec.class),
            persistence,
            mock(SpriteManager.class),
            null,
            null,
            new NearestLandmarkBar());
    }
}
