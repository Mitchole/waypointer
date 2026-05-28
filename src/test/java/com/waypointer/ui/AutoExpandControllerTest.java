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
}
