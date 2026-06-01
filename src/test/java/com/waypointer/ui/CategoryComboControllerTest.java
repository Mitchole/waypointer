package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.service.WaypointStore;
import java.util.UUID;
import javax.swing.JComboBox;
import org.junit.Test;
import static org.junit.Assert.*;

public class CategoryComboControllerTest
{
    private static WaypointStore freshStore()
    {
        // In-memory store, bootstrapped with an empty library (no debounced persistence wired).
        // Mirrors the existing CaptureFormTest setup.
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        return store;
    }

    @Test
    public void rebuildListsOrderedCategoriesPlusSentinel()
    {
        WaypointStore store = freshStore();
        Category a = store.createCategory("Alpha");
        JComboBox<CategoryComboItem> combo = new JComboBox<>();
        CategoryComboController c = new CategoryComboController(combo, store);

        c.rebuild(a.getId());

        // last item is the "+ New category..." sentinel
        CategoryComboItem last = combo.getItemAt(combo.getItemCount() - 1);
        assertTrue("last item should be the sentinel", last.isSentinel());
        // the requested id is selected
        CategoryComboItem sel = (CategoryComboItem) combo.getSelectedItem();
        assertNotNull(sel);
        assertEquals(a.getId(), sel.id());
    }

    @Test
    public void resolveSelectedIdReturnsUncategorizedForSentinel()
    {
        WaypointStore store = freshStore();
        JComboBox<CategoryComboItem> combo = new JComboBox<>();
        CategoryComboController c = new CategoryComboController(combo, store);
        c.rebuild(store.getUncategorized().getId());

        // select the sentinel (last item)
        combo.setSelectedIndex(combo.getItemCount() - 1);

        assertTrue(c.isSentinelSelected());
        assertEquals(store.getUncategorized().getId(), c.resolveSelectedId());
    }

    @Test
    public void resolveSelectedIdReturnsRealCategoryWhenSelected()
    {
        WaypointStore store = freshStore();
        Category a = store.createCategory("Alpha");
        JComboBox<CategoryComboItem> combo = new JComboBox<>();
        CategoryComboController c = new CategoryComboController(combo, store);
        c.rebuild(a.getId());

        assertFalse(c.isSentinelSelected());
        assertEquals(a.getId(), c.resolveSelectedId());
    }

    @Test
    public void rebuildWithUnknownIdSelectsFirstItemAndLeavesLastNonSentinelNull()
    {
        WaypointStore store = freshStore();
        store.createCategory("Alpha");
        JComboBox<CategoryComboItem> combo = new JComboBox<>();
        CategoryComboController c = new CategoryComboController(combo, store);

        // An id that is not in the store: rebuild finds no match, so it selects nothing
        // explicitly -- JComboBox defaults to index 0 -- and lastNonSentinel stays null.
        c.rebuild(UUID.randomUUID());

        assertEquals(0, combo.getSelectedIndex());
        assertFalse("first item is a real category, not the sentinel", c.isSentinelSelected());
        assertNull("no match means lastNonSentinel was never assigned", c.lastNonSentinel());
    }
}
