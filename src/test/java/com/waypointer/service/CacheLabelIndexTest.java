package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CacheLabelIndexTest
{
    @Test
    public void loadsAtLeastOneRowFromMapLabelsTsv()
    {
        CacheLabelIndex idx = new CacheLabelIndex();
        assertTrue(idx.size() > 0);
    }

    @Test
    public void poiHitWithinThreeTilesReturnsPoiTier()
    {
        CacheLabelIndex idx = CacheLabelIndex.forTesting(Arrays.asList(
            new CacheLabelIndex.Entry(WorldPointPacker.pack(3208, 3220, 0), "Bank", 7, 50, 0)));

        LookupHit h = idx.lookup(WorldPointPacker.pack(3210, 3221, 0));
        assertNotNull(h);
        assertEquals("Bank", h.getName());
        assertEquals(LookupHit.Tier.POI, h.getTier());
    }

    @Test
    public void poiOutOfRadiusReturnsNull()
    {
        CacheLabelIndex idx = CacheLabelIndex.forTesting(Arrays.asList(
            new CacheLabelIndex.Entry(WorldPointPacker.pack(3208, 3220, 0), "Bank", 7, 50, 0)));

        assertNull(idx.lookup(WorldPointPacker.pack(3215, 3220, 0))); // 7 tiles away, > 3
    }

    @Test
    public void cityHitWithinTwoHundredTilesReturnsCityTier()
    {
        CacheLabelIndex idx = CacheLabelIndex.forTesting(Arrays.asList(
            new CacheLabelIndex.Entry(WorldPointPacker.pack(3210, 3424, 0), "Varrock", 0, 200, 2)));

        LookupHit h = idx.lookup(WorldPointPacker.pack(3270, 3450, 0)); // 60 tiles away
        assertNotNull(h);
        assertEquals("Varrock", h.getName());
        assertEquals(LookupHit.Tier.CITY, h.getTier());
    }

    @Test
    public void poiBeatsCityWhenBothInRange()
    {
        CacheLabelIndex idx = CacheLabelIndex.forTesting(Arrays.asList(
            new CacheLabelIndex.Entry(WorldPointPacker.pack(3210, 3424, 0), "Varrock", 0, 200, 2),
            new CacheLabelIndex.Entry(WorldPointPacker.pack(3253, 3422, 0), "Bank", 7, 50, 0)));

        // Stand 2 tiles from the bank. POI tier wins.
        LookupHit h = idx.lookup(WorldPointPacker.pack(3254, 3422, 0));
        assertNotNull(h);
        assertEquals("Bank", h.getName());
        assertEquals(LookupHit.Tier.POI, h.getTier());
    }

    @Test
    public void ignoresOtherPlanes()
    {
        CacheLabelIndex idx = CacheLabelIndex.forTesting(Arrays.asList(
            new CacheLabelIndex.Entry(WorldPointPacker.pack(3208, 3220, 0), "Bank", 7, 50, 0)));

        assertNull(idx.lookup(WorldPointPacker.pack(3208, 3220, 1)));
    }
}
