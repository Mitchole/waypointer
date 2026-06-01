package com.waypointer.ui;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
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

        assertNotNull(field.getActionMap().get("editorCommit"));
        field.getActionMap().get("editorCommit").actionPerformed(null);
        assertEquals(1, hits[0]);
        assertEquals(0, hits[1]);

        assertNotNull(panel.getActionMap().get("editorCancel"));
        panel.getActionMap().get("editorCancel").actionPerformed(null);
        assertEquals(1, hits[0]);
        assertEquals(1, hits[1]);
    }
}
