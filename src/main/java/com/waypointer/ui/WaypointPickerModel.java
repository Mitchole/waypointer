package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure selection state behind {@link WaypointTreePicker}. The set of checked waypoints is the
 * source of truth; a separate set tracks explicitly-checked empty categories, which have no
 * child waypoint to carry the signal. A category's tri-state is derived from its children. No
 * Swing imports; fully unit-testable.
 */
final class WaypointPickerModel
{
    enum Tri { CHECKED, UNCHECKED, PARTIAL }

    private final List<Category> orderedCategories;
    private final Map<UUID, List<Waypoint>> waypointsByCategory;
    private final Set<UUID> checkedWaypoints = new HashSet<>();
    private final Set<UUID> checkedEmptyCategories = new HashSet<>();

    /** Builds a model from {@code library} with every waypoint and every empty category checked. */
    WaypointPickerModel(Library library)
    {
        this.waypointsByCategory = new HashMap<>();
        for (Waypoint w : library.getWaypoints())
        {
            waypointsByCategory.computeIfAbsent(w.getCategoryId(), k -> new ArrayList<>()).add(w);
            checkedWaypoints.add(w.getId());
        }
        for (List<Waypoint> bucket : waypointsByCategory.values())
        {
            bucket.sort(Comparator.comparingInt(Waypoint::getSortOrder));
        }
        List<Category> cats = new ArrayList<>(library.getCategories());
        cats.sort(WaypointPickerModel::compareForDisplay);
        this.orderedCategories = cats;
        for (Category c : cats)
        {
            if (waypointsOf(c.getId()).isEmpty()) checkedEmptyCategories.add(c.getId());
        }
    }

    // Mirrors WaypointStore.getCategoriesOrdered: Uncategorized first, then user-created, then
    // bundled; ties broken by sortOrder.
    private static int compareForDisplay(Category a, Category b)
    {
        int tierA = a.isUncategorized() ? 0 : (a.isBundled() ? 2 : 1);
        int tierB = b.isUncategorized() ? 0 : (b.isBundled() ? 2 : 1);
        if (tierA != tierB) return Integer.compare(tierA, tierB);
        return Integer.compare(a.getSortOrder(), b.getSortOrder());
    }

    List<Category> getOrderedCategories() { return orderedCategories; }

    List<Waypoint> waypointsOf(UUID categoryId)
    {
        List<Waypoint> bucket = waypointsByCategory.get(categoryId);
        return bucket == null ? Collections.emptyList() : bucket;
    }

    boolean isWaypointChecked(UUID id) { return checkedWaypoints.contains(id); }

    void setWaypointChecked(UUID id, boolean checked)
    {
        if (checked) checkedWaypoints.add(id); else checkedWaypoints.remove(id);
    }

    Tri categoryState(UUID categoryId)
    {
        List<Waypoint> children = waypointsOf(categoryId);
        if (children.isEmpty())
        {
            return checkedEmptyCategories.contains(categoryId) ? Tri.CHECKED : Tri.UNCHECKED;
        }
        int checked = 0;
        for (Waypoint w : children) if (checkedWaypoints.contains(w.getId())) checked++;
        if (checked == 0) return Tri.UNCHECKED;
        if (checked == children.size()) return Tri.CHECKED;
        return Tri.PARTIAL;
    }

    void setCategoryChecked(UUID categoryId, boolean checked)
    {
        List<Waypoint> children = waypointsOf(categoryId);
        if (children.isEmpty())
        {
            if (checked) checkedEmptyCategories.add(categoryId);
            else checkedEmptyCategories.remove(categoryId);
            return;
        }
        for (Waypoint w : children) setWaypointChecked(w.getId(), checked);
    }

    void selectAll()
    {
        for (Category c : orderedCategories) setCategoryChecked(c.getId(), true);
    }

    void selectNone()
    {
        checkedWaypoints.clear();
        checkedEmptyCategories.clear();
    }

    Set<UUID> getSelectedWaypointIds() { return new HashSet<>(checkedWaypoints); }

    Set<UUID> getSelectedCategoryIds()
    {
        Set<UUID> out = new HashSet<>();
        for (Category c : orderedCategories)
        {
            if (categoryState(c.getId()) == Tri.CHECKED) out.add(c.getId());
        }
        return out;
    }

    boolean isEmptySelection()
    {
        return checkedWaypoints.isEmpty() && checkedEmptyCategories.isEmpty();
    }
}
