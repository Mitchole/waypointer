package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.CategorySortMode;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.util.Listeners;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * In-memory CRUD over a {@link Library}. Mutations are confined to the EDT and notify
 * registered listeners synchronously. Persistence is layered on by
 * {@link WaypointStorePersistence}.
 */
@Slf4j
@Singleton
public class WaypointStore
{
    private static final String UNCATEGORIZED_NAME = "Uncategorized";

    /**
     * Live in-memory library. {@code volatile} for safe publication of the reference to the
     * render thread's NPC-name snapshot reads; multi-step mutations remain EDT-confined.
     */
    private volatile Library library = new Library();
    private final Listeners listeners = new Listeners();
    private final LibraryViews views = new LibraryViews(() -> library);
    private final LibraryMerger merger = new LibraryMerger();

    // Packed-point -> NPC name for waypoints that target an NPC. Read from the client thread by
    // NpcHighlightOverlay.render() (~50fps); rebuilt eagerly on every mutation in fireChanged().
    // volatile so the eagerly-built reference publishes safely to the render thread without the
    // overlay copying the live (EDT-mutated) waypoint list each frame.
    private volatile Map<Integer, String> npcNamesByPacked = Collections.emptyMap();

    /**
     * Single-slot undo buffer. Holds the inverse of the most recent destructive op
     * ({@link #deleteWaypoint(UUID)}, {@link #deleteWaypoints(Collection)},
     * {@link #updateWaypointPoint(UUID, int)}, {@link #deleteCategory(UUID, boolean)}), each of
     * which arms it via {@link #armUndoAndNotify(Runnable)}. Every other successful mutation goes
     * through {@link #notifyChanged()}, which clears it.
     */
    private final UndoBuffer undo = new UndoBuffer();

    /**
     * Default notify path for every mutation: clears the undo slot, then fires. Clearing here
     * makes "no undo" the default for any successful mutation that fires listeners. Methods that
     * want their inverse to survive must opt in via {@link #armUndoAndNotify(Runnable)} instead.
     */
    private void notifyChanged()
    {
        undo.clear();
        fireChanged();
    }

    /** Arms the single-slot undo buffer with {@code inverse}, then fires without clearing it. */
    private void armUndoAndNotify(Runnable inverse)
    {
        undo.arm(inverse);
        fireChanged();
    }

    private void fireChanged()
    {
        views.invalidate();
        rebuildNpcNameSnapshot();
        listeners.fire();
    }

    // Eagerly rebuild the packed->NPC-name snapshot on the mutating thread (the live list is only
    // touched here, where mutation is single-threaded). Most waypoints are plain tiles, so the map
    // is typically tiny. Published via the volatile field for the render thread.
    private void rebuildNpcNameSnapshot()
    {
        Map<Integer, String> snapshot = new HashMap<>();
        for (Waypoint w : library.getWaypoints())
        {
            // If two NPC waypoints share a packed tile, last in list order wins. Duplicates are
            // rare and either choice is arbitrary; this just avoids a surprised future reader.
            if (w.getTargetNpcName() != null)
            {
                snapshot.put(w.getPackedWorldPoint(), w.getTargetNpcName());
            }
        }
        npcNamesByPacked = Collections.unmodifiableMap(snapshot);
    }

    @Inject
    public WaypointStore() {}

