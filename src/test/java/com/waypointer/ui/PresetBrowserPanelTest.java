package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.WaypointStore;
import java.util.Collections;
import java.util.UUID;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        PresetBrowserPanel panel = buildPanel();
        assertEquals(0, panel.getPreferredSize().height);
        assertEquals(0, panel.getMinimumSize().height);
    }

    @Test
    public void noToastTextAfterConstruction()
    {
        PresetBrowserPanel panel = buildPanel();
        org.junit.Assert.assertNull("no toast text expected at construction",
            panel.lastToastTextForTest());
    }

    @Test
    public void addDoesNotShowToast()
    {
        PresetBrowserPanel panel = buildPanel();
        Preset preset = new Preset("Cities", "", null,
            Collections.singletonList(new PresetWaypoint("Varrock", "", 3210, 3424, 0)));

        UUID newId = panel.addWaypoint(preset, preset.getWaypoints().get(0));
        assertNotNull("addWaypoint should return the new UUID", newId);
        org.junit.Assert.assertNull("add path should not call toast",
            panel.lastToastTextForTest());
    }

    @Test
    public void toastShowsRemovedMessageAfterRemove()
    {
        PresetBrowserPanel panel = buildPanel();
        Preset preset = new Preset("Cities", "", null,
            Collections.singletonList(new PresetWaypoint("Varrock", "", 3210, 3424, 0)));

        UUID newId = panel.addWaypoint(preset, preset.getWaypoints().get(0));
        panel.removeWaypoint(newId);

        String last = panel.lastToastTextForTest();
        assertNotNull("remove should fire a toast", last);
        assertTrue("expected toast to mention the waypoint, got: " + last,
            last.contains("Varrock"));
    }

    private static PresetBrowserPanel buildPanel()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        PresetCatalog catalog = mock(PresetCatalog.class);
        when(catalog.getPresets()).thenReturn(Collections.emptyList());

        return new PresetBrowserPanel(
            catalog, store, mock(SpriteManager.class), () -> { });
    }
}
