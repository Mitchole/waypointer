package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import org.junit.Test;
import static org.junit.Assert.assertNull;

public class CuratedPointIndexTest
{
    @Test
    public void returnsBundledBankName()
    {
        CuratedPointIndex idx = new CuratedPointIndex();
        // Lumbridge bank, taken from bank.tsv vendored from shortest-path.
        int packed = WorldPointPacker.pack(3208, 3220, 2);
        // Specific assertion deliberately avoids hardcoding the exact string,
        // because shortest-path's vendored data may change; the contract is
        // "non-null name for a known bank tile".
        org.junit.Assert.assertNotNull(idx.lookup(packed));
    }

    @Test
    public void returnsNullForUnknownTile()
    {
        CuratedPointIndex idx = new CuratedPointIndex();
        int packed = WorldPointPacker.pack(1, 1, 0);
        assertNull(idx.lookup(packed));
    }
}
