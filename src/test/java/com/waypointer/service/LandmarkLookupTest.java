package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LandmarkLookupTest
{
    @Test
    public void bboxHitWinsAndIsTaggedCurated()
    {
        BboxIndex bbox = mock(BboxIndex.class);
        CacheLabelIndex cache = mock(CacheLabelIndex.class);
        int packed = WorldPointPacker.pack(3094, 3493, 0);
        when(bbox.lookup(packed)).thenReturn("Edgeville Bank");

        LookupHit h = new LandmarkLookup(bbox, cache).lookup(packed);

        assertNotNull(h);
        assertEquals("Edgeville Bank", h.getName());
        assertEquals(LookupHit.Tier.CURATED, h.getTier());
    }

    @Test
    public void cacheHitWinsWhenBboxMisses()
    {
        BboxIndex bbox = mock(BboxIndex.class);
        CacheLabelIndex cache = mock(CacheLabelIndex.class);
        int packed = WorldPointPacker.pack(3210, 3424, 0);
        when(bbox.lookup(packed)).thenReturn(null);
        when(cache.lookup(packed)).thenReturn(new LookupHit("Varrock", LookupHit.Tier.CITY));

        LookupHit h = new LandmarkLookup(bbox, cache).lookup(packed);

        assertNotNull(h);
        assertEquals("Varrock", h.getName());
        assertEquals(LookupHit.Tier.CITY, h.getTier());
    }

    @Test
    public void returnsNullWhenBothIndicesMiss()
    {
        BboxIndex bbox = mock(BboxIndex.class);
        CacheLabelIndex cache = mock(CacheLabelIndex.class);
        int packed = WorldPointPacker.pack(1, 1, 0);
        when(bbox.lookup(packed)).thenReturn(null);
        when(cache.lookup(packed)).thenReturn(null);

        assertNull(new LandmarkLookup(bbox, cache).lookup(packed));
    }
}
