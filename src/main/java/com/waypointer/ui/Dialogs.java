package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import net.runelite.client.ui.ColorScheme;

/**
 * Shared scaffolding for the plugin's modal dialogs: a dark content pane with the standard
 * border and gap, ESC-to-close, and pack/center. Keeps each dialog from re-deriving the same
 * dark-panel setup by hand.
 */
final class Dialogs
{
    private Dialogs()
    {
    }

    /** Dark content pane with the standard 8px gap and border. See the gap overload for exceptions. */
    static JPanel applyDarkContentPane(JDialog d)
    {
        return applyDarkContentPane(d, 8, 8);
    }

    /** Gap overload for dialogs that need tighter inner spacing than the 8px default. */
    static JPanel applyDarkContentPane(JDialog d, int hgap, int vgap)
    {
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel content = (JPanel) d.getContentPane();
        content.setLayout(new BorderLayout(hgap, vgap));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        return content;
    }

    /** Closes the dialog when ESC is pressed anywhere within it. */
    static void bindEscape(JDialog d)
    {
        JComponent root = d.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "waypointer.dialog.close");
        root.getActionMap().put("waypointer.dialog.close", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                d.dispose();
            }
        });
    }

    /** Sizes the dialog to its contents and centers it over the owner. */
    static void finish(JDialog d, Window owner)
    {
        d.pack();
        d.setLocationRelativeTo(owner);
    }
}
