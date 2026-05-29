package com.waypointer.preset;

import com.waypointer.service.PresetOverridesSnapshot;
import com.waypointer.service.PresetOverridesSnapshot.CategoryOverride;
import com.waypointer.service.PresetOverridesSnapshot.DeletedWaypoint;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PresetCatalogOverrideMergeTest
{
    private PresetCatalog catalog(List<Preset> bundled)
    {
        return PresetCatalog.forTesting(bundled);
    }

    @Test
    public void byCategoryOverrideReplacesBundledList()
    {
        Preset bundled = new Preset("Bosses", "Bundled desc", null,
            Arrays.asList(new PresetWaypoint("OldVork", "", 1, 1, 0)));
        PresetCatalog c = catalog(Arrays.asList(bundled));

        Map<String, CategoryOverride> by = new LinkedHashMap<>();
        by.put("Bosses", new CategoryOverride("Bosses", "Overridden", null,
            Arrays.asList(new Waypoint("NewVork", "", 5, 5, 0))));
        c.applyOverrides(new PresetOverridesSnapshot(1, by, new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>()));

        Preset out = findCat(c, "Bosses");
        assertEquals(1, out.getWaypoints().size());
        assertEquals("NewVork", out.getWaypoints().get(0).getName());
    }

    @Test
    public void deletedWaypointsRemovesFromBundledCategoryNotInByCategory()
    {
        Preset bundled = new Preset("Skilling", null, null,
            Arrays.asList(
                new PresetWaypoint("Keep", "", 1, 1, 0),
                new PresetWaypoint("Drop", "", 2, 2, 0)));
        PresetCatalog c = catalog(Arrays.asList(bundled));
        c.applyOverrides(new PresetOverridesSnapshot(1,
            new LinkedHashMap<>(), new ArrayList<>(), new ArrayList<>(),
            Arrays.asList(new DeletedWaypoint("Skilling", "Drop", 2, 2, 0))));
        Preset out = findCat(c, "Skilling");
        assertEquals(1, out.getWaypoints().size());
        assertEquals("Keep", out.getWaypoints().get(0).getName());
    }

    @Test
    public void deletedCategoriesRemovesEntireCategory()
    {
        Preset bundled = new Preset("ToDrop", null, null, new ArrayList<>());
        PresetCatalog c = catalog(Arrays.asList(bundled));
        c.applyOverrides(new PresetOverridesSnapshot(1,
            new LinkedHashMap<>(), new ArrayList<>(),
            Arrays.asList("ToDrop"), new ArrayList<>()));
        assertNull(findCat(c, "ToDrop"));
    }

    @Test
    public void addedCategoriesAppearInOutput()
    {
        PresetCatalog c = catalog(new ArrayList<>());
        c.applyOverrides(new PresetOverridesSnapshot(1, new LinkedHashMap<>(),
            Arrays.asList(new CategoryOverride("NewCat", "", null,
                Arrays.asList(new Waypoint("X", "", 1, 1, 0)))),
            new ArrayList<>(), new ArrayList<>()));
        assertNotNull(findCat(c, "NewCat"));
    }

    @Test
    public void emptySnapshotIsPassThrough()
    {
        Preset bundled = new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("V", "", 1, 1, 0)));
        PresetCatalog c = catalog(Arrays.asList(bundled));
        c.applyOverrides(PresetOverridesSnapshot.empty());
        assertEquals(1, findCat(c, "Bosses").getWaypoints().size());
    }

    private static Preset findCat(PresetCatalog c, String name)
    {
        for (Preset p : c.getPresets())
        {
            if (name.equals(p.getCategory())) return p;
        }
        return null;
    }
}
