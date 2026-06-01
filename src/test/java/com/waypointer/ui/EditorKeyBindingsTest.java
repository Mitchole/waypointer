package com.waypointer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EditorKeyBindingsTest
{
    @Test
    public void enterOnFieldClicksPrimaryEscapeOnPanelClicksCancel()
    {
        JPanel panel = new JPanel();
        JTextField field = new JTextField();
        JButton primary = new JButton();
        JButton cancel = new JButton();
        int[] hits = {0, 0};
        primary.addActionListener(e -> hits[0]++);
        cancel.addActionListener(e -> hits[1]++);

        EditorKeyBindings.commitOnEnterCancelOnEscape(panel, field, primary, cancel);

        // The Enter keystroke is registered on the field's WHEN_FOCUSED input map, mapped to the
        // "editorCommit" action, which clicks the primary button.
        assertEquals("editorCommit", field.getInputMap(JComponent.WHEN_FOCUSED)
            .get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)));
        assertNotNull(field.getActionMap().get("editorCommit"));
        field.getActionMap().get("editorCommit")
            .actionPerformed(new ActionEvent(field, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals(1, hits[0]);
        assertEquals(0, hits[1]);

        // The Escape keystroke is registered on the panel's WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        // input map, mapped to the "editorCancel" action, which clicks the cancel button.
        assertEquals("editorCancel", panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));
        assertNotNull(panel.getActionMap().get("editorCancel"));
        panel.getActionMap().get("editorCancel")
            .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, ""));
        assertEquals(1, hits[0]);
        assertEquals(1, hits[1]);
    }
}
