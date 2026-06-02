package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class DialogsTest
{
    @Before
    public void requireDisplay()
    {
        // JDialog construction throws HeadlessException with no display; skip rather than fail.
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
    }

    @Test
    public void appliesDarkContentPaneWithDefaultGap()
    {
        JDialog d = new JDialog((Window) null);
        JPanel content = Dialogs.applyDarkContentPane(d);

        assertEquals(content, d.getContentPane());
        assertEquals(ColorScheme.DARK_GRAY_COLOR, content.getBackground());
        assertEquals(WindowConstants.DISPOSE_ON_CLOSE, d.getDefaultCloseOperation());

        BorderLayout layout = (BorderLayout) content.getLayout();
        assertEquals(8, layout.getHgap());
        assertEquals(8, layout.getVgap());

        Border border = content.getBorder();
        assertTrue(border instanceof EmptyBorder);
        Insets in = ((EmptyBorder) border).getBorderInsets();
        assertEquals(new Insets(8, 8, 8, 8), in);
    }

    @Test
    public void gapOverloadIsHonored()
    {
        JDialog d = new JDialog((Window) null);
        JPanel content = Dialogs.applyDarkContentPane(d, 0, 6);

        BorderLayout layout = (BorderLayout) content.getLayout();
        assertEquals(0, layout.getHgap());
        assertEquals(6, layout.getVgap());
    }

    @Test
    public void bindEscapeRegistersAction()
    {
        JDialog d = new JDialog((Window) null);
        Dialogs.bindEscape(d);

        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        Object actionKey = d.getRootPane()
            .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(esc);
        assertNotNull("ESC should map to an action key", actionKey);
        assertNotNull("action key should resolve to an action",
            d.getRootPane().getActionMap().get(actionKey));
    }
}
