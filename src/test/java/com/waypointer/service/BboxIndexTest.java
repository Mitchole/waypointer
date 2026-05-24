package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BboxIndexTest
{
    @Test
    public void loadsBundledTsvs()
    {
        BboxIndex idx = new BboxIndex();
        // Stub: lookup returns null until Task 24. Just verify construction doesn't throw.
        assertNull(idx.lookup(WorldPointPacker.pack(0, 0, 0)));
    }

    @Test
    public void forTestingBuildsFromLiteralEntries()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));
        // Stub: returns null until Task 24.
        assertNull(idx.lookup(WorldPointPacker.pack(3094, 3493, 0)));
    }

    @Test
    public void entryComputesAreaInclusive()
    {
        BboxIndex.Entry e = new BboxIndex.Entry(0, 0, 4, 4, 0, "x");
        assertTrue(e.area == 25); // (4-0+1) * (4-0+1)
    }
}
