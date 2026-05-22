package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.service.WaypointStore;
import java.util.Collections;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PresetBrowserPanelTest
{
    /**
     * Regression: like WaypointerPanel, this PluginPanel must report height 0 so ClientUI
     * does not resize (and unmaximize) the game window when the preset list grows.
     */
    @Test
    public void reportedHeightIsZero()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        PresetCatalog catalog = mock(PresetCatalog.class);
        when(catalog.getPresets()).thenReturn(Collections.emptyList());

        PresetBrowserPanel panel = new PresetBrowserPanel(
            catalog, store, mock(SpriteManager.class), () -> { });

        assertEquals(0, panel.getPreferredSize().height);
        assertEquals(0, panel.getMinimumSize().height);
    }
}
