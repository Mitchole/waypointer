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
}