    /** Initializes the store with a library, ensuring the Uncategorized sentinel is present. */
    public void bootstrap(Library lib)
    {
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

    /**
     * Current packed-point -> NPC-name snapshot. Backed by a volatile reference rebuilt on every
     * mutation, so it is safe to read from the client thread. Never mutated after publication.
     */
    public Map<Integer, String> npcNamesSnapshot()
    {
        return npcNamesByPacked;
    }

    public Category getUncategorized()
    {
        return views.getUncategorized();
    }

    public Category getCategoryById(UUID id) { return views.getCategoryById(id); }

    public Category getCategoryByName(String name)
    {
        return views.getCategoryByName(name);
    }

    public Waypoint getWaypointById(UUID id) { return views.getWaypointById(id); }

    /** True if any saved waypoint sits on {@code packed}. Used to suppress the path banner when
     *  the active target already has a row whose Play/Stop button covers it. */
    public boolean hasWaypointAt(int packed)
    {
        return library.getWaypoints().stream().anyMatch(w -> w.getPackedWorldPoint() == packed);
    }

    public List<Category> getCategoriesOrdered() { return views.getCategoriesOrdered(); }

    public List<Waypoint> getWaypointsInCategory(UUID categoryId) { return views.getWaypointsInCategory(categoryId); }

    public Category createCategory(String name)
    {
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
            return;
        }
        if (c.isUncategorized())
        {
            throw new IllegalArgumentException("Cannot delete Uncategorized");
        }
        Category snapshotCategory = c;
        Runnable inverse;
        if (moveChildrenToUncategorized)
        {
            // Capture each affected waypoint's previous categoryId AND sortOrder so the
            // undo restores them exactly. The destructive path overwrites BOTH below.
            List<Waypoint> affected = new ArrayList<>(getWaypointsInCategory(id));
            Map<UUID, Integer> prevSort = new HashMap<>();
            for (Waypoint w : affected) prevSort.put(w.getId(), w.getSortOrder());

            UUID uId = getUncategorized().getId();
            int nextOrder = views.nextWaypointSortOrder(uId);
            for (Waypoint w : affected)
            {
                w.setCategoryId(uId);
                w.setSortOrder(nextOrder++);
            }
            library.getCategories().removeIf(cc -> cc.getId().equals(id));

            UUID origCategoryId = id;
            inverse = () -> {
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

            inverse = () -> {
                library.getCategories().add(snapshotCategory);
                library.getWaypoints().addAll(deletedChildren);
                notifyChanged();
            };
        }
        armUndoAndNotify(inverse);
    }

    public void setCategoryIcon(UUID categoryId, Integer iconId)
    {
        Category c = getCategoryById(categoryId);
        if (c == null) return;
        c.setIconId(iconId);
        notifyChanged();
    }

    public void setCategoryColor(UUID categoryId, Integer color)
    {
        Category c = getCategoryById(categoryId);
        if (c == null) return;
        c.setColor(color);
        notifyChanged();
    }

    public void setCategorySortMode(UUID categoryId, CategorySortMode mode)
    {
        Category c = getCategoryById(categoryId);
        if (c == null) return;
        c.setSortMode(mode);
        notifyChanged();
    }

    public void reorderCategories(List<UUID> idsInNewOrder)
    {
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
        return createWaypoint(packed, name, categoryId, null);
    }

    public Waypoint createWaypoint(int packed, String name, UUID categoryId, String targetNpcName)
    {
        Waypoint w = new Waypoint(
            UUID.randomUUID(),
            name,
            packed,
            categoryId,
            null,
            "",
            Instant.now(),
            views.nextWaypointSortOrder(categoryId),
            false,
            null,
            false);
        w.setTargetNpcName(targetNpcName);
        library.getWaypoints().add(w);
        notifyChanged();
        return w;
    }

    public void renameWaypoint(UUID id, String newName)
    {
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        w.setName(newName);
        notifyChanged();
    }

    public void updateWaypointIcon(UUID id, Integer iconId)
    {
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        w.setIconId(iconId);
        notifyChanged();
    }

    public void setWaypointPinned(UUID id, boolean pinned)
    {
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        if (pinned == w.isPinned()) return;
        w.setPinned(pinned);
        w.setPinnedAt(pinned ? Instant.now() : null);
        notifyChanged();
    }

    public void setWaypointBypassWildernessConfirm(UUID id, boolean bypass)
    {
        Waypoint w = getWaypointById(id);
        if (w == null) return;
        if (bypass == w.isBypassWildernessConfirm()) return;
        w.setBypassWildernessConfirm(bypass);
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
            return;
        }
        int oldPacked = w.getPackedWorldPoint();
        w.setPackedWorldPoint(packed);
        UUID targetId = id;
        armUndoAndNotify(() -> {
            Waypoint cur = getWaypointById(targetId);
            if (cur != null)
            {
                cur.setPackedWorldPoint(oldPacked);
                notifyChanged();
            }
        });
    }

    public void deleteWaypoint(UUID id)
    {
        Waypoint w = getWaypointById(id);
        if (w == null)
        {
            return;
        }
        library.getWaypoints().removeIf(x -> x.getId().equals(id));
        Waypoint snapshot = w;
        armUndoAndNotify(() -> {
            library.getWaypoints().add(snapshot);
            notifyChanged();
        });
    }

