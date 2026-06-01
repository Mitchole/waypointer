package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;

public class CollapsibleSectionTest
{
    // Minimal concrete section: a chevron + fixed title, two collapse-change notifications
    // captured so the test can assert toggle wiring.
    private static final class TestSection extends CollapsibleSection
    {
        private final List<Boolean> changes;

        TestSection(boolean collapsed, List<Boolean> changes)
        {
            super(collapsed, changes::add);
            this.changes = changes;
            JPanel headerRow = buildHeaderRow(2);
            add(headerRow, BorderLayout.NORTH);
            attachBody();
        }

        @Override
        protected String headerText()
        {
            return (collapsed ? "▶" : "▼") + " Test";
        }

        JLabel header() { return headerLabel; }
        JPanel bodyPanel() { return body; }
    }

    @Test
    public void startsExpandedShowsBodyAndDownChevron()
    {
        TestSection s = new TestSection(false, new ArrayList<>());
        assertTrue(s.bodyPanel().isVisible());
        assertEquals("▼ Test", s.header().getText());
        assertFalse(s.isCollapsed());
    }

    @Test
    public void clickingHeaderTogglesCollapseAndFiresCallback()
    {
        List<Boolean> changes = new ArrayList<>();
        TestSection s = new TestSection(false, changes);
        clickFirstListener(s.header());
        assertTrue(s.isCollapsed());
        assertFalse(s.bodyPanel().isVisible());
        assertEquals("▶ Test", s.header().getText());
        assertEquals(1, changes.size());
        assertEquals(Boolean.TRUE, changes.get(0));
    }

    @Test
    public void setExpandedTransientDoesNotFireCallback()
    {
        List<Boolean> changes = new ArrayList<>();
        TestSection s = new TestSection(true, changes);
        s.setExpandedTransient(true);
        assertFalse(s.isCollapsed());
        assertTrue(s.bodyPanel().isVisible());
        assertTrue("transient expand must not persist", changes.isEmpty());
        s.confirmTransientExpand();
        assertEquals(1, changes.size());
        assertEquals(Boolean.FALSE, changes.get(0));
    }

    @Test
    public void maximumSizeIsHeightCapped()
    {
        TestSection s = new TestSection(false, new ArrayList<>());
        assertEquals(Integer.MAX_VALUE, s.getMaximumSize().width);
        assertEquals(s.getPreferredSize().height, s.getMaximumSize().height);
    }

    private static void clickFirstListener(JLabel target)
    {
        MouseEvent click = new MouseEvent(target, MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(), 0, 5, 5, 1, false);
        for (MouseListener ml : target.getMouseListeners())
        {
            ml.mouseClicked(click);
        }
    }
}
