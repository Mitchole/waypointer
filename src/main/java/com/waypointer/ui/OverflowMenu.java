package com.waypointer.ui;

import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.service.WaypointStore;
import java.awt.Component;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Builds the panel's "⋮" overflow menu. Constructed by Guice with all of its services so the
 * panel doesn't need to hold them just to forward them in. The panel passes its own component
 * as the anchor at {@link #show(Component, Component)} time so JOption dialogs land on the
 * right window.
 */
@Singleton
final class OverflowMenu
{
    private final WaypointStore store;
    private final WaypointShareCodec shareCodec;

    @Inject
    OverflowMenu(WaypointStore store, WaypointShareCodec shareCodec)
    {
        this.store = store;
        this.shareCodec = shareCodec;
    }

    /** Show the popup anchored just below {@code near}; uses {@code panel} as JOption parent. */
    void show(Component near, WaypointerPanel panel, Toasts toasts)
    {
        Component anchor = panel;
        JPopupMenu menu = new JPopupMenu();

        JMenuItem newCategory = new JMenuItem("New category...");
        newCategory.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(anchor, "Category name:", "New category",
                JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) return;
            try { store.createCategory(name.trim()); }
            catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(anchor, ex.getMessage(), "Waypointer",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        menu.add(newCategory);

        // Expand all / Collapse all: one toggle whose label flips based on current state.
        // Majority-collapsed → "Expand all"; otherwise "Collapse all". Walks both Pinned and
        // real categories via WaypointerPanel.setAllSectionsExpanded.
        boolean expandLabel = panel.isMajorityCollapsed();
        JMenuItem expandToggle = new JMenuItem(expandLabel ? "Expand all" : "Collapse all");
        expandToggle.addActionListener(e -> panel.setAllSectionsExpanded(expandLabel));
        menu.add(expandToggle);
        menu.addSeparator();

        JMenuItem exportItem = new JMenuItem("Export...");
        exportItem.addActionListener(e ->
            new ExportPickerDialog(SwingUtilities.getWindowAncestor(anchor), store, shareCodec,
                toasts, null).setVisible(true));
        menu.add(exportItem);

        JMenuItem importItem = new JMenuItem("Import...");
        importItem.addActionListener(e ->
            new ImportPickerDialog(SwingUtilities.getWindowAncestor(anchor), store, shareCodec,
                toasts).setVisible(true));
        menu.add(importItem);

        menu.addSeparator();

        JMenuItem reset = new JMenuItem("Reset library...");
        reset.addActionListener(e -> {
            // Custom options with Cancel as the default-focused button so a stray Enter
            // can't wipe the whole library - there is no undo for this.
            String[] options = {"Cancel", "Delete all waypoints and categories"};
            int choice = JOptionPane.showOptionDialog(anchor,
                "Delete ALL waypoints and categories? This cannot be undone.",
                "Reset library", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            if (choice == 1) store.bootstrap(new Library());
        });
        menu.add(reset);

        menu.show(near, 0, near.getHeight());
    }
}
