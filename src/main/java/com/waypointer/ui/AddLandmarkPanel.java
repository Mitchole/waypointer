package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointCapture;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;

final class AddLandmarkPanel extends JPanel
{
    private final JTextField nameField = new JTextField();
    private final JRadioButton pointBtn = new JRadioButton("Point", true);
    private final JRadioButton areaBtn = new JRadioButton("Area");
    private final JSlider sizeSlider = new JSlider(2, 10, 3);
    private final JLabel sizeLabel = new JLabel("3x3");

    AddLandmarkPanel(WaypointCapture capture, LandmarkOverrides overrides,
        Supplier<LandmarkType> currentType,
        Consumer<Integer> onSizeChanged,
        Runnable onClose)
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        ButtonGroup g = new ButtonGroup();
        g.add(pointBtn);
        g.add(areaBtn);
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        header.setOpaque(false);
        header.add(pointBtn);
        header.add(areaBtn);
        header.add(sizeSlider);
        header.add(sizeLabel);

        Styles.textField(nameField);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        south.setOpaque(false);
        JButton capBtn = new JButton("Capture");
        Styles.secondaryButton(capBtn);
        JButton cancelBtn = new JButton("Cancel");
        Styles.secondaryButton(cancelBtn);
        south.add(cancelBtn);
        south.add(capBtn);

        add(header, BorderLayout.NORTH);
        add(nameField, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        sizeSlider.addChangeListener(e -> {
            int n = sizeSlider.getValue();
            sizeLabel.setText(n + "x" + n);
            onSizeChanged.accept(areaBtn.isSelected() ? n : 0);
        });
        pointBtn.addActionListener(e -> onSizeChanged.accept(0));
        areaBtn.addActionListener(e -> onSizeChanged.accept(sizeSlider.getValue()));

        capBtn.addActionListener(e -> capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED) return;
            int x = WorldPointPacker.getX(packed);
            int y = WorldPointPacker.getY(packed);
            int plane = WorldPointPacker.getPlane(packed);
            String name = nameField.getText().isEmpty() ? "Unnamed" : nameField.getText();
            int x2 = x, y2 = y;
            if (areaBtn.isSelected())
            {
                int n = sizeSlider.getValue();
                x2 = x + n - 1;
                y2 = y + n - 1;
            }
            overrides.addEntry(currentType.get().name(),
                new Entry(name, x, y, x2, y2, plane));
            onClose.run();
        }));
        cancelBtn.addActionListener(e -> onClose.run());

        // Enter on the name field captures; Escape cancels.
        EditorKeyBindings.commitOnEnterCancelOnEscape(this, nameField, capBtn, cancelBtn);
    }
}
