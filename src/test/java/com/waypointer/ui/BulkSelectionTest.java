package com.waypointer.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BulkSelectionTest
{
    @Test
    public void toggleAddsThenRemoves()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID();
        assertTrue(s.isEmpty());
        s.toggle(a);
        assertTrue(s.ids().contains(a));
        assertEquals(1, s.size());
        s.toggle(a);
        assertFalse(s.ids().contains(a));
        assertTrue(s.isEmpty());
    }

    @Test
    public void selectRangeForwardIsInclusiveAndAdditive()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        List<UUID> ordered = Arrays.asList(a, b, c, d);
        s.selectRange(ordered, b, d);
        assertTrue(s.ids().contains(b));
        assertTrue(s.ids().contains(c));
        assertTrue(s.ids().contains(d));
        assertFalse(s.ids().contains(a));
    }

    @Test
    public void selectRangeBackwardAnchorWorks()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        List<UUID> ordered = Arrays.asList(a, b, c);
        s.selectRange(ordered, c, a); // anchor after target
        assertEquals(3, s.size());
    }

    @Test
    public void selectRangeWithMissingAnchorIsNoOp()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        s.selectRange(Arrays.asList(a, b), UUID.randomUUID(), b);
        assertTrue(s.isEmpty());
    }

    @Test
    public void setCategorySelectsAndDeselects()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        List<UUID> cat = Arrays.asList(a, b);
        s.setCategory(cat, true);
        assertEquals(2, s.size());
        s.setCategory(cat, false);
        assertTrue(s.isEmpty());
    }

    @Test
    public void categoryStateTriState()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        List<UUID> cat = Arrays.asList(a, b);
        assertEquals(BulkSelection.TriState.NONE, s.categoryState(cat));
        s.toggle(a);
        assertEquals(BulkSelection.TriState.PARTIAL, s.categoryState(cat));
        s.toggle(b);
        assertEquals(BulkSelection.TriState.ALL, s.categoryState(cat));
    }

    @Test
    public void emptyCategoryStateIsNone()
    {
        BulkSelection s = new BulkSelection();
        assertEquals(BulkSelection.TriState.NONE, s.categoryState(Collections.emptyList()));
    }

    @Test
    public void clearEmptiesSelection()
    {
        BulkSelection s = new BulkSelection();
        s.toggle(UUID.randomUUID());
        s.clear();
        assertTrue(s.isEmpty());
    }

    @Test
    public void selectionIsIdKeyedAndSurvivesADifferentVisibleList()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID();
        s.toggle(a);
        // A later visible list that does not even contain 'a'; id-keyed selection must stand
        // and the range adds on top of it.
        UUID x = UUID.randomUUID(), y = UUID.randomUUID();
        s.selectRange(Arrays.asList(x, y), x, y);
        assertTrue(s.ids().contains(a));
        assertEquals(3, s.size());
    }

    @Test
    public void idsPreservesInsertionOrder()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        s.toggle(b);
        s.toggle(a);
        s.toggle(c);
        assertEquals(Arrays.asList(b, a, c), new java.util.ArrayList<>(s.ids()));
    }

    @Test
    public void idsReturnsACopy()
    {
        BulkSelection s = new BulkSelection();
        UUID a = UUID.randomUUID();
        s.toggle(a);
        s.ids().clear(); // mutating the returned set must not affect internal state
        assertTrue(s.ids().contains(a));
    }
}
