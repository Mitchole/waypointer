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
import javax.swing.JButton;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

final class AddLandmarkPanel extends JPanel
{
    AddLandmarkPanel(WaypointCapture capture, LandmarkOverrides overrides,
        Supplier<LandmarkType> currentType,
        Consumer<Integer> onSizeChanged,
        Runnable onClose)
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        LandmarkCaptureControls controls = new LandmarkCaptureControls("", false, 3);
        controls.addSizeListeners(onSizeChanged);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        south.setOpaque(false);
        JButton capBtn = new JButton("Capture");
        Styles.secondaryButton(capBtn);
        JButton cancelBtn = new JButton("Cancel");
        Styles.secondaryButton(cancelBtn);
        south.add(cancelBtn);
        south.add(capBtn);

        add(controls.header(), BorderLayout.NORTH);
        add(controls.nameField(), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        capBtn.addActionListener(e -> capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED) return;
            int x = WorldPointPacker.getX(packed);
            int y = WorldPointPacker.getY(packed);
            int plane = WorldPointPacker.getPlane(packed);
            String name = controls.getName().isEmpty() ? "Unnamed" : controls.getName();
            int[] c = controls.cornersFor(x, y);
            overrides.addEntry(currentType.get().name(),
                new Entry(name, c[0], c[1], c[2], c[3], plane));
            onClose.run();
        }));
        cancelBtn.addActionListener(e -> onClose.run());

        // Enter on the name field captures; Escape cancels.
        EditorKeyBindings.commitOnEnterCancelOnEscape(this, controls.nameField(), capBtn, cancelBtn);
    }
}
