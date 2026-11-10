package com.waypointer.ui;

import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One step row in {@link RouteEditorPanel}: grip + index + type glyph + label, with edit and
 * delete buttons. Drag the grip to reorder; click the pencil (or the label) to edit.
 */
final class RouteStepRow extends JPanel
{
    // ◉ = filled circle (WAYPOINT), ▤ = note card (MANUAL), ⠿ = drag grip,
    // ✎ = pencil (edit), ✕ = multiply/close (delete)
    private static final String GLYPH_WAYPOINT = "◉";
    private static final String GLYPH_MANUAL   = "▤";
    private static final String GLYPH_GRIP     = "⠿";
    private static final String GLYPH_EDIT     = "✎";
    private static final String GLYPH_DELETE   = "✕";

    /** Text for the distance from the previous stop, or null when there is none to show. */
    static String legText(int leg)
    {
        return leg < 0 ? null : String.format(leg == 1 ? "%,d tile" : "%,d tiles", leg);
    }

    RouteStepRow(int index, int leg, RouteStep step, Runnable onEdit, Consumer<RouteStep> onDelete,
        RouteStepDragHandler dragHandler)
    {
        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 4));

        // Grip handle (west): dragging it past the threshold starts a reorder.
        JLabel grip = new JLabel(GLYPH_GRIP);
        grip.setFont(FontManager.getRunescapeSmallFont());
        grip.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
        grip.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        grip.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        add(grip, BorderLayout.WEST);

        String labelText = step.getLabel() == null || step.getLabel().isEmpty()
            ? "(empty)" : step.getLabel();
        JLabel text = new JLabel((index + 1) + ". " + stepGlyph(step.getType()) + "  " + labelText);
        text.setFont(FontManager.getRunescapeSmallFont());
        text.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        // Long instructions are cut off by the panel width; the tooltip carries the rest.
        text.setToolTipText(labelText);
        text.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { onEdit.run(); }
        });

        // The leg distance sits on its own line under the label so it cannot crowd the buttons.
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        text.setAlignmentX(LEFT_ALIGNMENT);
        center.add(text);
        String legText = legText(leg);
        if (legText != null)
        {
            JLabel legLabel = new JLabel(legText);
            legLabel.setFont(FontManager.getRunescapeSmallFont());
            legLabel.setForeground(Styles.MUTED_TEXT);
            legLabel.setAlignmentX(LEFT_ALIGNMENT);
            legLabel.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
            center.add(legLabel);
        }
        add(center, BorderLayout.CENTER);

        // East: edit (pencil) then delete (x). Pencil is the primary, visible edit affordance.
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton edit = new JButton(GLYPH_EDIT);
        Styles.glyphButton(edit);
        edit.getAccessibleContext().setAccessibleName("Edit step");
        edit.addActionListener(e -> onEdit.run());
        actions.add(edit);

        JButton del = new JButton(GLYPH_DELETE);
        Styles.glyphButton(del);
        del.getAccessibleContext().setAccessibleName("Delete step");
        del.addActionListener(e -> onDelete.accept(step));
        actions.add(del);

        add(actions, BorderLayout.EAST);

        dragHandler.attach(this, grip, step.getId());
    }

    static String stepGlyph(StepType type)
    {
        return type == StepType.WAYPOINT ? GLYPH_WAYPOINT : GLYPH_MANUAL;
    }

    @Override
    public Dimension getMaximumSize() { return Styles.capHeight(this); }
}
