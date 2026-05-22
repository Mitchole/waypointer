package com.waypointer.preset;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointStore;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PresetImportTest
{
    private static Preset preset(String category, PresetWaypoint... wps)
    {
        return new Preset(category, "set description", 99, Arrays.asList(wps));
    }

    @Test
    public void singleEntryLibraryMapsEveryField()
    {
        PresetWaypoint wp = new PresetWaypoint("GE", "central hub", 3164, 3486, 0);
        Library lib = PresetImport.singleEntryLibrary(preset("Banks", wp), wp);

        assertEquals(1, lib.getCategories().size());
        Category c = lib.getCategories().get(0);
        assertEquals("Banks", c.getName());
        assertTrue("preset category must be flagged bundled", c.isBundled());
        assertEquals(Integer.valueOf(99), c.getIconId());

        assertEquals(1, lib.getWaypoints().size());
        Waypoint w = lib.getWaypoints().get(0);
        assertEquals("GE", w.getName());
        assertEquals("central hub", w.getNotes());
        assertEquals(c.getId(), w.getCategoryId());
        assertEquals(WorldPointPacker.pack(3164, 3486, 0), w.getPackedWorldPoint());
    }

    @Test
    public void nullDescriptionBecomesEmptyNotes()
    {
        PresetWaypoint wp = new PresetWaypoint("Spot", null, 3000, 3000, 0);
        Library lib = PresetImport.singleEntryLibrary(preset("Cat", wp), wp);
        assertEquals("", lib.getWaypoints().get(0).getNotes());
    }

    @Test
    public void addingThroughStoreCreatesThenMergesByCategoryName()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());

        PresetWaypoint a = new PresetWaypoint("GE", "", 3164, 3486, 0);
        PresetWaypoint b = new PresetWaypoint("Falador", "", 2945, 3368, 0);
        store.importMerge(PresetImport.singleEntryLibrary(preset("Banks", a), a));
        store.importMerge(PresetImport.singleEntryLibrary(preset("Banks", b), b));

        Category banks = store.getCategoryByName("Banks");
        assertNotNull("Banks category created by the first add", banks);
        assertEquals("second add merges into the existing category",
            2, store.getWaypointsInCategory(banks.getId()).size());
    }
}
