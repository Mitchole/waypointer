package com.waypointer.ui;

import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutoExpandControllerTest
{
    @Test
    public void recordAndIsTransientlyExpanded()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        assertFalse(c.isTransientlyExpanded(a));
        c.recordTransientExpand(a);
        assertTrue(c.isTransientlyExpanded(a));
    }

    @Test
    public void onHoverInsideAutoExpandedDoesNotRevertIt()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        c.recordTransientExpand(a);
        Set<UUID> reverted = c.onHover(a);
        assertTrue(reverted.isEmpty());
        assertTrue(c.isTransientlyExpanded(a));
    }

    @Test
    public void onHoverOutsideAutoExpandedRevertsIt()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        c.recordTransientExpand(a);
        Set<UUID> reverted = c.onHover(other);
        assertEquals(java.util.Collections.singleton(a), reverted);
        assertFalse(c.isTransientlyExpanded(a));
    }

    @Test
    public void onHoverWithMultipleAutoExpandedRevertsOnlyOutsideOnes()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        c.recordTransientExpand(a);
        c.recordTransientExpand(b);
        Set<UUID> reverted = c.onHover(a);
        assertEquals(java.util.Collections.singleton(b), reverted);
        assertTrue(c.isTransientlyExpanded(a));
        assertFalse(c.isTransientlyExpanded(b));
    }

    @Test
    public void onDropAtAutoExpandedConfirmsIt()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        c.recordTransientExpand(a);
        AutoExpandController.DropResolution r = c.onDropAt(a);
        assertEquals(a, r.getToConfirm());
        assertTrue(r.getToRevert().isEmpty());
        assertFalse(c.isTransientlyExpanded(a));
    }

    @Test
    public void onDropAtAutoExpandedRevertsOthers()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        c.recordTransientExpand(a);
        c.recordTransientExpand(b);
        AutoExpandController.DropResolution r = c.onDropAt(a);
        assertEquals(a, r.getToConfirm());
        assertEquals(java.util.Collections.singleton(b), r.getToRevert());
        assertFalse(c.isTransientlyExpanded(a));
        assertFalse(c.isTransientlyExpanded(b));
    }

    @Test
    public void onDropAtNonAutoExpandedRevertsAll()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID dest = UUID.randomUUID();
        c.recordTransientExpand(a);
        c.recordTransientExpand(b);
        AutoExpandController.DropResolution r = c.onDropAt(dest);
        assertNull(r.getToConfirm());
        assertEquals(2, r.getToRevert().size());
        assertTrue(r.getToRevert().contains(a));
        assertTrue(r.getToRevert().contains(b));
    }

    @Test
    public void onDragEndReturnsAllAutoExpandedAndClears()
    {
        AutoExpandController c = new AutoExpandController();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        c.recordTransientExpand(a);
        c.recordTransientExpand(b);
        Set<UUID> reverted = c.onDragEnd();
        assertEquals(2, reverted.size());
        assertTrue(reverted.contains(a));
        assertTrue(reverted.contains(b));
        assertFalse(c.isTransientlyExpanded(a));
        assertFalse(c.isTransientlyExpanded(b));
    }

    @Test
    public void onDragEndWithNothingExpandedReturnsEmpty()
    {
        AutoExpandController c = new AutoExpandController();
        assertTrue(c.onDragEnd().isEmpty());
    }
}
