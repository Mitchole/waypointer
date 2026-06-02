package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.CategorySortMode;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Memoized derived views over the live {@link Library}: the tier-sorted category list, the
 * per-category waypoint buckets, and the UUID->object indexes. Caches are lazy and dropped by
 * {@link WaypointStore#fireChanged()} on every mutation, so the store stays focused on
 * mutation + undo.
 */
final class LibraryViews
{
    private final Supplier<Library> library;

    private List<Category> cachedCategoriesOrdered;
    private Map<UUID, List<Waypoint>> cachedWaypointsByCategory;
    private Map<UUID, Category> categoryIndex;
    private Map<UUID, Waypoint> waypointIndex;

    LibraryViews(Supplier<Library> library)
    {
        this.library = library;
    }

    /** Drops every cache + index. Called from the store's fireChanged(). */
    void invalidate()
    {
        cachedCategoriesOrdered = null;
        cachedWaypointsByCategory = null;
        categoryIndex = null;
        waypointIndex = null;
    }

    /** Drops only the UUID indexes. Called mid-importMerge after a direct category mutation. */
    void invalidateIndexes()
    {
        categoryIndex = null;
        waypointIndex = null;
    }

    Category getCategoryById(UUID id)
    {
        if (categoryIndex == null)
        {
            Library lib = library.get();
            Map<UUID, Category> idx = new HashMap<>(lib.getCategories().size() * 2);
            for (Category c : lib.getCategories()) idx.put(c.getId(), c);
            categoryIndex = idx;
        }
        return categoryIndex.get(id);
    }

    Waypoint getWaypointById(UUID id)
    {
        if (waypointIndex == null)
        {
            Library lib = library.get();
            Map<UUID, Waypoint> idx = new HashMap<>(lib.getWaypoints().size() * 2);
            for (Waypoint w : lib.getWaypoints()) idx.put(w.getId(), w);
            waypointIndex = idx;
        }
        return waypointIndex.get(id);
    }

    List<Category> getCategoriesOrdered()
    {
        if (cachedCategoriesOrdered == null)
        {
            List<Category> sorted = new ArrayList<>(library.get().getCategories());
            sorted.sort((a, b) -> {
                int tierA = a.isUncategorized() ? 0 : (a.isBundled() ? 2 : 1);
                int tierB = b.isUncategorized() ? 0 : (b.isBundled() ? 2 : 1);
                if (tierA != tierB) return Integer.compare(tierA, tierB);
                return Integer.compare(a.getSortOrder(), b.getSortOrder());
            });
            cachedCategoriesOrdered = Collections.unmodifiableList(sorted);
        }
        return cachedCategoriesOrdered;
    }

    List<Waypoint> getWaypointsInCategory(UUID categoryId)
    {
        if (cachedWaypointsByCategory == null)
        {
            Map<UUID, List<Waypoint>> grouped = new HashMap<>();
            for (Waypoint w : library.get().getWaypoints())
            {
                grouped.computeIfAbsent(w.getCategoryId(), k -> new ArrayList<>()).add(w);
            }
            for (Map.Entry<UUID, List<Waypoint>> e : grouped.entrySet())
            {
                // getCategoryById may build categoryIndex as a side-effect; both caches self-init lazily.
                Category cat = getCategoryById(e.getKey());
                CategorySortMode mode = cat == null ? null : cat.getSortMode();
                e.getValue().sort(comparatorFor(mode));
            }
            cachedWaypointsByCategory = grouped;
        }
        List<Waypoint> bucket = cachedWaypointsByCategory.get(categoryId);
        return bucket == null ? Collections.emptyList() : Collections.unmodifiableList(bucket);
    }

    private static Comparator<Waypoint> comparatorFor(CategorySortMode mode)
    {
        if (mode == null || mode == CategorySortMode.MANUAL)
        {
            return Comparator.comparingInt(Waypoint::getSortOrder);
        }
        if (mode == CategorySortMode.NAME)
        {
            return Comparator
                .comparing((Waypoint w) -> w.getName() == null ? "" : w.getName().toLowerCase(Locale.ROOT))
                .thenComparing(w -> w.getCreatedAt() == null ? Instant.EPOCH : w.getCreatedAt())
                .thenComparing(Waypoint::getId);
        }
        return Comparator
            .comparing((Waypoint w) -> w.getCreatedAt() == null ? Instant.EPOCH : w.getCreatedAt(),
                Comparator.reverseOrder())
            .thenComparing(w -> w.getName() == null ? "" : w.getName().toLowerCase(Locale.ROOT))
            .thenComparing(Waypoint::getId);
    }
}
