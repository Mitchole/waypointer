package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import java.util.Locale;

// Search-filter logic, factored out of the panel for unit testing. Match scope is
// case-insensitive substring against waypoint name, waypoint notes, and the containing
// category name. A category-name match counts every waypoint in that category.
public final class WaypointFilter
{
    private WaypointFilter() {}

    public static boolean matches(Waypoint w, Category c, String filter)
    {
        if (filter == null || filter.isEmpty()) return true;
        return matchesLowered(w, c, filter.toLowerCase(Locale.ROOT));
    }

    public static boolean categoryNameMatches(Category c, String filter)
    {
        if (filter == null || filter.isEmpty()) return true;
        return categoryNameMatchesLowered(c, filter.toLowerCase(Locale.ROOT));
    }

    // Takes an already-lowercased filter; loweredFilter MUST be .toLowerCase(Locale.ROOT).
    // Used in hot paths so the filter isn't re-lowercased per row.
    public static boolean matchesLowered(Waypoint w, Category c, String loweredFilter)
    {
        if (loweredFilter == null || loweredFilter.isEmpty()) return true;
        if (c != null && c.getName() != null
            && c.getName().toLowerCase(Locale.ROOT).contains(loweredFilter)) return true;
        if (w != null && w.getName() != null
            && w.getName().toLowerCase(Locale.ROOT).contains(loweredFilter)) return true;
        if (w != null && w.getNotes() != null
            && w.getNotes().toLowerCase(Locale.ROOT).contains(loweredFilter)) return true;
        return false;
    }

    public static boolean categoryNameMatchesLowered(Category c, String loweredFilter)
    {
        if (loweredFilter == null || loweredFilter.isEmpty()) return true;
        return c != null && c.getName() != null
            && c.getName().toLowerCase(Locale.ROOT).contains(loweredFilter);
    }
}
