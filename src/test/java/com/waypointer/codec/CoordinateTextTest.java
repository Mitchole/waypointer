package com.waypointer.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import com.waypointer.model.WorldPointPacker;
import org.junit.Test;

public class CoordinateTextTest
{
    private static final String FORMAT_MSG = "Enter coordinates as x, y or x, y, plane.";
    private static final String RANGE_MSG = "x and y must be between 0 and 16383.";
    private static final String PLANE_MSG = "Plane must be between 0 and 3.";

    @Test
    public void parsesCommaAndSpaceForms()
    {
        int plane0 = WorldPointPacker.pack(2301, 3690, 0);
        assertEquals(plane0, CoordinateText.parse("2301, 3690"));
        assertEquals(plane0, CoordinateText.parse("2301 3690"));
        assertEquals(plane0, CoordinateText.parse("2301\t3690"));
        assertEquals(WorldPointPacker.pack(2301, 3690, 1), CoordinateText.parse("2301,3690,1"));
        assertEquals(WorldPointPacker.pack(2301, 3690, 1), CoordinateText.parse(" 2301  3690  1 "));
    }

    @Test
    public void formatRoundTrips()
    {
        assertEquals("2301, 3690", CoordinateText.format(CoordinateText.parse("2301,3690")));
        assertEquals("2301, 3690, 1", CoordinateText.format(CoordinateText.parse("2301,3690,1")));
    }

    @Test
    public void rejectsBadInput()
    {
        assertRejected("", FORMAT_MSG);
        assertRejected("2301", FORMAT_MSG);
        assertRejected("a, b", FORMAT_MSG);
        assertRejected("1,2,3,4", FORMAT_MSG);
        assertRejected("-1, 5", RANGE_MSG);
        assertRejected("16384, 5", RANGE_MSG);
        assertRejected("99999999999, 5", RANGE_MSG);
        assertRejected("5, 5, 4", PLANE_MSG);
    }

    private static void assertRejected(String text, String message)
    {
        try
        {
            CoordinateText.parse(text);
            fail("expected rejection of '" + text + "'");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals(message, e.getMessage());
        }
    }
}
