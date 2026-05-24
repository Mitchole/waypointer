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
    public void curatedHitWinsAndIsTaggedCurated()
    {
        CuratedPointIndex curated = mock(CuratedPointIndex.class);
        CacheLabelIndex cache = mock(CacheLabelIndex.class);
        int packed = WorldPointPacker.pack(3208, 3220, 0);
        when(curated.lookup(packed)).thenReturn("Lumbridge Bank");

        LandmarkLookup l = new LandmarkLookup(curated, cache);
        LookupHit h = l.lookup(packed);

        assertNotNull(h);
        assertEquals("Lumbridge Bank", h.getName());
        assertEquals(LookupHit.Tier.CURATED, h.getTier());
    }

    @Test
    public void cacheHitWinsWhenCuratedMisses()
    {
        CuratedPointIndex curated = mock(CuratedPointIndex.class);
        CacheLabelIndex cache = mock(CacheLabelIndex.class);
        int packed = WorldPointPacker.pack(3210, 3424, 0);
        when(curated.lookup(packed)).thenReturn(null);
        when(cache.lookup(packed)).thenReturn(new LookupHit("Varrock", LookupHit.Tier.CITY));

        LookupHit h = new LandmarkLookup(curated, cache).lookup(packed);

        assertNotNull(h);
        assertEquals("Varrock", h.getName());
        assertEquals(LookupHit.Tier.CITY, h.getTier());
    }

    @Test
    public void returnsNullWhenBothIndicesMiss()
    {
        CuratedPointIndex curated = mock(CuratedPointIndex.class);
        CacheLabelIndex cache = mock(CacheLabelIndex.class);
        int packed = WorldPointPacker.pack(1, 1, 0);
        when(curated.lookup(packed)).thenReturn(null);
        when(cache.lookup(packed)).thenReturn(null);

        assertNull(new LandmarkLookup(curated, cache).lookup(packed));
    }
}
