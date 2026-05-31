package com.waypointer.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class WaypointerPanelFooterTest
{
    @Test
    public void zeroCountsArePlural()
    {
        assertEquals("0 waypoints · 0 categories",
            WaypointerPanel.footerCountText(0, 0));
    }

    @Test
    public void singularCountsDropTheS()
    {
        assertEquals("1 waypoint · 1 category",
            WaypointerPanel.footerCountText(1, 1));
    }

    @Test
    public void pluralCountsAddTheS()
    {
        assertEquals("12 waypoints · 3 categories",
            WaypointerPanel.footerCountText(12, 3));
    }

    @Test
    public void mixedSingularPlural()
    {
        assertEquals("1 waypoint · 5 categories",
            WaypointerPanel.footerCountText(1, 5));
        assertEquals("5 waypoints · 1 category",
            WaypointerPanel.footerCountText(5, 1));
    }
}
