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
import javax.swing.JButton;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Dev-tools inline editor for a single landmark. Commit contract: changes commit ONLY on the
 * explicit Save or Recapture button (via {@code overrides.replaceEntry}); Cancel and any parent
 * rebuild discard unsaved text. This is deliberate and differs from {@link InlineEditPanel},
 * which flushes on focus-loss + removeNotify. A flush-on-removeNotify here would silently commit
 * a half-typed rename whenever an unrelated bboxIndex/catalog subscription fire rebuilds the
 * parent editor -- worse than losing the text. Keep the explicit-Save contract.
 */
final class InlineLandmarkEdit extends JPanel
{
    InlineLandmarkEdit(LandmarkType type, BboxIndex.Entry original,
        LandmarkOverrides overrides, WaypointCapture capture,
        Consumer<Integer> onSizeChanged, Runnable onClose)
    {
        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        boolean originallyArea = original.x1 != original.x2 || original.y1 != original.y2;
        int initialSize = Math.max(original.x2 - original.x1 + 1, original.y2 - original.y1 + 1);

        LandmarkCaptureControls controls =
            new LandmarkCaptureControls(original.name, originallyArea, initialSize);

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

        add(controls.header(), BorderLayout.NORTH);
        add(controls.nameField(), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        Entry asEntry = new Entry(original.name, original.x1, original.y1,
            original.x2, original.y2, original.plane);

        controls.addSizeListeners(onSizeChanged);

        // Save keeps the captured tiles; only the name changes.
        save.addActionListener(e -> {
            overrides.replaceEntry(type.name(), asEntry,
                new Entry(controls.getName(), original.x1, original.y1,
                    original.x2, original.y2, original.plane));
            onClose.run();
        });
        // Recapture re-places the entry at the player's tile, as a point or an NxN area.
        recap.addActionListener(e -> capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED) return;
            int x = WorldPointPacker.getX(packed);
            int y = WorldPointPacker.getY(packed);
            int p = WorldPointPacker.getPlane(packed);
            int[] c = controls.cornersFor(x, y);
            overrides.replaceEntry(type.name(), asEntry,
                new Entry(controls.getName(), c[0], c[1], c[2], c[3], p));
            onClose.run();
        }));
        cancel.addActionListener(e -> onClose.run());

        // Enter on the name field saves the rename; Escape cancels. Recapture stays mouse-only.
        EditorKeyBindings.commitOnEnterCancelOnEscape(this, controls.nameField(), save, cancel);

        // Sync the in-scene area-preview overlay with the initial Point/Area state.
        onSizeChanged.accept(originallyArea ? controls.getSize() : 0);
    }
}
