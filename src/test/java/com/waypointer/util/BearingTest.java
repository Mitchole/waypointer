package com.waypointer.util;

import static org.junit.Assert.assertEquals;
import com.waypointer.model.WorldPointPacker;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class BearingTest
{
    private static final WorldPoint FROM = new WorldPoint(100, 100, 0);

    private static String to(int x, int y)
    {
        return Bearing.describe(FROM, WorldPointPacker.pack(x, y, 0));
    }

    @Test
    public void compassPoints()
    {
        assertEquals("14 tiles N", to(100, 114));
        assertEquals("10 tiles NE", to(110, 110));
        assertEquals("14 tiles E", to(114, 100));
        assertEquals("10 tiles SW", to(90, 90));
        assertEquals("14 tiles S", to(100, 86));
    }

    @Test
    public void oneTileAndHere()
    {
        assertEquals("1 tile N", to(100, 101));
        assertEquals("here", to(100, 100));
    }
}
