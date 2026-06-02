package com.waypointer.ui;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class LandmarkCaptureControlsTest
{
    @Test
    public void pointModeCollapsesToSingleTile()
    {
        LandmarkCaptureControls c = new LandmarkCaptureControls("n", false, 5);
        assertArrayEquals(new int[] {100, 200, 100, 200}, c.cornersFor(100, 200));
    }

    @Test
    public void areaModeExpandsBySizeMinusOne()
    {
        LandmarkCaptureControls c = new LandmarkCaptureControls("n", true, 3);
        assertArrayEquals(new int[] {100, 200, 102, 202}, c.cornersFor(100, 200));
        assertEquals(3, c.getSize());
        assertEquals(true, c.isArea());
        assertEquals("n", c.getName());
    }

    @Test
    public void sizeClampsToLowerBound()
    {
        assertEquals(2, new LandmarkCaptureControls("n", true, 1).getSize());
    }

    @Test
    public void sizeClampsToUpperBound()
    {
        assertEquals(10, new LandmarkCaptureControls("n", true, 15).getSize());
    }
}
