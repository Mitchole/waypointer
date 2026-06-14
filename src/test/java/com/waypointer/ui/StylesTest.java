package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StylesTest
{
    @Test
    public void editableMetaRow_placesNameWestDetailCenterButtonsEast()
    {
        JPanel row = new JPanel();
        Styles.editableMetaRow(row, "Vorkath", "(1, 2) p0", () -> {}, () -> {}, () -> {});

        BorderLayout layout = (BorderLayout) row.getLayout();
        assertEquals("Vorkath",
            ((JLabel) layout.getLayoutComponent(BorderLayout.WEST)).getText());
        assertEquals("(1, 2) p0",
            ((JLabel) layout.getLayoutComponent(BorderLayout.CENTER)).getText());
        JPanel east = (JPanel) layout.getLayoutComponent(BorderLayout.EAST);
        assertEquals(3, east.getComponentCount());
        assertEquals("Go",     ((JButton) east.getComponent(0)).getText());
        assertEquals("Edit",   ((JButton) east.getComponent(1)).getText());
        assertEquals("Delete", ((JButton) east.getComponent(2)).getText());
    }

    @Test
    public void editableMetaRow_buttonsInvokeSuppliedCallbacks()
    {
        JPanel row = new JPanel();
        boolean[] fired = new boolean[3];
        Styles.editableMetaRow(row, "n", "d",
            () -> fired[0] = true, () -> fired[1] = true, () -> fired[2] = true);
        JPanel east = (JPanel) ((BorderLayout) row.getLayout())
            .getLayoutComponent(BorderLayout.EAST);
        ((JButton) east.getComponent(0)).doClick();
        ((JButton) east.getComponent(1)).doClick();
        ((JButton) east.getComponent(2)).doClick();
        assertTrue(fired[0]);
        assertTrue(fired[1]);
        assertTrue(fired[2]);
    }

    @Test
    public void errorLabel_isHiddenBlankAndRed()
    {
        JLabel label = Styles.errorLabel();
        assertFalse("error label starts hidden", label.isVisible());
        assertEquals(" ", label.getText());
        assertEquals(Styles.ERROR_RED, label.getForeground());
    }
}
