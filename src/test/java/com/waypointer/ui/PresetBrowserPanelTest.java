package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.WaypointStore;
import java.awt.Component;
import java.util.Arrays;
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

    @Test
    public void rebuildAfterStoreMutationPreservesExpandedSections()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        Preset cities = new Preset("Cities", "", null,
            Arrays.asList(new PresetWaypoint("Varrock", "", 3210, 3424, 0)));
        Preset bosses = new Preset("Bosses", "", null,
            Arrays.asList(new PresetWaypoint("Vorkath", "", 2272, 4052, 0)));
        PresetCatalog catalog = mock(PresetCatalog.class);
        when(catalog.getPresets()).thenReturn(Arrays.asList(cities, bosses));

        PresetBrowserPanel panel = new PresetBrowserPanel(catalog, store, mock(SpriteManager.class));
        PresetSection citiesSection = findSection(panel, "Cities");
        assertNotNull("Cities section must render", citiesSection);
        citiesSection.setExpanded(true);
        assertTrue("Cities should be expanded after setExpanded", citiesSection.isExpanded());

        panel.scheduleRebuildForTest();

        PresetSection citiesAfter = findSection(panel, "Cities");
        assertNotNull("Cities section still renders after rebuild", citiesAfter);
        assertTrue("Cities should remain expanded across rebuild",
            citiesAfter.isExpanded());

        PresetSection bossesAfter = findSection(panel, "Bosses");
        assertFalse("Bosses (not expanded before) should remain collapsed",
            bossesAfter.isExpanded());
    }

    @Test
    public void disposeReleasesStoreSubscription()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        int before = store.listenerCountForTest();
        PresetBrowserPanel panel = buildPanelWithStore(store);
        int afterConstruct = store.listenerCountForTest();
        assertEquals("constructor must add exactly one listener",
            before + 1, afterConstruct);

        panel.dispose();
        assertEquals("dispose must release the listener",
            before, store.listenerCountForTest());
    }

    private static PresetBrowserPanel buildPanel()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        return buildPanelWithStore(store);
    }

    private static PresetBrowserPanel buildPanelWithStore(WaypointStore store)
    {
        PresetCatalog catalog = mock(PresetCatalog.class);
        when(catalog.getPresets()).thenReturn(Collections.emptyList());
        return new PresetBrowserPanel(catalog, store, mock(SpriteManager.class));
    }

    private static PresetSection findSection(PresetBrowserPanel panel, String name)
    {
        return findSectionIn(panel, name);
    }

    private static PresetSection findSectionIn(Component c, String name)
    {
        if (c instanceof PresetSection)
        {
            PresetSection s = (PresetSection) c;
            if (name.equals(s.getPresetName())) return s;
        }
        if (c instanceof java.awt.Container)
        {
            for (Component child : ((java.awt.Container) c).getComponents())
            {
                PresetSection found = findSectionIn(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }
}
