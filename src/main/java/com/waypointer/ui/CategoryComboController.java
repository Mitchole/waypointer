package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.service.WaypointStore;
import java.awt.event.ActionListener;
import java.util.UUID;
import javax.swing.JComboBox;

/**
 * Shared category-combo machinery for the capture forms. Owns populating the combo with the
 * ordered categories plus the "+ New category..." sentinel, tracking the last real selection,
 * and resolving the selected id at save time. The "+ New category" reaction itself stays with
 * the host -- CaptureForm's inline row -- while only the mechanical rebuild + resolution are
 * shared here.
 */
class CategoryComboController
{
    private final JComboBox<CategoryComboItem> combo;
    private final WaypointStore store;
    private CategoryComboItem lastNonSentinel;

    CategoryComboController(JComboBox<CategoryComboItem> combo, WaypointStore store)
    {
        this.combo = combo;
        this.store = store;
    }

    /** Repopulate from the store and select {@code selectId}, suppressing action events. */
    void rebuild(UUID selectId)
    {
        ActionListener[] listeners = combo.getActionListeners();
        for (ActionListener l : listeners) combo.removeActionListener(l);

        combo.removeAllItems();
        CategoryComboItem toSelect = null;
        for (Category c : store.getCategoriesOrdered())
        {
            CategoryComboItem item = new CategoryComboItem(c);
            combo.addItem(item);
            if (c.getId().equals(selectId)) toSelect = item;
        }
        combo.addItem(CategoryComboItem.sentinel("+ New category..."));
        if (toSelect != null)
        {
            combo.setSelectedItem(toSelect);
            lastNonSentinel = toSelect;
        }

        for (ActionListener l : listeners) combo.addActionListener(l);
    }

    boolean isSentinelSelected()
    {
        CategoryComboItem sel = selected();
        return sel != null && sel.isSentinel();
    }

    /** The last real (non-sentinel) selection; null only until the first rebuild that matches a category. */
    CategoryComboItem lastNonSentinel()
    {
        return lastNonSentinel;
    }

    void setLastNonSentinel(CategoryComboItem item)
    {
        this.lastNonSentinel = item;
    }

    /** Save-time resolution: uncategorized for null/sentinel, else the selected category id. */
    UUID resolveSelectedId()
    {
        CategoryComboItem sel = selected();
        return (sel == null || sel.isSentinel()) ? store.getUncategorized().getId() : sel.id();
    }

    private CategoryComboItem selected()
    {
        return (CategoryComboItem) combo.getSelectedItem();
    }
}
