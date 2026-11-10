package com.waypointer.sailing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class WaterMapTest
{
    private final WaterMap map = new WaterMap(); // loads the real bundled resource

    @Test
    public void portSarimMooringIsWater()
    {
        // Verified against water-collision.txt chunk 381 399: tile bit 27 is open.
        assertTrue(map.isWater(3051, 3195));
    }

    @Test
    public void adjacentDockTileIsLand()
    {
        // Same chunk, tile bit 0 is blocked (the walkable shore the boat moors against).
        assertFalse(map.isWater(3048, 3192));
    }

    @Test
    public void inlandTileIsNotWater()
    {
        // Lumbridge is not in the baked dataset -> UNKNOWN -> not water.
        assertFalse(map.isWater(3222, 3218));
    }

    @Test
    public void wrongPlaneIsNotWater()
    {
        assertFalse(map.isWater(new net.runelite.api.coords.WorldPoint(3051, 3195, 1)));
    }

    @Test
    public void openWaterIsAtSea()
    {
        assertTrue(map.isAtSea(3051, 3195));
    }

    @Test
    public void dockSideLandStaysLand()
    {
        assertFalse(map.isAtSea(3048, 3192));
    }

    @Test
    public void inlandIsNotAtSea()
    {
        assertFalse(map.isAtSea(3222, 3218));
    }

    @Test
    public void blockedTileInOpenWaterIsAtSea()
    {
        assertTrue(fixture(49).isAtSea(1000, 1000));
    }

    @Test
    public void halfWaterCoastIsNotAtSea()
    {
        assertFalse(fixture(24).isAtSea(1000, 1000));
    }

    @Test
    public void thresholdIsThirtySevenOfFortyNine()
    {
        assertFalse(fixture(36).isAtSea(1000, 1000));
        assertTrue(fixture(37).isAtSea(1000, 1000));
    }

    /** Known area 990..1010 squared, all blocked except {@code open} tiles taken in row order
     *  from the 7x7 box around (1000, 1000). The centre is never open. */
    private static WaterMap fixture(int open)
    {
        Map<Long, WaterCollisionData.Chunk> chunks = new HashMap<>();
        int remaining = open;
        for (int x = 984; x < 1016; x++)
        {
            for (int y = 984; y < 1016; y++)
            {
                boolean inBox = Math.abs(x - 1000) <= 3 && Math.abs(y - 1000) <= 3;
                boolean isOpen = false;
                if (inBox && !(x == 1000 && y == 1000) && remaining > 0)
                {
                    isOpen = true;
                    remaining--;
                }
                long key = WaterCollisionData.key(x >> 3, y >> 3);
                WaterCollisionData.Chunk c = chunks.get(key);
                long known = c == null ? 0 : c.known;
                long blocked = c == null ? 0 : c.blocked;
                long bit = 1L << ((x & 7) + ((y & 7) << 3));
                known |= bit;
                if (!isOpen)
                {
                    blocked |= bit;
                }
                chunks.put(key, new WaterCollisionData.Chunk(known, blocked));
            }
        }
        return new WaterMap(new WaterCollisionData(chunks));
    }
}
