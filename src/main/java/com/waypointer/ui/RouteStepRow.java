package com.waypointer.ui;

import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** One step row in {@link RouteEditorPanel}: glyph + label + delete. Click the label to edit. */
final class RouteStepRow extends JPanel
{
    private final RouteStep step;

    // ◉ = filled circle (WAYPOINT), ✎ = pencil (MANUAL), ✕ = multiply/close
    private static final String GLYPH_WAYPOINT = "◉";
    private static final String GLYPH_MANUAL   = "✎";
    private static final String GLYPH_DELETE   = "✕";

    RouteStepRow(int index, RouteStep step, Runnable onEdit, Consumer<RouteStep> onDelete)
    {
        this.step = step;
        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        String labelText = step.getLabel() == null || step.getLabel().isEmpty()
            ? "(empty)" : step.getLabel();
        JLabel text = new JLabel((index + 1) + ". " + stepGlyph(step.getType()) + "  " + labelText);
        text.setFont(FontManager.getRunescapeSmallFont());
        text.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        text.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { onEdit.run(); }
        });
        add(text, BorderLayout.CENTER);

        JButton del = new JButton(GLYPH_DELETE);
        Styles.compactSecondaryButton(del);
        del.getAccessibleContext().setAccessibleName("Delete step");
        del.addActionListener(e -> onDelete.accept(step));
        add(del, BorderLayout.EAST);
    }

    static String stepGlyph(StepType type)
    {
        return type == StepType.WAYPOINT ? GLYPH_WAYPOINT : GLYPH_MANUAL;
    }

    RouteStep getStep() { return step; }

    @Override
    public Dimension getMaximumSize() { return Styles.capHeight(this); }
}