    /**
     * Batch delete. Snapshots every found waypoint and arms ONE undo that re-adds them all;
     * unknown ids are skipped, and if none are found the undo slot is cleared without firing.
     * Mirrors the single-item {@link #deleteWaypoint(UUID)}.
     */
    public void deleteWaypoints(Collection<UUID> ids)
    {
        Set<UUID> idSet = new HashSet<>(ids);
        List<Waypoint> removed = new ArrayList<>();
        for (Waypoint w : library.getWaypoints())
        {
            if (idSet.contains(w.getId())) removed.add(w);
        }
        if (removed.isEmpty())
        {
            return;
        }
        library.getWaypoints().removeIf(w -> idSet.contains(w.getId()));
        armUndoAndNotify(() -> {
            library.getWaypoints().addAll(removed);
            notifyChanged();
        });
    }

    public void moveWaypointToCategory(UUID waypointId, UUID newCategoryId)
    {
        Waypoint w = getWaypointById(waypointId);
        if (w == null) return;
        if (getCategoryById(newCategoryId) == null) return;
        w.setCategoryId(newCategoryId);
        w.setSortOrder(views.nextWaypointSortOrder(newCategoryId));
        notifyChanged();
    }

    /**
     * Batch reparent. Validates the target once; each found waypoint is moved to the tail of
     * {@code targetId} with ascending sortOrder. Unknown ids are skipped. Non-undoable, matching
     * the single-item {@link #moveWaypointToCategory(UUID, UUID)}.
     */
    public void moveWaypointsToCategory(Collection<UUID> ids, UUID targetId)
    {
        if (getCategoryById(targetId) == null) return;
        int order = views.nextWaypointSortOrder(targetId);
        boolean any = false;
        for (UUID id : ids)
        {
            Waypoint w = getWaypointById(id);
            if (w == null) continue;
            w.setCategoryId(targetId);
            w.setSortOrder(order++);
            any = true;
        }
        if (any) notifyChanged();
    }

    public void reorderWithinCategory(UUID categoryId, List<UUID> waypointIdsInOrder)
    {
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
        ImportResult result = merger.merge(library, views, incoming);
        // Also fire when only categories were added or matched by name: a categories-only import
        // would otherwise skip the rebuild + save and the new categories would vanish on reload.
        if (result.waypointsAdded > 0 || result.categoriesAdded > 0 || result.categoriesMerged > 0)
        {
            notifyChanged();
        }
        return result;
    }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public boolean hasUndoable() { return undo.hasUndoable(); }

    /**
     * Run the inverse of the most recent destructive op, if any. Clears the slot before
     * running so the runnable's direct-to-library writes don't re-clear it themselves.
     */
    public void undoLast()
    {
        undo.runAndClear();
    }

    // Package-private test seam: arm the slot with an arbitrary runnable so tests can
    // verify that non-undoable mutations clear it. Production code never calls this.
    void testArmUndoSlot(Runnable r) { undo.arm(r); }

    /** Test seam: number of currently-subscribed listeners. */
    public int listenerCountForTest()
    {
        return listeners.size();
    }

    // ---- Persistence wiring (write-through) ----

    private Listeners.Subscription saveSub;

    /**
     * Subscribe a write-through saver fired synchronously on every mutation. Idempotent. The
     * saver should read the live library (e.g. {@code () -> persistence.save(getLibrary())}); a
     * later {@link #bootstrap(Library)} that swaps the library is picked up automatically.
     */
    public void enablePersistence(Runnable saver)
    {
        if (saveSub != null) return;
        saveSub = listeners.subscribe(saver);
    }

    /**
     * Detach the saver. Idempotent. Called from {@link com.waypointer.WaypointerPlugin#shutDown()}
     * so plugin reload cycles do not stack savers.
     */
    public void disablePersistence()
    {
        if (saveSub != null)
        {
            saveSub.close();
            saveSub = null;
        }
    }

    private void ensureUncategorized()
    {
        Category existing = library.getCategories().stream()
            .filter(Category::isUncategorized).findFirst().orElse(null);
        if (existing == null)
        {
            Category u = new Category(UUID.randomUUID(), UNCATEGORIZED_NAME, 0, true, null, false);
            // Top-of-list ordering is enforced later by getCategoriesOrdered's tier sort, not here.
            library.getCategories().add(u);
        }
    }

    public static class ImportResult
    {
        public int waypointsAdded;
        public int waypointsSkipped;
        public int categoriesAdded;
        public int categoriesMerged;
    }

}
