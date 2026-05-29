package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.PresetOverrides;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import com.waypointer.service.WaypointCapture;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;

final class InlinePresetWaypointEdit extends JPanel
{
    InlinePresetWaypointEdit(String category, PresetWaypoint original,
        PresetOverrides overrides, WaypointCapture capture, Runnable onClose)
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JTextField name = new JTextField(original.getName());
        Styles.textField(name);
        JTextArea desc = new JTextArea(original.getDescription() == null ? "" : original.getDescription(), 3, 12);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        Styles.textArea(desc);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(name, BorderLayout.NORTH);
        top.add(new JScrollPane(desc), BorderLayout.CENTER);
        add(top, BorderLayout.CENTER);

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

        Waypoint asWp = new Waypoint(original.getName(),
            original.getDescription() == null ? "" : original.getDescription(),
            original.getX(), original.getY(), original.getPlane());

        save.addActionListener(e -> {
            overrides.upsertWaypoint(category, asWp,
                new Waypoint(name.getText(), desc.getText(),
                    original.getX(), original.getY(), original.getPlane()));
            onClose.run();
        });
        recap.addActionListener(e -> capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED) return;
            overrides.upsertWaypoint(category, asWp,
                new Waypoint(name.getText(), desc.getText(),
                    WorldPointPacker.getX(packed),
                    WorldPointPacker.getY(packed),
                    WorldPointPacker.getPlane(packed)));
            onClose.run();
        }));
        cancel.addActionListener(e -> onClose.run());
    }
}
