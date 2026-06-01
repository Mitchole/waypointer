package com.waypointer.ui;

import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.service.RouteStore;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.UUID;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Edits one route's steps: an ordered list of {@link RouteStepRow}s plus add-step controls and a
 * repeating toggle. All mutations go through {@link RouteStore} so they persist and notify.
 */
final class RouteEditorPanel extends JPanel
{
    private final RouteStore store;
    private final UUID routeId;
    private final Runnable onBack;
    private final Runnable onMarkCurrentLocation;
    private final Runnable onAddFromLibrary;
    private final JPanel stepList = new JPanel();

    RouteEditorPanel(RouteStore store, UUID routeId, Runnable onBack,
        Runnable onMarkCurrentLocation, Runnable onAddFromLibrary)
    {
        this.store = store;
        this.routeId = routeId;
        this.onBack = onBack;
        this.onMarkCurrentLocation = onMarkCurrentLocation;
        this.onAddFromLibrary = onAddFromLibrary;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(), BorderLayout.NORTH);
        stepList.setLayout(new BoxLayout(stepList, BoxLayout.Y_AXIS));
        stepList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(Styles.pinnedScrollPane(stepList), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        rebuild();
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        // ◀ = left-pointing triangle for back navigation
        JButton back = new JButton("◀ Routes");
        Styles.compactSecondaryButton(back);
        back.addActionListener(e -> onBack.run());
        header.add(back);

        Route r = store.getRouteById(routeId);
        JCheckBox repeating = new JCheckBox("Repeating", r != null && r.isRepeating());
        repeating.setBackground(ColorScheme.DARK_GRAY_COLOR);
        repeating.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        repeating.addActionListener(e -> store.setRepeating(routeId, repeating.isSelected()));
        header.add(repeating);
        return header;
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        // ◉ = waypoint glyph; capture the player's current tile as a step
        JButton markHere = new JButton("◉ Mark location");
        Styles.secondaryButton(markHere);
        markHere.addActionListener(e -> onMarkCurrentLocation.run());
        footer.add(markHere);

        JButton fromSaved = new JButton("◉ From saved");
        Styles.secondaryButton(fromSaved);
        fromSaved.addActionListener(e -> onAddFromLibrary.run());
        footer.add(fromSaved);

        // ✎ = pencil glyph for manual step
        JButton addManual = new JButton("✎ Add manual step");
        Styles.secondaryButton(addManual);
        addManual.addActionListener(e -> {
            String text = JOptionPane.showInputDialog(this, "Instruction:");
            if (text != null && !text.trim().isEmpty()) store.addManualStep(routeId, text.trim());
        });
        footer.add(addManual);
        return footer;
    }

    /** Rebuild the step list from the store. Call after any mutation. */
    void rebuild()
    {
        stepList.removeAll();
        Route r = store.getRouteById(routeId);
        if (r != null)
        {
            int i = 0;
            for (RouteStep s : r.getSteps())
            {
                stepList.add(new RouteStepRow(i++, s,
                    () -> editStep(s),
                    step -> store.deleteStep(routeId, step.getId())));
            }
        }
        stepList.revalidate();
        stepList.repaint();
    }

    private void editStep(RouteStep s)
    {
        String text = JOptionPane.showInputDialog(this, "Edit step:", s.getLabel());
        if (text != null) store.updateStepLabel(routeId, s.getId(), text.trim());
    }
}
