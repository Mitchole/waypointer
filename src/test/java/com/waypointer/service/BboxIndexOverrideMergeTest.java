package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.LandmarkOverridesSnapshot.DeletedEntry;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BboxIndexOverrideMergeTest
{
    @Test
    public void overrideReplacesBundledTypeEntirely()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(1, 1, 1, 1, 0, "Original bank", LandmarkType.BANK)));
        Map<String, TypeOverride> by = new LinkedHashMap<>();
        by.put("BANK", new TypeOverride(Arrays.asList(
            new Entry("Override bank", 5, 5, 5, 5, 0))));
        LandmarkOverridesSnapshot s = new LandmarkOverridesSnapshot(1, by, new ArrayList<>());

        idx.applyOverrides(s);

        BboxIndex.Hit h = idx.nearest(LandmarkType.BANK, WorldPointPacker.pack(5, 5, 0));
        assertEquals("Override bank", h.name);
        assertNull("original bank should be gone",
            idx.lookup(WorldPointPacker.pack(1, 1, 0)));
    }

    @Test
    public void deletionsRemoveBundledEntriesNotInByType()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(1, 1, 1, 1, 0, "Keep", LandmarkType.BANK),
            new BboxIndex.Entry(3, 3, 3, 3, 0, "Drop", LandmarkType.BANK)));
        LandmarkOverridesSnapshot s = new LandmarkOverridesSnapshot(1,
            new LinkedHashMap<>(),
            new ArrayList<>(Arrays.asList(new DeletedEntry("BANK", "Drop", 3, 3, 3, 3, 0))));

        idx.applyOverrides(s);

        assertEquals("Keep", idx.lookup(WorldPointPacker.pack(1, 1, 0)));
        assertNull(idx.lookup(WorldPointPacker.pack(3, 3, 0)));
    }

    @Test
    public void emptySnapshotIsPassThrough()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(1, 1, 1, 1, 0, "Bundled", LandmarkType.BANK)));
        idx.applyOverrides(LandmarkOverridesSnapshot.empty());
        assertEquals("Bundled", idx.lookup(WorldPointPacker.pack(1, 1, 0)));
    }
}
