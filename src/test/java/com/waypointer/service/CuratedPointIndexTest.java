package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

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
        assertNotNull(idx.lookup(packed));
    }

    @Test
    public void bankNameEndsWithBankSuffix()
    {
        CuratedPointIndex idx = new CuratedPointIndex();
        int packed = WorldPointPacker.pack(3208, 3220, 2); // Lumbridge bank
        String name = idx.lookup(packed);
        assertNotNull(name);
        assertTrue("expected name to end with ' Bank', got: " + name, name.endsWith(" Bank"));
    }

    @Test
    public void anvilWithBlankNameUsesSuffixOnly()
    {
        CuratedPointIndex idx = new CuratedPointIndex();
        int packed = WorldPointPacker.pack(1514, 2995, 0);
        assertEquals("Anvil", idx.lookup(packed));
    }

    @Test
    public void returnsNullForUnknownTile()
    {
        CuratedPointIndex idx = new CuratedPointIndex();
        int packed = WorldPointPacker.pack(1, 1, 0);
        assertNull(idx.lookup(packed));
    }
}
