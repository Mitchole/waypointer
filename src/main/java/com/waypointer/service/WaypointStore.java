package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.util.Listeners;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Single-threaded in-memory CRUD over a {@link Library}. All mutations notify registered
 * listeners synchronously. Persistence is layered on by {@link WaypointStorePersistence}.
 */
@Slf4j
@Singleton
public class WaypointStore
{
    private static final String UNCATEGORIZED_NAME = "Uncategorized";

    private Library library = new Library();
    private final Listeners listeners = new Listeners();

    // Memoized derived views; invalidated by every mutation via notifyChanged().
    private List<Category> cachedCategoriesOrdered;
    private Map<UUID, List<Waypoint>> cachedWaypointsByCategory;

    // Lazy-rebuilt UUID->object indexes; null means "dirty, rebuild on next access".
    private Map<UUID, Category> categoryIndex;
    private Map<UUID, Waypoint> waypointIndex;

    /**
     * Single-slot undo buffer. Holds the inverse of the most recent destructive op
     * ({@link #deleteWaypoint(UUID)}, {@link #updateWaypointPoint(UUID, int)},
     * {@link #deleteCategory(UUID, boolean)}). Any other public mutation method clears
     * the slot. Survives toast hide; does not survive plugin disable/enable.
     */
    private Runnable lastUndo;

    private void invalidateIndexes()
    {
        categoryIndex = null;
        waypointIndex = null;
    }

    private void notifyChanged()
    {
        cachedCategoriesOrdered = null;
        cachedWaypointsByCategory = null;
        invalidateIndexes();
        listeners.fire();
    }

    @Inject
    public WaypointStore() {}

    /** Initializes the store with a library, ensuring the Uncategorized sentinel is present. */
    public void bootstrap(Library lib)
    {
        lastUndo = null;
        this.library = lib;
        ensureUncategorized();
        // Notify so any listener attached before bootstrap (e.g. the panel built by Guice
        // before WaypointerPlugin.startUp() calls bootstrap with the loaded library) re-renders
        // against the freshly-loaded data.
        notifyChanged();
    }

    // Live in-memory library. NOT a defensive copy; callers that mutate it bypass listeners
    // and risk corrupting state. Read-only outside of test fixtures.
    public Library getLibrary() { return library; }

    public Category getUncategorized()
    {
        return library.getCategories().stream()
            .filter(Category::isUncategorized)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Uncategorized sentinel missing"));
    }

    public Category getCategoryById(UUID id)
    {
        if (categoryIndex == null)
        {
            Map<UUID, Category> idx = new HashMap<>(library.getCategories().size() * 2);
            for (Category c : library.getCategories()) idx.put(c.getId(), c);
            categoryIndex = idx;
        }
        return categoryIndex.get(id);
    }

