package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaypointStoreTest
{
    private WaypointStore store;

    @Before
    public void setUp()
    {
        store = new WaypointStore();
        store.bootstrap(new Library());
    }

    @Test
    public void bootstrapEnsuresUncategorizedSentinel()
    {
        Category u = store.getUncategorized();
        assertNotNull(u);
        assertTrue(u.isUncategorized());
        assertEquals("Uncategorized", u.getName());
    }

    @Test
    public void createWaypointAddsToUncategorizedAndAssignsSortOrder()
    {
        Waypoint w1 = store.createWaypoint(100, "First", store.getUncategorized().getId());
        Waypoint w2 = store.createWaypoint(200, "Second", store.getUncategorized().getId());
        assertEquals(2, store.getWaypointsInCategory(store.getUncategorized().getId()).size());
        assertTrue(w1.getSortOrder() < w2.getSortOrder());
    }

    @Test
    public void deleteWaypointRemoves()
    {
        Waypoint w = store.createWaypoint(100, "Delete me", store.getUncategorized().getId());
        store.deleteWaypoint(w.getId());
        assertNull(store.getWaypointById(w.getId()));
    }

    @Test
    public void renameWaypointUpdatesName()
    {
        Waypoint w = store.createWaypoint(100, "Old", store.getUncategorized().getId());
        store.renameWaypoint(w.getId(), "New");
        assertEquals("New", store.getWaypointById(w.getId()).getName());
    }

    @Test
    public void reorderWaypointsRespectsNewOrder()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        Waypoint c = store.createWaypoint(3, "C", uId);

        // Move C to front: new order [C, A, B]
        store.reorderWithinCategory(uId, java.util.Arrays.asList(c.getId(), a.getId(), b.getId()));

        List<Waypoint> after = store.getWaypointsInCategory(uId);
        assertEquals("C", after.get(0).getName());
        assertEquals("A", after.get(1).getName());
        assertEquals("B", after.get(2).getName());
    }

    @Test
    public void moveWaypointToDifferentCategoryAppendsAtEnd()
    {
        UUID uId = store.getUncategorized().getId();
        Category bossing = store.createCategory("Bossing");
        Waypoint w = store.createWaypoint(99, "W", uId);
        store.moveWaypointToCategory(w.getId(), bossing.getId());
        assertTrue(store.getWaypointsInCategory(uId).isEmpty());
        assertEquals(1, store.getWaypointsInCategory(bossing.getId()).size());
        assertEquals(bossing.getId(), store.getWaypointById(w.getId()).getCategoryId());
    }

    @Test
    public void deleteCategoryWithMoveToUncategorizedReassigns()
    {
        Category bossing = store.createCategory("Bossing");
        Waypoint w = store.createWaypoint(1, "W", bossing.getId());
        store.deleteCategory(bossing.getId(), true);
        assertNull(store.getCategoryById(bossing.getId()));
        assertEquals(store.getUncategorized().getId(),
            store.getWaypointById(w.getId()).getCategoryId());
    }

    @Test
    public void deleteCategoryCascadeRemovesChildren()
    {
        Category bossing = store.createCategory("Bossing");
        Waypoint w = store.createWaypoint(1, "W", bossing.getId());
        store.deleteCategory(bossing.getId(), false);
        assertNull(store.getCategoryById(bossing.getId()));
        assertNull(store.getWaypointById(w.getId()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotDeleteUncategorized()
    {
        store.deleteCategory(store.getUncategorized().getId(), true);
    }

    @Test
    public void renameCategoryRejectsDuplicateName()
    {
        store.createCategory("Bossing");
        try
        {
            store.createCategory("Bossing");
            fail("expected duplicate name rejection");
        }
        catch (IllegalArgumentException expected) {}
    }

    @Test
    public void observersNotifiedOnMutation()
    {
        AtomicInteger calls = new AtomicInteger();
        store.subscribe(() -> calls.incrementAndGet());
        store.createWaypoint(1, "W", store.getUncategorized().getId());
        assertEquals(1, calls.get());
        store.renameWaypoint(store.getWaypointsInCategory(store.getUncategorized().getId())
            .get(0).getId(), "Renamed");
        assertEquals(2, calls.get());
    }

    @Test
    public void bundledCategoriesSortAfterUserCategories()
    {
        Category bundledA = new Category(UUID.randomUUID(), "BundledA", 0, false, null, true);
        Category bundledB = new Category(UUID.randomUUID(), "BundledB", 1, false, null, true);
        Category userZ = new Category(UUID.randomUUID(), "UserZ", 50, false, null, false);
        Category userA = new Category(UUID.randomUUID(), "UserA", 100, false, null, false);
        store.getLibrary().getCategories().add(bundledA);
        store.getLibrary().getCategories().add(bundledB);
        store.getLibrary().getCategories().add(userZ);
        store.getLibrary().getCategories().add(userA);

        List<Category> ordered = store.getCategoriesOrdered();
        assertEquals("Uncategorized", ordered.get(0).getName());
        assertEquals("UserZ", ordered.get(1).getName());     // sortOrder 50
        assertEquals("UserA", ordered.get(2).getName());     // sortOrder 100
        assertEquals("BundledA", ordered.get(3).getName());  // bundled tier always after user, even with sortOrder 0
        assertEquals("BundledB", ordered.get(4).getName());
    }

    @Test
    public void dedupeImportSkipsExistingIdsAndRebindsCategoryByName()
    {
        // User has a 'Bossing' category with id A. Incoming Library has same-name category with id B.
        Category myBossing = store.createCategory("Bossing");

        Library incoming = new Library();
        UUID incomingCatId = UUID.randomUUID();
        incoming.getCategories().add(new Category(incomingCatId, "Bossing", 0, false, null, false));
        UUID newWpId = UUID.randomUUID();
        incoming.getWaypoints().add(new Waypoint(
            newWpId, "Vorkath", 42, incomingCatId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false));

        WaypointStore.ImportResult r = store.importMerge(incoming);
        assertEquals(1, r.waypointsAdded);
        assertEquals(0, r.waypointsSkipped);

        // Waypoint must be attached to user's existing 'Bossing' (id A), not a new category.
        assertEquals(myBossing.getId(), store.getWaypointById(newWpId).getCategoryId());
        assertNull(store.getCategoryById(incomingCatId));
    }

    @Test
    public void dedupeImportSkipsWaypointAlreadyPresent()
    {
        Waypoint existing = store.createWaypoint(1, "Existing", store.getUncategorized().getId());

        Library incoming = new Library();
        incoming.getCategories().add(new Category(
            store.getUncategorized().getId(), "Uncategorized", 0, true, null, false));
        incoming.getWaypoints().add(new Waypoint(
            existing.getId(), "Different name", 99, store.getUncategorized().getId(), null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false));

        WaypointStore.ImportResult r = store.importMerge(incoming);
        assertEquals(0, r.waypointsAdded);
        assertEquals(1, r.waypointsSkipped);
        assertEquals("Existing", store.getWaypointById(existing.getId()).getName());  // not overwritten
    }

    @Test
    public void importMergePlacesWaypointsInFreshlyAddedCategories()
    {
        // Empty store. Incoming library has a brand-new category and a waypoint pointing to it.
        // Regression: importMerge populated the categoryIndex cache before Phase 1, then mutated
        // library.getCategories() directly. Phase 2's existence check on the newly-added category
        // returned null and the waypoint was rebound to Uncategorized.
        UUID newCatId = UUID.randomUUID();
        UUID newWpId = UUID.randomUUID();

        Library incoming = new Library();
        incoming.getCategories().add(new Category(newCatId, "Bossing", 0, false, null, false));
        incoming.getWaypoints().add(new Waypoint(
            newWpId, "Vorkath", 42, newCatId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false));

        WaypointStore.ImportResult r = store.importMerge(incoming);
        assertEquals(1, r.categoriesAdded);
        assertEquals(1, r.waypointsAdded);
        assertEquals(newCatId, store.getWaypointById(newWpId).getCategoryId());
    }

    @Test
    public void getCategoryByIdAfterCreateAndDelete()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new com.waypointer.model.Library());
        com.waypointer.model.Category c = store.createCategory("Test");
        assertSame(c, store.getCategoryById(c.getId()));
        UUID id = c.getId();
        store.deleteCategory(id, false);
        assertNull(store.getCategoryById(id));
    }

    @Test
    public void hasUndoableIsFalseAtConstruction()
    {
        assertFalse(store.hasUndoable());
    }

    @Test
    public void undoLastIsNoOpWhenNothingArmed()
    {
        // Should not throw and should leave the store unchanged.
        store.undoLast();
        assertFalse(store.hasUndoable());
        assertTrue(store.getLibrary().getWaypoints().isEmpty());
    }

    @Test
    public void renameCategoryClearsUndoSlot()
    {
        store.testArmUndoSlot(() -> {}); // arm a no-op runnable via test seam
        assertTrue(store.hasUndoable());
        Category c = store.createCategory("X");
        store.renameCategory(c.getId(), "Y");
        assertFalse("renameCategory should clear the undo slot", store.hasUndoable());
    }

    @Test
    public void createCategoryClearsUndoSlot()
    {
        store.testArmUndoSlot(() -> {});
        store.createCategory("X");
        assertFalse(store.hasUndoable());
    }

    @Test
    public void createWaypointClearsUndoSlot()
    {
        store.testArmUndoSlot(() -> {});
        store.createWaypoint(100, "X", store.getUncategorized().getId());
        assertFalse(store.hasUndoable());
    }

    @Test
    public void reorderWithinCategoryClearsUndoSlot()
    {
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.testArmUndoSlot(() -> {});
        store.reorderWithinCategory(store.getUncategorized().getId(),
            java.util.Collections.singletonList(w.getId()));
        assertFalse(store.hasUndoable());
    }

    @Test
    public void undoLastAfterDeleteWaypointRestoresIt()
    {
        Waypoint w = store.createWaypoint(100, "Catherby",
            store.getUncategorized().getId());
        UUID id = w.getId();
        int sortOrderBefore = w.getSortOrder();

        store.deleteWaypoint(id);
        assertNull("waypoint should be gone after delete", store.getWaypointById(id));
        assertTrue("delete should arm undo", store.hasUndoable());

        store.undoLast();
        Waypoint restored = store.getWaypointById(id);
        assertNotNull("undo should restore the waypoint", restored);
        assertEquals("Catherby", restored.getName());
        assertEquals(100, restored.getPackedWorldPoint());
        assertEquals(store.getUncategorized().getId(), restored.getCategoryId());
        assertEquals(sortOrderBefore, restored.getSortOrder());
        assertFalse("slot should be cleared after undo", store.hasUndoable());
    }

    @Test
    public void undoLastAfterRecaptureRestoresOldPoint()
    {
        Waypoint w = store.createWaypoint(100, "Catherby",
            store.getUncategorized().getId());

        store.updateWaypointPoint(w.getId(), 200);
        assertEquals(200, store.getWaypointById(w.getId()).getPackedWorldPoint());
        assertTrue("recapture should arm undo", store.hasUndoable());

        store.undoLast();
        assertEquals("undo should restore the original packed point", 100,
            store.getWaypointById(w.getId()).getPackedWorldPoint());
        assertFalse(store.hasUndoable());
    }

    @Test
    public void undoLastAfterTwoRecapturesRestoresOnlyTheMostRecent()
    {
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.updateWaypointPoint(w.getId(), 200);
        store.updateWaypointPoint(w.getId(), 300);

        store.undoLast();
        assertEquals("only the most recent recapture is undone", 200,
            store.getWaypointById(w.getId()).getPackedWorldPoint());
    }

    @Test
    public void undoLastAfterDeleteCategoryMoveChildrenRestoresEverything()
    {
        Category cat = store.createCategory("Herbs");
        Waypoint a = store.createWaypoint(100, "A", cat.getId());
        Waypoint b = store.createWaypoint(200, "B", cat.getId());
        int aSortBefore = a.getSortOrder();
        int bSortBefore = b.getSortOrder();

        store.deleteCategory(cat.getId(), true);
        // Category is gone, waypoints survived but moved to Uncategorized.
        assertNull(store.getCategoryById(cat.getId()));
        assertEquals(store.getUncategorized().getId(),
            store.getWaypointById(a.getId()).getCategoryId());

        store.undoLast();
        assertNotNull("category should be restored", store.getCategoryById(cat.getId()));
        assertEquals("waypoint A should be back in original category",
            cat.getId(), store.getWaypointById(a.getId()).getCategoryId());
        assertEquals("waypoint B should be back in original category",
            cat.getId(), store.getWaypointById(b.getId()).getCategoryId());
        assertEquals("waypoint A sort order should be restored",
            aSortBefore, store.getWaypointById(a.getId()).getSortOrder());
        assertEquals("waypoint B sort order should be restored",
            bSortBefore, store.getWaypointById(b.getId()).getSortOrder());
    }

    @Test
    public void undoLastAfterDeleteCategoryWithChildrenRestoresEverything()
    {
        Category cat = store.createCategory("Herbs");
        Waypoint a = store.createWaypoint(100, "A", cat.getId());
        Waypoint b = store.createWaypoint(200, "B", cat.getId());

        store.deleteCategory(cat.getId(), false);
        assertNull(store.getCategoryById(cat.getId()));
        assertNull("waypoint A should be deleted", store.getWaypointById(a.getId()));
        assertNull("waypoint B should be deleted", store.getWaypointById(b.getId()));

        store.undoLast();
        assertNotNull("category should be restored", store.getCategoryById(cat.getId()));
        Waypoint restoredA = store.getWaypointById(a.getId());
        Waypoint restoredB = store.getWaypointById(b.getId());
        assertNotNull(restoredA);
        assertNotNull(restoredB);
        assertEquals(cat.getId(), restoredA.getCategoryId());
        assertEquals(cat.getId(), restoredB.getCategoryId());
        assertEquals("A", restoredA.getName());
        assertEquals("B", restoredB.getName());
    }

    @Test
    public void undoLastCalledTwiceSecondCallIsNoOp()
    {
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.deleteWaypoint(w.getId());
        store.undoLast();
        // Slot is cleared by undoLast. A second call has nothing to do and must not throw,
        // duplicate the restored waypoint, or arm the slot.
        store.undoLast();
        assertEquals(1, store.getLibrary().getWaypoints().size());
        assertFalse(store.hasUndoable());
    }

    @Test
    public void setWaypointPinnedFlipsFlagAndStampsPinnedAt()
    {
        Waypoint w = store.createWaypoint(1, "Home", store.getUncategorized().getId());
        assertFalse(w.isPinned());
        assertNull(w.getPinnedAt());

        Instant before = Instant.now();
        store.setWaypointPinned(w.getId(), true);
        Waypoint after = store.getWaypointById(w.getId());
        assertTrue(after.isPinned());
        assertNotNull(after.getPinnedAt());
        assertFalse(after.getPinnedAt().isBefore(before));
    }

    @Test
    public void setWaypointPinnedFalseClearsPinnedAt()
    {
        Waypoint w = store.createWaypoint(1, "Home", store.getUncategorized().getId());
        store.setWaypointPinned(w.getId(), true);
        assertNotNull(store.getWaypointById(w.getId()).getPinnedAt());
        store.setWaypointPinned(w.getId(), false);
        Waypoint after = store.getWaypointById(w.getId());
        assertFalse(after.isPinned());
        assertNull(after.getPinnedAt());
    }

    @Test
    public void getPinnedWaypointsAscendingByPinnedAt() throws InterruptedException
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        Waypoint c = store.createWaypoint(3, "C", uId);
        store.setWaypointPinned(a.getId(), true);
        Thread.sleep(5);
        store.setWaypointPinned(b.getId(), true);
        Thread.sleep(5);
        store.setWaypointPinned(c.getId(), true);

        List<Waypoint> asc = store.getPinnedWaypoints(false);
        assertEquals("A", asc.get(0).getName());
        assertEquals("B", asc.get(1).getName());
        assertEquals("C", asc.get(2).getName());
    }

    @Test
    public void getPinnedWaypointsDescendingByPinnedAt() throws InterruptedException
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        store.setWaypointPinned(a.getId(), true);
        Thread.sleep(5);
        store.setWaypointPinned(b.getId(), true);

        List<Waypoint> desc = store.getPinnedWaypoints(true);
        assertEquals("B", desc.get(0).getName());
        assertEquals("A", desc.get(1).getName());
    }

    @Test
    public void getPinnedWaypointsExcludesUnpinned()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "Pinned", uId);
        store.createWaypoint(2, "Loose", uId);
        store.setWaypointPinned(a.getId(), true);

        List<Waypoint> pinned = store.getPinnedWaypoints(false);
        assertEquals(1, pinned.size());
        assertEquals("Pinned", pinned.get(0).getName());
    }

    @Test
    public void setWaypointPinnedFiresListeners()
    {
        AtomicInteger calls = new AtomicInteger();
        Waypoint w = store.createWaypoint(1, "W", store.getUncategorized().getId());
        store.subscribe(() -> calls.incrementAndGet());
        store.setWaypointPinned(w.getId(), true);
        assertEquals(1, calls.get());
    }
}
