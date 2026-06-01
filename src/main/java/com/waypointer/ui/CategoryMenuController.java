package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.service.IconCatalog;
import com.waypointer.service.WaypointStore;
import java.awt.Component;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;

/**
 * Category-level menu actions wired into each {@link CategorySection} via
 * {@link CategorySection.Actions}: rename, delete (with undo toast), set icon, set colour.
 * Pure dialog + store glue extracted from {@link WaypointerPanel}; holds no state of its own.
 */
final class CategoryMenuController
{
    private final WaypointStore store;
    private final SpriteManager spriteManager;
    private final IconCatalog iconCatalog;
    private final Toasts toasts;
    // Parent for modal dialogs and the source of the window ancestor the icon picker needs.
    private final JComponent parent;

    CategoryMenuController(WaypointStore store, SpriteManager spriteManager,
        IconCatalog iconCatalog, Toasts toasts, JComponent parent)
    {
        this.store = store;
        this.spriteManager = spriteManager;
        this.iconCatalog = iconCatalog;
        this.toasts = toasts;
        this.parent = parent;
    }

    void promptRename(Category c)
    {
        String newName = JOptionPane.showInputDialog(parent,
            "Rename '" + c.getName() + "' to:", c.getName());
        if (newName == null || newName.trim().isEmpty()) return;
        try { store.renameCategory(c.getId(), newName.trim()); }
        catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Waypointer",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    void promptDelete(Category c)
    {
        String[] options = {"Move to Uncategorized", "Delete waypoints", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent,
            "Delete category '" + c.getName() + "'?\n\nWhat to do with its waypoints?",
            "Delete category", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);
        String name = c.getName();
        if (choice == 0)
        {
            store.deleteCategory(c.getId(), true);
            toasts.show("Deleted category '" + name + "'", "Undo", store::undoLast);
        }
        else if (choice == 1)
        {
            // childCount is captured BEFORE the delete - deleteCategory(_, false) removes
            // those waypoints so getWaypointsInCategory would return 0 afterwards.
            int childCount = store.getWaypointsInCategory(c.getId()).size();
            store.deleteCategory(c.getId(), false);
            String msg = "Deleted '" + name + "' and " + childCount
                + (childCount == 1 ? " waypoint" : " waypoints");
            toasts.show(msg, "Undo", store::undoLast);
        }
    }

    void promptSetIcon(Category c)
    {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        new IconPickerDialog(owner, spriteManager, iconCatalog, c.getIconId(),
            iconId -> store.setCategoryIcon(c.getId(), iconId)).setVisible(true);
    }

    void promptSetColour(Category c, Component anchor)
    {
        ColorPalettePopup.build(c.getColor(), rgb -> store.setCategoryColor(c.getId(), rgb))
            .show(anchor, 0, anchor.getHeight());
    }
}
