package com.waypointer.service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Composer over the two landmark indices. Lookups consult the wiki-sourced bbox index first
 * (specific named POIs like "Edgeville Bank") and fall back to the cache-label index for
 * city / sub-area / region names.
 */
@Singleton
public class LandmarkLookup
{
    private final BboxIndex bbox;
    private final CacheLabelIndex cache;

    @Inject
    public LandmarkLookup(BboxIndex bbox, CacheLabelIndex cache)
    {
        this.bbox = bbox;
        this.cache = cache;
    }

    @Nullable
    public LookupHit lookup(int packedPoint)
    {
        String bboxName = bbox.lookup(packedPoint);
        if (bboxName != null)
        {
            return new LookupHit(bboxName, LookupHit.Tier.CURATED);
        }
        return cache.lookup(packedPoint);
    }
}
