package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        assertEquals("Edgeville Bank", idx.lookup(WorldPointPacker.pack(3094, 3493, 0)));
    }

    @Test
    public void entryComputesAreaInclusive()
    {
        BboxIndex.Entry e = new BboxIndex.Entry(0, 0, 4, 4, 0, "x");
        assertTrue(e.area == 25); // (4-0+1) * (4-0+1)
    }

    @Test
    public void tileInsideBboxReturnsName()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));

        assertEquals("Edgeville Bank", idx.lookup(WorldPointPacker.pack(3094, 3493, 0)));
    }

    @Test
    public void tileOutsideBboxReturnsNull()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));

        assertNull(idx.lookup(WorldPointPacker.pack(3200, 3200, 0)));
    }

    @Test
    public void smallestAreaWinsOnOverlap()
    {
        // Outer 100x100 polygon, inner 5x5 polygon, point inside both.
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3000, 3000, 3099, 3099, 0, "Big Area"),
            new BboxIndex.Entry(3050, 3050, 3054, 3054, 0, "Small POI")));

        assertEquals("Small POI", idx.lookup(WorldPointPacker.pack(3052, 3052, 0)));
    }

    @Test
    public void ignoresOtherPlanes()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));

        assertNull(idx.lookup(WorldPointPacker.pack(3094, 3493, 1)));
    }

    @Test
    public void degenerateOneByOneBboxMatchesExactTile()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(2412, 4434, 2412, 4434, 0, "Zanaris Fairy Ring")));

        assertEquals("Zanaris Fairy Ring", idx.lookup(WorldPointPacker.pack(2412, 4434, 0)));
        assertNull(idx.lookup(WorldPointPacker.pack(2413, 4434, 0)));
    }

    @Test
    public void bundledTsvsCoverEdgevilleBank()
    {
        BboxIndex idx = new BboxIndex();
        String name = idx.lookup(WorldPointPacker.pack(3094, 3493, 0));
        assertEquals("Edgeville Bank", name);
    }

    @Test
    public void bundledTsvsCoverGrandExchangePlaza()
    {
        BboxIndex idx = new BboxIndex();
        String name = idx.lookup(WorldPointPacker.pack(3163, 3490, 0));
        assertNotNull(name);
        assertTrue("expected GE-area bbox, got " + name, name.toLowerCase().contains("grand exchange"));
    }
}
