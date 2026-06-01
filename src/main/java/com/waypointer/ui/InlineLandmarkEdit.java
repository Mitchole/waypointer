package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointCapture;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;

final class InlineLandmarkEdit extends JPanel
{
    // Slider bounds match AddLandmarkPanel so point/area capture behaves identically.
    private static final int MIN_SIZE = 2;
    private static final int MAX_SIZE = 10;

    InlineLandmarkEdit(LandmarkType type, BboxIndex.Entry original,
        LandmarkOverrides overrides, WaypointCapture capture,
        Consumer<Integer> onSizeChanged, Runnable onClose)
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        boolean originallyArea = original.x1 != original.x2 || original.y1 != original.y2;
        int initialSize = clampSize(Math.max(
            original.x2 - original.x1 + 1, original.y2 - original.y1 + 1));

        // Point/Area chooser + size slider. Recapture honours the selection; Save renames in place.
        JRadioButton pointBtn = new JRadioButton("Point", !originallyArea);
        JRadioButton areaBtn = new JRadioButton("Area", originallyArea);
        pointBtn.setOpaque(false);
        areaBtn.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        group.add(pointBtn);
        group.add(areaBtn);
        JSlider sizeSlider = new JSlider(MIN_SIZE, MAX_SIZE, initialSize);
        sizeSlider.setOpaque(false);
        JLabel sizeLabel = new JLabel(initialSize + "x" + initialSize);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        header.add(pointBtn);
        header.add(areaBtn);
        header.add(sizeSlider);
        header.add(sizeLabel);

        JTextField nameField = new JTextField(original.name);
        Styles.textField(nameField);

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

        add(header, BorderLayout.NORTH);
        add(nameField, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        Entry asEntry = new Entry(original.name, original.x1, original.y1,
            original.x2, original.y2, original.plane);

        sizeSlider.addChangeListener(e -> {
            int n = sizeSlider.getValue();
            sizeLabel.setText(n + "x" + n);
            onSizeChanged.accept(areaBtn.isSelected() ? n : 0);
        });
        pointBtn.addActionListener(e -> onSizeChanged.accept(0));
        areaBtn.addActionListener(e -> onSizeChanged.accept(sizeSlider.getValue()));

        // Save keeps the captured tiles; only the name changes.
        save.addActionListener(e -> {
            overrides.replaceEntry(type.name(), asEntry,
                new Entry(nameField.getText(), original.x1, original.y1,
                    original.x2, original.y2, original.plane));
            onClose.run();
        });
        // Recapture re-places the entry at the player's tile, as a point or an NxN area.
        recap.addActionListener(e -> capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED) return;
            int x = WorldPointPacker.getX(packed);
            int y = WorldPointPacker.getY(packed);
            int p = WorldPointPacker.getPlane(packed);
            int x2 = x, y2 = y;
            if (areaBtn.isSelected())
            {
                int n = sizeSlider.getValue();
                x2 = x + n - 1;
                y2 = y + n - 1;
            }
            overrides.replaceEntry(type.name(), asEntry,
                new Entry(nameField.getText(), x, y, x2, y2, p));
            onClose.run();
        }));
        cancel.addActionListener(e -> onClose.run());

        // Sync the in-scene area-preview overlay with the initial Point/Area state.
        onSizeChanged.accept(originallyArea ? initialSize : 0);
    }

    private static int clampSize(int n)
    {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, n));
    }
}
