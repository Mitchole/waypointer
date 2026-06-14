package com.waypointer.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class FooterStripTest
{
    @Test
    public void zeroCountsArePlural()
    {
        assertEquals("0 waypoints · 0 categories", FooterStrip.countText(0, 0));
    }

    @Test
    public void singularCountsDropTheS()
    {
        assertEquals("1 waypoint · 1 category", FooterStrip.countText(1, 1));
    }

    @Test
    public void pluralCountsAddTheS()
    {
        assertEquals("12 waypoints · 3 categories", FooterStrip.countText(12, 3));
    }

    @Test
    public void mixedSingularPlural()
    {
        assertEquals("1 waypoint · 5 categories", FooterStrip.countText(1, 5));
        assertEquals("5 waypoints · 1 category", FooterStrip.countText(5, 1));
    }

    @Test
    public void filteredCountUsesShownOfTotal()
    {
        assertEquals("4 of 15 shown · 2 categories", FooterStrip.countTextFiltered(4, 15, 2));
    }

    @Test
    public void filteredCountSingularCategory()
    {
        assertEquals("4 of 15 shown · 1 category", FooterStrip.countTextFiltered(4, 15, 1));
    }

    @Test
    public void filteredCountZeroCategoriesStaysPlural()
    {
        assertEquals("0 of 15 shown · 0 categories", FooterStrip.countTextFiltered(0, 15, 0));
    }
}
