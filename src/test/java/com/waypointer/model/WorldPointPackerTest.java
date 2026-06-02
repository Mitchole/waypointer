package com.waypointer.model;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldPointPackerTest
{
    @Test
    public void packAndUnpackRoundTrip()
    {
        WorldPoint wp = new WorldPoint(3200, 3200, 0);
        int packed = WorldPointPacker.pack(wp);
        WorldPoint back = WorldPointPacker.unpack(packed);
        assertEquals(wp.getX(), back.getX());
        assertEquals(wp.getY(), back.getY());
        assertEquals(wp.getPlane(), back.getPlane());
    }

    @Test
    public void packPreservesPlane()
    {
        WorldPoint wp = new WorldPoint(2500, 9800, 3);
        int packed = WorldPointPacker.pack(wp);
        assertEquals(2500, WorldPointPacker.getX(packed));
        assertEquals(9800, WorldPointPacker.getY(packed));
        assertEquals(3, WorldPointPacker.getPlane(packed));
    }

    @Test
    public void undefinedSentinelDistinguishable()
    {
        assertEquals(WorldPointPacker.UNDEFINED, WorldPointPacker.UNDEFINED);
        assertNotEquals(WorldPointPacker.UNDEFINED, WorldPointPacker.pack(new WorldPoint(0, 0, 0)));
    }

    @Test
    public void arrivedTrueWithinRadiusSamePlane()
    {
        int target = WorldPointPacker.pack(new WorldPoint(3200, 3200, 0));
        assertTrue(WorldPointPacker.arrived(target, new WorldPoint(3200, 3200, 0), 3));
        assertTrue(WorldPointPacker.arrived(target, new WorldPoint(3203, 3200, 0), 3));
    }

    @Test
    public void arrivedFalseOutsideRadius()
    {
        int target = WorldPointPacker.pack(new WorldPoint(3200, 3200, 0));
        assertFalse(WorldPointPacker.arrived(target, new WorldPoint(3203, 3203, 0), 3));
    }

    @Test
    public void arrivedFalseOnDifferentPlane()
    {
        int target = WorldPointPacker.pack(new WorldPoint(3200, 3200, 0));
        assertFalse(WorldPointPacker.arrived(target, new WorldPoint(3200, 3200, 1), 3));
    }
}
