package com.waypointer.sailing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
}
