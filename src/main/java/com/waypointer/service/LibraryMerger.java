package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Merges an incoming {@link Library} into a target. Dedupes by id, rebinds incoming categoryIds by
 * name, and routes orphaned / Uncategorized waypoints to the target's Uncategorized sentinel. Pure
 * model mutation plus counting -- it never fires store listeners; the caller decides whether the
 * returned {@link WaypointStore.ImportResult} warrants a notify.
 */
final class LibraryMerger
{
    WaypointStore.ImportResult merge(Library target, LibraryViews views, Library incoming)
    {
        WaypointStore.ImportResult result = new WaypointStore.ImportResult();
        Map<UUID, UUID> categoryIdRemap = new HashMap<>();

        // Phase 1: categories
        for (Category c : incoming.getCategories())
        {
            if (c.isUncategorized()) continue; // never duplicate the sentinel
            Category existingById = views.getCategoryById(c.getId());
            if (existingById != null) continue;
            Category existingByName = views.getCategoryByName(c.getName());
            if (existingByName != null)
            {
                categoryIdRemap.put(c.getId(), existingByName.getId());
            }
            else
            {
                int nextOrder = target.getCategories().stream()
                    .mapToInt(Category::getSortOrder).max().orElse(-1) + 1;
                target.getCategories().add(new Category(
                    c.getId(), c.getName(), nextOrder, false, c.getIconId(), c.isBundled()));
                result.categoriesAdded++;
            }
        }

        // Phase 1 mutated target.getCategories() directly, so the categoryIndex cache populated by
        // the getCategoryById call above is now stale. Without this, Phase 2's category-exists check
        // returns null for every freshly-added category and the waypoint falls through to Uncategorized.
        views.invalidateIndexes();

        // Phase 2: waypoints
        Set<UUID> existingWpIds = target.getWaypoints().stream()
            .map(Waypoint::getId).collect(Collectors.toCollection(HashSet::new));
        for (Waypoint w : incoming.getWaypoints())
        {
            if (existingWpIds.contains(w.getId()))
            {
                result.waypointsSkipped++;
                continue;
            }
            UUID resolvedCat = categoryIdRemap.getOrDefault(w.getCategoryId(), w.getCategoryId());
            // If incoming categoryId is the Uncategorized sentinel id from the source side, map it
            // to OUR uncategorized id.
            Category srcCat = findInList(incoming.getCategories(), w.getCategoryId());
            if (srcCat != null && srcCat.isUncategorized())
            {
                resolvedCat = views.getUncategorized().getId();
            }
            if (views.getCategoryById(resolvedCat) == null)
            {
                resolvedCat = views.getUncategorized().getId();
            }
            int sortOrder = views.nextWaypointSortOrder(resolvedCat);
            target.getWaypoints().add(new Waypoint(
                w.getId(), w.getName(), w.getPackedWorldPoint(),
                resolvedCat, w.getIconId(), w.getNotes() == null ? "" : w.getNotes(),
                w.getCreatedAt() == null ? Instant.now() : w.getCreatedAt(),
                sortOrder, false, null, false));
            result.waypointsAdded++;
        }

        result.categoriesMerged = categoryIdRemap.size();
        return result;
    }

    private static Category findInList(List<Category> list, UUID id)
    {
        for (Category c : list) if (c.getId().equals(id)) return c;
        return null;
    }
}
