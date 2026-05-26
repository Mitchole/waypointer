package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import org.junit.Test;
import static org.junit.Assert.*;

public class WildernessTest
{
    private static int packed(int x, int y, int plane)
    {
        return WorldPointPacker.pack(x, y, plane);
    }

    @Test public void edgevilleBankIsNotWild()        { assertFalse(Wilderness.isInWilderness(packed(3093, 3493, 0))); }
    @Test public void firstTileNorthOfDitchIsWild()   { assertTrue (Wilderness.isInWilderness(packed(3093, 3525, 0))); }
    @Test public void safeDitchZoneIsNotWild()        { assertFalse(Wilderness.isInWilderness(packed(3093, 3522, 0))); }
    @Test public void surfaceNECornerInclusive()      { assertTrue (Wilderness.isInWilderness(packed(3392, 3967, 0))); }
    @Test public void revenantsCaveEntranceIsWild()   { assertTrue (Wilderness.isInWilderness(packed(3128, 10115, 0))); }
    @Test public void wildernessSlayerCaveIsWild()    { assertTrue (Wilderness.isInWilderness(packed(3055, 10000, 0))); }
    @Test public void mageArenaTwoIsNotWild()         { assertFalse(Wilderness.isInWilderness(packed(2538, 4717, 0))); }
    @Test public void varrockIsNotWild()              { assertFalse(Wilderness.isInWilderness(packed(3200, 3300, 0))); }
    @Test public void wildernessYOnPlaneOneIsNotWild() { assertFalse(Wilderness.isInWilderness(packed(3093, 3525, 1))); }
}
