package com.waypointer.ui;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class IconPickerDialogTest
{
    private static final List<Integer> TEN_IDS = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);

    @Test
    public void nullCurrentMapsToFirstPage()
    {
        assertEquals(0, IconPickerDialog.computeInitialPage(null, TEN_IDS, 5));
    }

    @Test
    public void firstPageEntries()
    {
        // Indices 0..4 with pageSize 5 → page 0
        assertEquals(0, IconPickerDialog.computeInitialPage(10, TEN_IDS, 5));
        assertEquals(0, IconPickerDialog.computeInitialPage(50, TEN_IDS, 5));
    }

    @Test
    public void secondPageEntries()
    {
        // Indices 5..9 → page 1
        assertEquals(1, IconPickerDialog.computeInitialPage(60, TEN_IDS, 5));
        assertEquals(1, IconPickerDialog.computeInitialPage(100, TEN_IDS, 5));
    }

    @Test
    public void unknownIdFallsBackToFirstPage()
    {
        assertEquals(0, IconPickerDialog.computeInitialPage(999, TEN_IDS, 5));
        assertEquals(0, IconPickerDialog.computeInitialPage(-1, TEN_IDS, 5));
    }

    @Test
    public void singleItemListIsPageZero()
    {
        assertEquals(0, IconPickerDialog.computeInitialPage(42, Arrays.asList(42), 50));
    }
}
