package com.waypointer.ui;

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
}
