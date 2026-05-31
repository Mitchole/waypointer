package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WaypointPickerModelTest
{
    private static Waypoint wp(UUID id, UUID catId)
    {
        return new Waypoint(id, "w", 1, catId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);
    }

    @Test
    public void defaultsToAllChecked()
    {
        UUID cat = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(cat, "C", 0, false, null, false));
        lib.getWaypoints().add(wp(a, cat));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        assertTrue(m.isWaypointChecked(a));
        assertEquals(WaypointPickerModel.Tri.CHECKED, m.categoryState(cat));
        assertFalse(m.isEmptySelection());
    }

    @Test
    public void childTogglesRollUpToPartialThenUnchecked()
    {
        UUID cat = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(cat, "C", 0, false, null, false));
        lib.getWaypoints().add(wp(a, cat));
        lib.getWaypoints().add(wp(b, cat));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        m.setWaypointChecked(a, false);
        assertEquals(WaypointPickerModel.Tri.PARTIAL, m.categoryState(cat));
        m.setWaypointChecked(b, false);
        assertEquals(WaypointPickerModel.Tri.UNCHECKED, m.categoryState(cat));
    }

    @Test
    public void categoryToggleCascadesToChildren()
    {
        UUID cat = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(cat, "C", 0, false, null, false));
        lib.getWaypoints().add(wp(a, cat));
        lib.getWaypoints().add(wp(b, cat));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        m.setCategoryChecked(cat, false);
        assertFalse(m.isWaypointChecked(a));
        assertFalse(m.isWaypointChecked(b));
        assertTrue(m.getSelectedWaypointIds().isEmpty());

        m.setCategoryChecked(cat, true);
        assertTrue(m.isWaypointChecked(a));
        assertEquals(2, m.getSelectedWaypointIds().size());
    }

    @Test
    public void emptyCategoryCheckTrackedExplicitly()
    {
        UUID cat = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(cat, "Empty", 0, false, null, false));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        assertEquals(WaypointPickerModel.Tri.CHECKED, m.categoryState(cat));
        assertTrue(m.getSelectedCategoryIds().contains(cat));

        m.setCategoryChecked(cat, false);
        assertEquals(WaypointPickerModel.Tri.UNCHECKED, m.categoryState(cat));
        assertTrue(m.isEmptySelection());
    }

    @Test
    public void selectNoneThenSelectAll()
    {
        UUID cat = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(cat, "C", 0, false, null, false));
        lib.getWaypoints().add(wp(a, cat));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        m.selectNone();
        assertTrue(m.isEmptySelection());
        assertTrue(m.getSelectedWaypointIds().isEmpty());
        m.selectAll();
        assertEquals(1, m.getSelectedWaypointIds().size());
    }

    @Test
    public void partialCategoryIsExcludedFromSelectedCategoryIds()
    {
        UUID cat = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(cat, "C", 0, false, null, false));
        lib.getWaypoints().add(wp(a, cat));
        lib.getWaypoints().add(wp(b, cat));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        m.setWaypointChecked(a, false);
        assertEquals(WaypointPickerModel.Tri.PARTIAL, m.categoryState(cat));
        assertFalse(m.getSelectedCategoryIds().contains(cat));
        assertEquals(1, m.getSelectedWaypointIds().size());

        m.setWaypointChecked(a, true);
        assertEquals(WaypointPickerModel.Tri.CHECKED, m.categoryState(cat));
    }

    @Test
    public void fullyCheckedNonEmptyCategoryIsInSelectedCategoryIds()
    {
        UUID cat = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(cat, "C", 0, false, null, false));
        lib.getWaypoints().add(wp(a, cat));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        assertTrue(m.getSelectedCategoryIds().contains(cat));
    }

    @Test
    public void selectAllCoversEveryCategory()
    {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(c1, "One", 0, false, null, false));
        lib.getCategories().add(new Category(c2, "Two", 1, false, null, false));
        lib.getWaypoints().add(wp(a, c1));
        lib.getWaypoints().add(wp(b, c2));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        m.selectNone();
        assertTrue(m.isEmptySelection());
        m.selectAll();
        assertEquals(2, m.getSelectedWaypointIds().size());
        assertEquals(WaypointPickerModel.Tri.CHECKED, m.categoryState(c1));
        assertEquals(WaypointPickerModel.Tri.CHECKED, m.categoryState(c2));
    }

    @Test
    public void uncategorizedSortsFirst()
    {
        UUID user = UUID.randomUUID();
        UUID unc = UUID.randomUUID();
        Library lib = new Library();
        lib.getCategories().add(new Category(user, "Banks", 0, false, null, false));
        lib.getCategories().add(new Category(unc, "Uncategorized", 5, true, null, false));

        WaypointPickerModel m = new WaypointPickerModel(lib);
        assertTrue(m.getOrderedCategories().get(0).isUncategorized());
    }
}
