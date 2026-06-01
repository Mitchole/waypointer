package com.waypointer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Shared keyboard wiring for the inline editor panels: Enter on the name field triggers the
 * primary action, Escape anywhere in the panel cancels. Mirrors the bindings CaptureForm sets up
 * inline so the dev-tools and category editors don't each re-implement them. Enter is bound on the
 * single-line name field only, so editors with a multi-line description area keep normal newline
 * behaviour there.
 */
final class EditorKeyBindings
{
    private EditorKeyBindings()
    {
    }

    static void commitOnEnterCancelOnEscape(JPanel panel, JTextField commitField,
        JButton primary, JButton cancel)
    {
        commitField.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editorCommit");
        commitField.getActionMap().put("editorCommit", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e) { primary.doClick(); }
        });

        panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "editorCancel");
        panel.getActionMap().put("editorCancel", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e) { cancel.doClick(); }
        });
    }
}
