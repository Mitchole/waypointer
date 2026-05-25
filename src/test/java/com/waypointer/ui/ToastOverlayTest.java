package com.waypointer.ui;

import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ToastOverlayTest
{
    @Test
    public void wrapsContentAndStartsWithCardHidden()
    {
        JLabel content = new JLabel("inner");
        ToastOverlay overlay = new ToastOverlay(content);

        // The wrapped content is a direct child of the overlay (in some layer).
        boolean found = false;
        for (java.awt.Component c : overlay.getComponents())
        {
            if (c == content) found = true;
        }
        assertTrue("expected content to be a child of the overlay", found);

        // No card is visible until show() is called.
        assertFalse("toast card should be hidden initially", overlay.cardIsVisibleForTest());
    }
}
