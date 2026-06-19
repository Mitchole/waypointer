package com.waypointer.sailing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.waypointer.model.WorldPointPacker;
import org.junit.Test;

/** Locks the rule the PLAY branch uses: sail only when enabled AND the target tile is water. */
public class SailHookDecisionTest
{
    private final WaterMap waterMap = new WaterMap();

    private boolean shouldSail(boolean enabled, int packed)
    {
        return enabled && waterMap.isWater(WorldPointPacker.unpack(packed));
    }

    @Test
    public void waterTargetWithFeatureOnSails()
    {
        assertTrue(shouldSail(true, WorldPointPacker.pack(3051, 3195, 0)));
    }

    @Test
    public void waterTargetWithFeatureOffDoesNot()
    {
        assertFalse(shouldSail(false, WorldPointPacker.pack(3051, 3195, 0)));
    }

    @Test
    public void landTargetDoesNotSail()
    {
        assertFalse(shouldSail(true, WorldPointPacker.pack(3222, 3218, 0)));
    }
}