    public Category getCategoryByName(String name)
    {
        return library.getCategories().stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    public Waypoint getWaypointById(UUID id)
    {
        if (waypointIndex == null)
        {
            Map<UUID, Waypoint> idx = new HashMap<>(library.getWaypoints().size() * 2);
            for (Waypoint w : library.getWaypoints()) idx.put(w.getId(), w);
            waypointIndex = idx;
        }
        return waypointIndex.get(id);
    }

    public List<Category> getCategoriesOrdered()
    {
        if (cachedCategoriesOrdered == null)
        {
            List<Category> sorted = new ArrayList<>(library.getCategories());
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

    public List<Waypoint> getWaypointsInCategory(UUID categoryId)
    {
        if (cachedWaypointsByCategory == null)
        {
            Map<UUID, List<Waypoint>> grouped = new HashMap<>();
            for (Waypoint w : library.getWaypoints())
            {
                grouped.computeIfAbsent(w.getCategoryId(), k -> new ArrayList<>()).add(w);
            }
            for (List<Waypoint> bucket : grouped.values())
            {
                bucket.sort(Comparator.comparingInt(Waypoint::getSortOrder));
            }
            cachedWaypointsByCategory = grouped;
        }
        List<Waypoint> bucket = cachedWaypointsByCategory.get(categoryId);
        return bucket == null ? Collections.emptyList() : Collections.unmodifiableList(bucket);
    }

    public Category createCategory(String name)
    {
        lastUndo = null;
        if (getCategoryByName(name) != null)
        {
            throw new IllegalArgumentException("Category name already exists: " + name);
        }
        int nextOrder = library.getCategories().stream()
            .mapToInt(Category::getSortOrder).max().orElse(-1) + 1;
        Category c = new Category(UUID.randomUUID(), name, nextOrder, false, null, false);
        library.getCategories().add(c);
        notifyChanged();
        return c;
    }

    public void renameCategory(UUID id, String newName)
    {
        lastUndo = null;
        Category c = getCategoryById(id);
        if (c == null) return;
        if (c.isUncategorized())
        {
            throw new IllegalArgumentException("Cannot rename Uncategorized");
        }
        Category collision = getCategoryByName(newName);
        if (collision != null && !collision.getId().equals(id))
        {
            throw new IllegalArgumentException("Category name already exists: " + newName);
        }
        c.setName(newName);
        notifyChanged();
    }

    public void deleteCategory(UUID id, boolean moveChildrenToUncategorized)
    {
        Category c = getCategoryById(id);
        if (c == null)
        {
            lastUndo = null;
            return;
        }
        if (c.isUncategorized())
        {
            throw new IllegalArgumentException("Cannot delete Uncategorized");
        }
        Category snapshotCategory = c;
        if (moveChildrenToUncategorized)
        {
            // Capture each affected waypoint's previous categoryId AND sortOrder so the
            // undo restores them exactly. The destructive path overwrites BOTH below.
            List<Waypoint> affected = new ArrayList<>(getWaypointsInCategory(id));
            Map<UUID, Integer> prevSort = new HashMap<>();
            for (Waypoint w : affected) prevSort.put(w.getId(), w.getSortOrder());

            UUID uId = getUncategorized().getId();
            int nextOrder = nextWaypointSortOrder(uId);
            for (Waypoint w : affected)
            {
                w.setCategoryId(uId);
                w.setSortOrder(nextOrder++);
            }
            library.getCategories().removeIf(cc -> cc.getId().equals(id));

            UUID origCategoryId = id;
            lastUndo = () -> {
                library.getCategories().add(snapshotCategory);
                for (Waypoint w : affected)
                {
                    w.setCategoryId(origCategoryId);
                    Integer s = prevSort.get(w.getId());
                    if (s != null) w.setSortOrder(s);
                }
                notifyChanged();
            };
        }
        else
        {
            List<Waypoint> deletedChildren = new ArrayList<>();
            for (Waypoint w : library.getWaypoints())
            {
                if (w.getCategoryId().equals(id)) deletedChildren.add(w);
            }
            library.getWaypoints().removeIf(w -> w.getCategoryId().equals(id));
            library.getCategories().removeIf(cc -> cc.getId().equals(id));

            lastUndo = () -> {
                library.getCategories().add(snapshotCategory);
                library.getWaypoints().addAll(deletedChildren);
                notifyChanged();
            };
        }
        notifyChanged();
    }

    public void setCategoryIcon(UUID categoryId, Integer iconId)
    {
        lastUndo = null;
        Category c = getCategoryById(categoryId);
        if (c == null) return;
        c.setIconId(iconId);
        notifyChanged();
    }

    public void reorderCategories(List<UUID> idsInNewOrder)
    {
        lastUndo = null;
        Map<UUID, Integer> rank = new HashMap<>();
        for (int i = 0; i < idsInNewOrder.size(); i++) rank.put(idsInNewOrder.get(i), i);
        for (Category c : library.getCategories())
        {
            Integer r = rank.get(c.getId());
            if (r != null) c.setSortOrder(r);
        }
        notifyChanged();
    }

    public Waypoint createWaypoint(int packed, String name, UUID categoryId)
    {
        lastUndo = null;
        Waypoint w = new Waypoint(
            UUID.randomUUID(),
            name,
            packed,
            categoryId,
            null,
            "",
            Instant.now(),
            nextWaypointSortOrder(categoryId),
            false,
            null,
            false);
        library.getWaypoints().add(w);
        notifyChanged();
        return w;
    }

    public void renameWaypoint(UUID id, String newName)
    {
        lastUndo = null;
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        w.setName(newName);
        notifyChanged();
    }

    public void updateWaypointIcon(UUID id, Integer iconId)
    {
        lastUndo = null;
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        w.setIconId(iconId);
        notifyChanged();
    }

    public void setWaypointPinned(UUID id, boolean pinned)
    {
        lastUndo = null;
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        if (pinned == w.isPinned()) return;
        w.setPinned(pinned);
        w.setPinnedAt(pinned ? Instant.now() : null);
        notifyChanged();
    }

    /**
     * Derived view: all pinned waypoints sorted by {@code pinnedAt}.
     *
     * @param newestAtTop true = descending (most recently pinned first);
     *                    false = ascending (most recently pinned last).
     *                    Waypoints with null pinnedAt sort as Instant.EPOCH.
     */
    public List<Waypoint> getPinnedWaypoints(boolean newestAtTop)
    {
        List<Waypoint> pinned = new ArrayList<>();
        for (Waypoint w : library.getWaypoints())
        {
            if (w.isPinned()) pinned.add(w);
        }
        Comparator<Waypoint> byPinnedAt = Comparator.comparing(
            w -> w.getPinnedAt() == null ? Instant.EPOCH : w.getPinnedAt());
        pinned.sort(newestAtTop ? byPinnedAt.reversed() : byPinnedAt);
        return Collections.unmodifiableList(pinned);
    }

    public void updateWaypointNotes(UUID id, String notes)
    {
        lastUndo = null;
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        w.setNotes(notes == null ? "" : notes);
        notifyChanged();
    }

    public void updateWaypointPoint(UUID id, int packed)
    {
        Waypoint w = getWaypointById(id);
        if (w == null)
        {
            lastUndo = null;
            return;
        }
        int oldPacked = w.getPackedWorldPoint();
        w.setPackedWorldPoint(packed);
        UUID targetId = id;
        lastUndo = () -> {
            Waypoint cur = getWaypointById(targetId);
            if (cur != null)
            {
                cur.setPackedWorldPoint(oldPacked);
                notifyChanged();
            }
        };
        notifyChanged();
    }

    public void deleteWaypoint(UUID id)
    {
        Waypoint w = getWaypointById(id);
        if (w == null)
        {
            lastUndo = null;
            return;
        }
        library.getWaypoints().removeIf(x -> x.getId().equals(id));
        Waypoint snapshot = w;
        lastUndo = () -> {
            library.getWaypoints().add(snapshot);
            notifyChanged();
        };
        notifyChanged();
    }

    public void moveWaypointToCategory(UUID waypointId, UUID newCategoryId)
    {
        lastUndo = null;
        Waypoint w = getWaypointById(waypointId);
        if (w == null) return;
        if (getCategoryById(newCategoryId) == null) return;
        w.setCategoryId(newCategoryId);
        w.setSortOrder(nextWaypointSortOrder(newCategoryId));
        notifyChanged();
    }

    public void reorderWithinCategory(UUID categoryId, List<UUID> waypointIdsInOrder)
    {
        lastUndo = null;
        Map<UUID, Integer> rank = new HashMap<>();
        for (int i = 0; i < waypointIdsInOrder.size(); i++) rank.put(waypointIdsInOrder.get(i), i);
        for (Waypoint w : library.getWaypoints())
        {
            if (!w.getCategoryId().equals(categoryId)) continue;
            Integer r = rank.get(w.getId());
            if (r != null) w.setSortOrder(r);
        }
        notifyChanged();
    }

    /** Merge another library into this one. Dedupe by id; rebind incoming categoryIds by name. */
    public ImportResult importMerge(Library incoming)
    {
        lastUndo = null;
        ImportResult result = new ImportResult();
        Map<UUID, UUID> categoryIdRemap = new HashMap<>();

        // Phase 1: categories
        for (Category c : incoming.getCategories())
        {
            if (c.isUncategorized()) continue; // never duplicate the sentinel
            Category existingById = getCategoryById(c.getId());
            if (existingById != null) continue;
            Category existingByName = getCategoryByName(c.getName());
            if (existingByName != null)
            {
                categoryIdRemap.put(c.getId(), existingByName.getId());
            }
            else
            {
                int nextOrder = library.getCategories().stream()
                    .mapToInt(Category::getSortOrder).max().orElse(-1) + 1;
                library.getCategories().add(new Category(
                    c.getId(), c.getName(), nextOrder, false, c.getIconId(), c.isBundled()));
                result.categoriesAdded++;
            }
        }

        // Phase 1 mutated library.getCategories() directly, so the categoryIndex cache
        // populated by the getCategoryById call above is now stale. Without this, Phase 2's
        // category-exists check below returns null for every freshly-added category and the
        // waypoint falls through to Uncategorized.
        invalidateIndexes();

        // Phase 2: waypoints
        Set<UUID> existingWpIds = library.getWaypoints().stream()
            .map(Waypoint::getId).collect(Collectors.toCollection(HashSet::new));
        for (Waypoint w : incoming.getWaypoints())
        {
            if (existingWpIds.contains(w.getId()))
            {
                result.waypointsSkipped++;
                continue;
            }
            UUID resolvedCat = categoryIdRemap.getOrDefault(w.getCategoryId(), w.getCategoryId());
            // If incoming categoryId is the Uncategorized sentinel id from the source side, map
            // it to OUR uncategorized id.
            Category srcCat = findInList(incoming.getCategories(), w.getCategoryId());
            if (srcCat != null && srcCat.isUncategorized())
            {
                resolvedCat = getUncategorized().getId();
            }
            if (getCategoryById(resolvedCat) == null)
            {
                resolvedCat = getUncategorized().getId();
            }
            int sortOrder = nextWaypointSortOrder(resolvedCat);
            library.getWaypoints().add(new Waypoint(
                w.getId(), w.getName(), w.getPackedWorldPoint(),
                resolvedCat, w.getIconId(), w.getNotes() == null ? "" : w.getNotes(),
                w.getCreatedAt() == null ? Instant.now() : w.getCreatedAt(),
                sortOrder,
                false,
                null,
                false));
            result.waypointsAdded++;
        }

        // Also fire when only categories were added: a categories-only import (e.g. defaults
        // with an empty waypoints list) would otherwise skip the rebuild + save and the new
        // categories would vanish on the next plugin reload.
        if (result.waypointsAdded > 0 || result.categoriesAdded > 0 || !categoryIdRemap.isEmpty())
        {
            notifyChanged();
        }
        return result;
    }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public boolean hasUndoable() { return lastUndo != null; }

    /**
     * Run the inverse of the most recent destructive op, if any. Clears the slot before
     * running so the runnable's direct-to-library writes don't re-clear it themselves.
     */
    public void undoLast()
    {
        Runnable u = lastUndo;
        lastUndo = null;
        if (u != null) u.run();
    }

    // Package-private test seam: arm the slot with an arbitrary runnable so tests can
    // verify that non-undoable mutations clear it. Production code never calls this.
    void testArmUndoSlot(Runnable r) { this.lastUndo = r; }

    // ---- Debounced persistence wiring ----

    private WaypointStorePersistence persistence;
    private ScheduledExecutorService scheduler;
    private Duration debounce;
    private volatile ScheduledFuture<?> pendingSave;
    private Listeners.Subscription saveSub;

    public void enableDebouncedPersistence(
        WaypointStorePersistence p,
        ScheduledExecutorService exec,
        Duration debounceWindow)
    {
        if (saveSub != null) return;
        this.persistence = p;
        this.scheduler = exec;
        this.debounce = debounceWindow;
        this.saveSub = listeners.subscribe(this::scheduleSave);
    }

    /**
     * Detaches the persistence subscription, cancels any pending debounced save, and stops
     * further saves from being scheduled by mutations. Idempotent. Used by tests and by
     * {@link com.waypointer.WaypointerPlugin#shutDown()} so plugin reload cycles don't leave
     * dangling listeners or orphaned scheduled tasks behind.
     */
    public void disableDebouncedPersistence()
    {
        if (saveSub != null)
        {
            saveSub.close();
            saveSub = null;
        }
        if (pendingSave != null && !pendingSave.isDone()) pendingSave.cancel(false);
    }

    private void scheduleSave()
    {
        if (persistence == null || scheduler == null) return;
        if (pendingSave != null && !pendingSave.isDone()) pendingSave.cancel(false);
        // Snapshot the library to JSON on the calling thread (mutations happen here, so
        // iteration is safe). The scheduler thread only writes the frozen bytes. Avoids
        // ConcurrentModificationException during gson iteration vs. a parallel mutation.
        final String json = persistence.serialize(library);
        pendingSave = scheduler.schedule(
            () -> {
                boolean ok = persistence.writeBlocking(json);
                if (!ok) log.warn("Library save failed");
            },
            debounce.toMillis(),
            TimeUnit.MILLISECONDS);
    }

    /** Cancels any pending debounced save, then writes the current library synchronously. */
    public void flushPendingSave()
    {
        if (pendingSave != null && !pendingSave.isDone()) pendingSave.cancel(false);
        if (persistence != null)
        {
            boolean ok = persistence.saveBlocking(library);
            if (!ok) log.warn("Library save failed");
        }
    }

    private int nextWaypointSortOrder(UUID categoryId)
    {
        return library.getWaypoints().stream()
            .filter(w -> w.getCategoryId().equals(categoryId))
            .mapToInt(Waypoint::getSortOrder).max().orElse(-1) + 1;
    }

    private void ensureUncategorized()
    {
        Category existing = library.getCategories().stream()
            .filter(Category::isUncategorized).findFirst().orElse(null);
        if (existing == null)
        {
            Category u = new Category(UUID.randomUUID(), UNCATEGORIZED_NAME, 0, true, null, false);
            // Push existing categories out by 1 so Uncategorized can sit at top.
            library.getCategories().add(u);
        }
    }

    private static Category findInList(List<Category> list, UUID id)
    {
        for (Category c : list) if (c.getId().equals(id)) return c;
        return null;
    }

    public static class ImportResult
    {
        public int waypointsAdded;
        public int waypointsSkipped;
        public int categoriesAdded;
    }

}
