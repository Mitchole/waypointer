package com.waypointer.sailing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class SailingDockTest
{
    @Test
    public void resolvesKnownIds()
    {
        assertEquals(SailingDock.PORT_SARIM, SailingDock.forId(0));
        assertEquals("Port Sarim", SailingDock.forId(0).getDockName());
        assertEquals(3050, SailingDock.forId(0).getNavigationLocation().getX());
        assertEquals(SailingDock.GRIMSTONE, SailingDock.forId(58));
    }

    @Test
    public void unknownAndSentinelIdsReturnNull()
    {
        assertNull(SailingDock.forId(254)); // Capsized sentinel
        assertNull(SailingDock.forId(255)); // Bottled sentinel
        assertNull(SailingDock.forId(999));
        assertNull(SailingDock.forId(-1));
    }

    @Test
    public void allFiftyNineDocksPresent()
    {
        assertEquals(59, SailingDock.values().length);
    }
}
