package com.waypointer.ui;

import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.service.WaypointStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;

public class PasteImportDialog extends JDialog
{
    enum CodeKind { WAYPOINT_SINGLE, WAYPOINT_LIBRARY, UNKNOWN }

    static CodeKind sniff(String input)
    {
        if (input == null) return CodeKind.UNKNOWN;
        String t = input.trim();
        if (t.startsWith(WaypointShareCodec.LIBRARY_MAGIC)) return CodeKind.WAYPOINT_LIBRARY;
        if (t.startsWith(WaypointShareCodec.SINGLE_MAGIC))  return CodeKind.WAYPOINT_SINGLE;
        return CodeKind.UNKNOWN;
    }

    public PasteImportDialog(Window owner, WaypointStore store, WaypointShareCodec codec)
    {
        super(owner, "Import from share code", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        contentPane.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JTextArea area = new JTextArea(8, 40);
        Styles.textArea(area);
        JLabel prompt = new JLabel("Paste a WP1: or WPL1: code:");
        prompt.setForeground(Color.WHITE);
        add(prompt, BorderLayout.NORTH);
        JScrollPane areaScroll = new JScrollPane(area);
        areaScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        add(areaScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton cancel = new JButton("Cancel");
        JButton importBtn = new JButton("Import");
        Styles.secondaryButton(cancel);
        Styles.secondaryButton(importBtn);
        cancel.addActionListener(e -> dispose());
        importBtn.addActionListener(e -> {
            String text = area.getText() == null ? "" : area.getText().trim();
            try
            {
                switch (sniff(text))
                {
                    case WAYPOINT_SINGLE:
                    case WAYPOINT_LIBRARY:
                    {
                        Library incoming = codec.decodeLibrary(text);
                        int n = incoming.getWaypoints().size();
                        int confirm = JOptionPane.showConfirmDialog(this,
                            String.format("This code adds %d waypoint(s) across %d categor%s. Import?",
                                n, incoming.getCategories().size(),
                                incoming.getCategories().size() == 1 ? "y" : "ies"),
                            "Confirm import", JOptionPane.OK_CANCEL_OPTION);
                        if (confirm != JOptionPane.OK_OPTION) return;
                        WaypointStore.ImportResult r = store.importMerge(incoming);
                        JOptionPane.showMessageDialog(this,
                            String.format("Imported %d waypoints, %d categories. Skipped %d.",
                                r.waypointsAdded, r.categoriesAdded, r.waypointsSkipped),
                            "Waypointer", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                        break;
                    }
                    default:
                        JOptionPane.showMessageDialog(this,
                            "Not a known share code (expected WP1: or WPL1:).",
                            "Waypointer", JOptionPane.WARNING_MESSAGE);
                }
            }
            catch (RuntimeException ex)
            {
                JOptionPane.showMessageDialog(this, "Couldn't read share code: " + ex.getMessage(),
                    "Waypointer", JOptionPane.WARNING_MESSAGE);
            }
        });
        buttons.add(cancel);
        buttons.add(importBtn);
        add(buttons, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }
}
