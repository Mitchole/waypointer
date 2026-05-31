package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class AccessibleNamesTest
{
    @Test
    public void palettePopupItemsAreNamed()
    {
        javax.swing.JPopupMenu p = ColorPalettePopup.build(null, rgb -> {});
        javax.swing.JMenuItem first = (javax.swing.JMenuItem) p.getComponent(0);
        assertNotNull(first.getAccessibleContext().getAccessibleName());
        assertFalse(first.getAccessibleContext().getAccessibleName().isEmpty());
    }

    @Test
    public void waypointRowPlayButtonIsNamed()
    {
        Waypoint wp = new Waypoint(UUID.randomUUID(), "Bank", 0, UUID.randomUUID(), null,
            "", Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);
        // selectMode=false so the row builds its Play button, which the accessibility
        // sweep names "Path to <name>". SpriteManager may be null (icon is unset).
        WaypointRow row = new WaypointRow(wp, false, false, false, false,
            () -> {}, () -> {}, () -> {}, () -> {},
            null, null,
            false, false, sel -> {});
        assertTrue("expected a 'Path to ...' accessible name on a row control",
            hasAccessibleNameStartingWith(row, "Path to"));
    }

    private static boolean hasAccessibleNameStartingWith(java.awt.Component c, String prefix)
    {
        if (c instanceof javax.accessibility.Accessible)
        {
            javax.accessibility.AccessibleContext ctx = ((javax.accessibility.Accessible) c).getAccessibleContext();
            if (ctx != null)
            {
                String n = ctx.getAccessibleName();
                if (n != null && n.startsWith(prefix)) return true;
            }
        }
        if (c instanceof java.awt.Container)
        {
            for (java.awt.Component child : ((java.awt.Container) c).getComponents())
            {
                if (hasAccessibleNameStartingWith(child, prefix)) return true;
            }
        }
        return false;
    }
}
