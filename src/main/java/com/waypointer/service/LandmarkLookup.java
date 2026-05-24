package com.waypointer.service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Composer over the two landmark indices. Lookups consult the curated index first (specific
 * names like "Lumbridge Bank") and fall back to the cache index (nearest-neighbour by
 * textScale-aware radius).
 */
@Singleton
public class LandmarkLookup
{
    private final CuratedPointIndex curated;
    private final CacheLabelIndex cache;

    @Inject
    public LandmarkLookup(CuratedPointIndex curated, CacheLabelIndex cache)
    {
        this.curated = curated;
        this.cache = cache;
    }

    @Nullable
    public LookupHit lookup(int packedPoint)
    {
        String curatedName = curated.lookup(packedPoint);
        if (curatedName != null)
        {
            return new LookupHit(curatedName, LookupHit.Tier.CURATED);
        }
        return cache.lookup(packedPoint);
    }
}
