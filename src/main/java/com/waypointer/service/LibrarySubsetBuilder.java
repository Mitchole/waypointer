package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a fresh {@link Library} holding a chosen subset of an existing library's waypoints and
 * categories. Stateless and side-effect-free: it never mutates {@code source}, and copies every
 * model object it carries over. Shared by the export picker, the import picker, and bulk export.
 */
public final class LibrarySubsetBuilder
{
    private LibrarySubsetBuilder() {}

    /**
     * @param source       library to copy from; left untouched.
     * @param waypointIds  ids of waypoints to include (null treated as empty).
     * @param categoryIds  ids of categories to include even when they carry no selected
     *                     waypoint (null treated as empty). A category referenced by an
     *                     included waypoint is pulled in regardless of this set, so the
     *                     receiving {@code importMerge} can rebind by name.
     * @return a new library (schemaVersion = CURRENT) with copied categories + waypoints.
     */
    public static Library build(Library source, Set<UUID> waypointIds, Set<UUID> categoryIds)
    {
        Set<UUID> wantWaypoints = waypointIds == null ? new HashSet<>() : waypointIds;
        Set<UUID> wantCategories = new HashSet<>(categoryIds == null ? new HashSet<>() : categoryIds);

        Library out = new Library();
        for (Waypoint w : source.getWaypoints())
        {
            if (wantWaypoints.contains(w.getId()))
            {
                out.getWaypoints().add(copyOf(w));
                wantCategories.add(w.getCategoryId());
            }
        }
        for (Category c : source.getCategories())
        {
            if (wantCategories.contains(c.getId()))
            {
                out.getCategories().add(copyOf(c));
            }
        }
        return out;
    }

    private static Waypoint copyOf(Waypoint w)
    {
        return new Waypoint(
            w.getId(), w.getName(), w.getPackedWorldPoint(), w.getCategoryId(),
            w.getIconId(), w.getNotes(), w.getCreatedAt(), w.getSortOrder(),
            w.isPinned(), w.getPinnedAt(), w.isBypassWildernessConfirm());
    }

    private static Category copyOf(Category c)
    {
        Category copy = new Category(c.getId(), c.getName(), c.getSortOrder(),
            c.isUncategorized(), c.getIconId(), c.isBundled());
        copy.setSortMode(c.getSortMode());
        return copy;
    }
}
