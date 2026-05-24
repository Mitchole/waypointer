package com.waypointer.service;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LookupHitTest
{
    @Test
    public void carriesNameAndTier()
    {
        LookupHit h = new LookupHit("Lumbridge Bank", LookupHit.Tier.CURATED);
        assertEquals("Lumbridge Bank", h.getName());
        assertEquals(LookupHit.Tier.CURATED, h.getTier());
    }

    @Test
    public void equalityMatchesByValue()
    {
        LookupHit a = new LookupHit("Varrock", LookupHit.Tier.CITY);
        LookupHit b = new LookupHit("Varrock", LookupHit.Tier.CITY);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
