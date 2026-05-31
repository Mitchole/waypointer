package com.waypointer.ui;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Pure selection state for the panel's bulk select mode. Keyed by waypoint UUID so the selection
 * survives filter changes and panel rebuilds. No Swing imports; fully unit-testable.
 */
final class BulkSelection
{
    enum TriState { NONE, PARTIAL, ALL }

    private final Set<UUID> ids = new LinkedHashSet<>();

    void toggle(UUID id)
    {
        if (!ids.add(id)) ids.remove(id);
    }

    /**
     * Additively selects the inclusive range of {@code ordered} between {@code fromId} and
     * {@code toId} (anchor order does not matter). No-op if either id is not in {@code ordered}.
     */
    void selectRange(List<UUID> ordered, UUID fromId, UUID toId)
    {
        int from = ordered.indexOf(fromId);
        int to = ordered.indexOf(toId);
        if (from < 0 || to < 0) return;
        int lo = Math.min(from, to);
        int hi = Math.max(from, to);
        for (int i = lo; i <= hi; i++) ids.add(ordered.get(i));
    }

    void setCategory(Collection<UUID> categoryWaypointIds, boolean selected)
    {
        if (selected) ids.addAll(categoryWaypointIds);
        else ids.removeAll(categoryWaypointIds);
    }

    TriState categoryState(Collection<UUID> categoryWaypointIds)
    {
        if (categoryWaypointIds.isEmpty()) return TriState.NONE;
        int present = 0;
        for (UUID id : categoryWaypointIds) if (ids.contains(id)) present++;
        if (present == 0) return TriState.NONE;
        if (present == categoryWaypointIds.size()) return TriState.ALL;
        return TriState.PARTIAL;
    }

    boolean isEmpty() { return ids.isEmpty(); }

    int size() { return ids.size(); }

    // Insertion-ordered copy: bulk move assigns tail sortOrder in iteration order, so callers
    // get the rows back in the order they were selected, not arbitrary hash order.
    Set<UUID> ids() { return new LinkedHashSet<>(ids); }

    void clear() { ids.clear(); }
}
