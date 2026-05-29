package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointCapture;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;

final class InlineLandmarkEdit extends JPanel
{
    InlineLandmarkEdit(LandmarkType type, BboxIndex.Entry original,
        LandmarkOverrides overrides, WaypointCapture capture, Runnable onClose)
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JTextField nameField = new JTextField(original.name);
        Styles.textField(nameField);
        add(nameField, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        south.setOpaque(false);
        JButton save = new JButton("Save");
        Styles.secondaryButton(save);
        JButton recap = new JButton("Recapture");
        Styles.secondaryButton(recap);
        JButton cancel = new JButton("Cancel");
        Styles.secondaryButton(cancel);
        south.add(cancel);
        south.add(recap);
        south.add(save);
        add(south, BorderLayout.SOUTH);

        Entry asEntry = new Entry(original.name, original.x1, original.y1,
            original.x2, original.y2, original.plane);

        save.addActionListener(e -> {
            overrides.replaceEntry(type.name(), asEntry,
                new Entry(nameField.getText(), original.x1, original.y1,
                    original.x2, original.y2, original.plane));
            onClose.run();
        });
        recap.addActionListener(e -> capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED) return;
            int x = WorldPointPacker.getX(packed);
            int y = WorldPointPacker.getY(packed);
            int p = WorldPointPacker.getPlane(packed);
            overrides.replaceEntry(type.name(), asEntry,
                new Entry(nameField.getText(), x, y, x, y, p));
            onClose.run();
        }));
        cancel.addActionListener(e -> onClose.run());
    }
}
