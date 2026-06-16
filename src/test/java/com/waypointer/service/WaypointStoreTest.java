package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    public void createWaypointWithNpcNameStoresIt()
    {
        Waypoint w = store.createWaypoint(99, "Banker", store.getUncategorized().getId(), "Banker");
        assertEquals("Banker", w.getTargetNpcName());
        assertEquals("Banker", store.getWaypointById(w.getId()).getTargetNpcName());
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
    public void noOpMutationLeavesUndoSlotArmed()
    {
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.deleteWaypoint(w.getId());
        assertTrue("delete arms undo", store.hasUndoable());

        // A mutation that changes nothing (unknown id) never reaches notifyChanged, so it
        // leaves the armed inverse intact rather than discarding it.
        store.renameWaypoint(UUID.randomUUID(), "ignored");
        assertTrue("a no-op mutation must not clear the slot", store.hasUndoable());

        store.undoLast();
        assertNotNull("the still-armed undo restores the deleted waypoint",
            store.getWaypointById(w.getId()));
    }

    @Test
    public void rejectedMutationLeavesUndoSlotArmed()
    {
        Category c = store.createCategory("Herbs");
        Waypoint w = store.createWaypoint(100, "X", c.getId());
        store.deleteWaypoint(w.getId());
        assertTrue(store.hasUndoable());

        // A rejected mutation throws before notifying, so the slot survives.
        try
        {
            store.renameCategory(c.getId(), store.getUncategorized().getName());
            fail("expected duplicate-name rejection");
        }
        catch (IllegalArgumentException expected)
        {
            // intended
        }
        assertTrue("a rejected mutation must not clear the slot", store.hasUndoable());
    }

    @Test
    public void renameWaypointClearsUndoSlot()
    {
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.testArmUndoSlot(() -> {});
        store.renameWaypoint(w.getId(), "Y");
        assertFalse(store.hasUndoable());
    }

    @Test
    public void moveWaypointToCategoryClearsUndoSlot()
    {
        Category c = store.createCategory("Herbs");
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.testArmUndoSlot(() -> {});
        store.moveWaypointToCategory(w.getId(), c.getId());
        assertFalse(store.hasUndoable());
    }

    @Test
    public void setCategoryColorClearsUndoSlot()
    {
        Category c = store.createCategory("Herbs");
        store.testArmUndoSlot(() -> {});
        store.setCategoryColor(c.getId(), 0xFF00FF);
        assertFalse(store.hasUndoable());
    }

    @Test
    public void setWaypointPinnedClearsUndoSlot()
    {
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.testArmUndoSlot(() -> {});
        store.setWaypointPinned(w.getId(), true);
        assertFalse(store.hasUndoable());
    }

    @Test
    public void updateWaypointNotesClearsUndoSlot()
    {
        Waypoint w = store.createWaypoint(100, "X", store.getUncategorized().getId());
        store.testArmUndoSlot(() -> {});
        store.updateWaypointNotes(w.getId(), "a note");
        assertFalse(store.hasUndoable());
    }

    @Test
    public void reorderCategoriesClearsUndoSlot()
    {
        Category a = store.createCategory("A");
        Category b = store.createCategory("B");
        store.testArmUndoSlot(() -> {});
        store.reorderCategories(java.util.Arrays.asList(b.getId(), a.getId()));
        assertFalse(store.hasUndoable());
    }

    @Test
    public void importMergeClearsUndoSlot()
    {
        store.testArmUndoSlot(() -> {});
        Library incoming = new Library();
        incoming.getCategories().add(
            new Category(UUID.randomUUID(), "Imported", 0, false, null, false));
        store.importMerge(incoming);
        assertFalse(store.hasUndoable());
    }

    @Test
    public void undoLastAfterDeleteWaypointsRestoresAll()
    {
        Waypoint a = store.createWaypoint(100, "A", store.getUncategorized().getId());
        Waypoint b = store.createWaypoint(200, "B", store.getUncategorized().getId());

        store.deleteWaypoints(java.util.Arrays.asList(a.getId(), b.getId()));
        assertNull(store.getWaypointById(a.getId()));
        assertNull(store.getWaypointById(b.getId()));
        assertTrue("batch delete arms one undo", store.hasUndoable());

        store.undoLast();
        assertNotNull("undo restores the first waypoint", store.getWaypointById(a.getId()));
        assertNotNull("undo restores the second waypoint", store.getWaypointById(b.getId()));
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
    public void getPinnedWaypointsAscendingByPinnedAt()
    {
        AtomicLong tick = new AtomicLong(1000L);
        store.setClockForTest(() -> Instant.ofEpochMilli(tick.getAndAdd(1000L)));
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        Waypoint c = store.createWaypoint(3, "C", uId);
        store.setWaypointPinned(a.getId(), true);
        store.setWaypointPinned(b.getId(), true);
        store.setWaypointPinned(c.getId(), true);

        List<Waypoint> asc = store.getPinnedWaypoints(false);
        assertEquals("A", asc.get(0).getName());
        assertEquals("B", asc.get(1).getName());
        assertEquals("C", asc.get(2).getName());
    }

    @Test
    public void getPinnedWaypointsDescendingByPinnedAt()
    {
        AtomicLong tick = new AtomicLong(1000L);
        store.setClockForTest(() -> Instant.ofEpochMilli(tick.getAndAdd(1000L)));
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        store.setWaypointPinned(a.getId(), true);
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

    @Test
    public void setBypassWildernessConfirmFlipsFlagAndFires()
    {
        AtomicInteger calls = new AtomicInteger();
        Waypoint w = store.createWaypoint(1, "Wild", store.getUncategorized().getId());
        assertFalse(w.isBypassWildernessConfirm());
        store.subscribe(() -> calls.incrementAndGet());

        store.setWaypointBypassWildernessConfirm(w.getId(), true);
        assertTrue(store.getWaypointById(w.getId()).isBypassWildernessConfirm());
        assertEquals(1, calls.get());

        store.setWaypointBypassWildernessConfirm(w.getId(), false);
        assertFalse(store.getWaypointById(w.getId()).isBypassWildernessConfirm());
        assertEquals(2, calls.get());
    }

    @Test
    public void setBypassWildernessConfirmDoesNotTouchPinnedState()
    {
        Waypoint w = store.createWaypoint(1, "W", store.getUncategorized().getId());
        store.setWaypointPinned(w.getId(), true);
        Instant pinnedAt = store.getWaypointById(w.getId()).getPinnedAt();

        store.setWaypointBypassWildernessConfirm(w.getId(), true);
        Waypoint after = store.getWaypointById(w.getId());
        assertTrue(after.isPinned());
        assertEquals(pinnedAt, after.getPinnedAt());
    }

    @Test
    public void getWaypointsInCategory_manualMode_sortsBySortOrder()
    {
        UUID cat = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "Zebra", cat);
        Waypoint b = store.createWaypoint(2, "Apple", cat);
        Waypoint c = store.createWaypoint(3, "Mango", cat);
        store.reorderWithinCategory(cat, java.util.Arrays.asList(c.getId(), a.getId(), b.getId()));
        List<Waypoint> result = store.getWaypointsInCategory(cat);
        assertEquals("Mango", result.get(0).getName());
        assertEquals("Zebra", result.get(1).getName());
        assertEquals("Apple", result.get(2).getName());
    }

    @Test
    public void setCategorySortMode_persistsModeAndFiresListeners()
    {
        Category cat = store.createCategory("Slayer Masters");
        java.util.concurrent.atomic.AtomicInteger fires = new java.util.concurrent.atomic.AtomicInteger();
        store.subscribe(fires::incrementAndGet);
        int before = fires.get();

        store.setCategorySortMode(cat.getId(), com.waypointer.model.CategorySortMode.NAME);

        assertEquals(com.waypointer.model.CategorySortMode.NAME,
            store.getCategoryById(cat.getId()).getSortMode());
        assertTrue("expected at least one notifyChanged fire", fires.get() > before);
    }

    @Test
    public void setCategorySortMode_clearsUndoSlot()
    {
        Category cat = store.createCategory("X");
        store.testArmUndoSlot(() -> {});
        assertTrue(store.hasUndoable());

        store.setCategorySortMode(cat.getId(), com.waypointer.model.CategorySortMode.NAME);

        assertFalse("setCategorySortMode must clear lastUndo", store.hasUndoable());
    }

    @Test
    public void setCategorySortMode_unknownCategoryIsSilentNoOp()
    {
        store.setCategorySortMode(UUID.randomUUID(), com.waypointer.model.CategorySortMode.NAME);
    }

    @Test
    public void getWaypointsInCategory_nameMode_sortsCaseInsensitive()
    {
        Category cat = store.createCategory("POIs");
        store.createWaypoint(1, "banana", cat.getId());
        store.createWaypoint(2, "Apple", cat.getId());
        store.createWaypoint(3, "cherry", cat.getId());
        store.setCategorySortMode(cat.getId(), com.waypointer.model.CategorySortMode.NAME);

        List<Waypoint> result = store.getWaypointsInCategory(cat.getId());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("banana", result.get(1).getName());
        assertEquals("cherry", result.get(2).getName());
    }

    @Test
    public void getWaypointsInCategory_nameMode_tieBreaksByCreatedAt()
    {
        Category cat = store.createCategory("POIs");
        Waypoint older = store.createWaypoint(1, "Twin", cat.getId());
        Waypoint newer = store.createWaypoint(2, "Twin", cat.getId());
        older.setCreatedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        newer.setCreatedAt(java.time.Instant.parse("2026-02-01T00:00:00Z"));
        store.setCategorySortMode(cat.getId(), com.waypointer.model.CategorySortMode.NAME);

        List<Waypoint> result = store.getWaypointsInCategory(cat.getId());
        assertEquals(older.getId(), result.get(0).getId());
        assertEquals(newer.getId(), result.get(1).getId());
    }

    @Test
    public void getWaypointsInCategory_dateAddedMode_newestFirst()
    {
        Category cat = store.createCategory("POIs");
        Waypoint oldest = store.createWaypoint(1, "A", cat.getId());
        Waypoint middle = store.createWaypoint(2, "B", cat.getId());
        Waypoint newest = store.createWaypoint(3, "C", cat.getId());
        oldest.setCreatedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        middle.setCreatedAt(java.time.Instant.parse("2026-02-01T00:00:00Z"));
        newest.setCreatedAt(java.time.Instant.parse("2026-03-01T00:00:00Z"));
        store.setCategorySortMode(cat.getId(), com.waypointer.model.CategorySortMode.DATE_ADDED);

        List<Waypoint> result = store.getWaypointsInCategory(cat.getId());
        assertEquals(newest.getId(), result.get(0).getId());
        assertEquals(middle.getId(), result.get(1).getId());
        assertEquals(oldest.getId(), result.get(2).getId());
    }

    @Test
    public void getWaypointsInCategory_nullSortMode_treatedAsManual()
    {
        Category cat = store.createCategory("Legacy");
        assertNull(cat.getSortMode());
        Waypoint z = store.createWaypoint(1, "Zebra", cat.getId());
        Waypoint a = store.createWaypoint(2, "Apple", cat.getId());
        List<Waypoint> result = store.getWaypointsInCategory(cat.getId());
        assertEquals(z.getId(), result.get(0).getId());
        assertEquals(a.getId(), result.get(1).getId());
    }

    @Test
    public void setCategorySortMode_preservesManualSortOrderWhenFlippedBack()
    {
        Category cat = store.createCategory("X");
        Waypoint a = store.createWaypoint(1, "Apple", cat.getId());
        Waypoint b = store.createWaypoint(2, "Banana", cat.getId());
        Waypoint c = store.createWaypoint(3, "Cherry", cat.getId());
        store.reorderWithinCategory(cat.getId(),
            java.util.Arrays.asList(c.getId(), a.getId(), b.getId()));

        store.setCategorySortMode(cat.getId(), com.waypointer.model.CategorySortMode.NAME);
        List<Waypoint> named = store.getWaypointsInCategory(cat.getId());
        assertEquals("Apple", named.get(0).getName());

        store.setCategorySortMode(cat.getId(), com.waypointer.model.CategorySortMode.MANUAL);
        List<Waypoint> manual = store.getWaypointsInCategory(cat.getId());
        assertEquals("Cherry", manual.get(0).getName());
        assertEquals("Apple", manual.get(1).getName());
        assertEquals("Banana", manual.get(2).getName());
    }

    @Test
    public void moveWaypointsToCategoryReparentsAllToTail()
    {
        UUID uId = store.getUncategorized().getId();
        Category bossing = store.createCategory("Bossing");
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        Waypoint keep = store.createWaypoint(3, "Keep", bossing.getId());

        store.moveWaypointsToCategory(java.util.Arrays.asList(a.getId(), b.getId()), bossing.getId());

        assertEquals(0, store.getWaypointsInCategory(uId).size());
        java.util.List<Waypoint> target = store.getWaypointsInCategory(bossing.getId());
        assertEquals(3, target.size());
        assertEquals("Keep", target.get(0).getName());
        assertTrue(target.get(1).getSortOrder() < target.get(2).getSortOrder());
    }

    @Test
    public void moveWaypointsToUnknownTargetIsNoOp()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        store.moveWaypointsToCategory(java.util.Collections.singletonList(a.getId()), UUID.randomUUID());
        assertEquals(uId, store.getWaypointById(a.getId()).getCategoryId());
    }

    @Test
    public void moveWaypointsAssignsTailOrderInIterationOrder()
    {
        UUID uId = store.getUncategorized().getId();
        Category bossing = store.createCategory("Bossing");
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        // Pass b before a; the target tail order must follow the given iteration order.
        store.moveWaypointsToCategory(java.util.Arrays.asList(b.getId(), a.getId()), bossing.getId());
        java.util.List<Waypoint> target = store.getWaypointsInCategory(bossing.getId());
        assertEquals("B", target.get(0).getName());
        assertEquals("A", target.get(1).getName());
    }

    @Test
    public void moveWaypointsSkipsUnknownIds()
    {
        UUID uId = store.getUncategorized().getId();
        Category bossing = store.createCategory("Bossing");
        Waypoint a = store.createWaypoint(1, "A", uId);
        store.moveWaypointsToCategory(
            java.util.Arrays.asList(a.getId(), UUID.randomUUID()), bossing.getId());
        assertEquals(1, store.getWaypointsInCategory(bossing.getId()).size());
    }

    @Test
    public void moveWaypointsClearsUndoSlot()
    {
        UUID uId = store.getUncategorized().getId();
        Category bossing = store.createCategory("Bossing");
        Waypoint a = store.createWaypoint(1, "A", uId);
        store.deleteWaypoint(store.createWaypoint(9, "Doomed", uId).getId()); // arms undo
        assertTrue(store.hasUndoable());
        store.moveWaypointsToCategory(java.util.Collections.singletonList(a.getId()), bossing.getId());
        assertFalse("move is non-undoable and clears the slot", store.hasUndoable());
    }

    @Test
    public void deleteWaypointsRemovesAll()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        store.deleteWaypoints(java.util.Arrays.asList(a.getId(), b.getId()));
        assertNull(store.getWaypointById(a.getId()));
        assertNull(store.getWaypointById(b.getId()));
    }

    @Test
    public void deleteWaypointsSingleUndoRestoresAll()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        store.deleteWaypoints(java.util.Arrays.asList(a.getId(), b.getId()));
        assertTrue("batch delete arms one undo", store.hasUndoable());
        store.undoLast();
        assertNotNull(store.getWaypointById(a.getId()));
        assertNotNull(store.getWaypointById(b.getId()));
        assertEquals(2, store.getWaypointsInCategory(uId).size());
        assertFalse("slot cleared after undo", store.hasUndoable());
    }

    @Test
    public void deleteWaypointsThenOtherMutationClearsUndoSlot()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        store.deleteWaypoints(java.util.Arrays.asList(a.getId(), b.getId()));
        store.createWaypoint(3, "C", uId); // any other mutation clears the slot
        assertFalse(store.hasUndoable());
    }

    @Test
    public void deleteWaypointsSkipsUnknownIdsAndDoesNotArmWhenNoneFound()
    {
        store.deleteWaypoints(java.util.Collections.singletonList(UUID.randomUUID()));
        assertFalse("nothing found -> no undo armed", store.hasUndoable());
    }

    @Test
    public void deleteWaypointsLeavesUnselectedIntact()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        Waypoint c = store.createWaypoint(3, "C", uId);
        store.deleteWaypoints(java.util.Collections.singletonList(b.getId()));
        assertNotNull(store.getWaypointById(a.getId()));
        assertNull(store.getWaypointById(b.getId()));
        assertNotNull(store.getWaypointById(c.getId()));
        assertEquals(2, store.getWaypointsInCategory(uId).size());
    }

    @Test
    public void deleteWaypointsEmptyCollectionIsNoOp()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        store.deleteWaypoints(java.util.Collections.emptyList());
        assertNotNull(store.getWaypointById(a.getId()));
        assertFalse(store.hasUndoable());
    }

    @Test
    public void moveWaypointsEmptyCollectionIsNoOp()
    {
        UUID uId = store.getUncategorized().getId();
        Category bossing = store.createCategory("Bossing");
        Waypoint a = store.createWaypoint(1, "A", uId);
        store.moveWaypointsToCategory(java.util.Collections.emptyList(), bossing.getId());
        assertEquals(uId, store.getWaypointById(a.getId()).getCategoryId());
        assertFalse(store.hasUndoable());
    }

    @Test
    public void setCategoryColorPersistsOnCategory()
    {
        Category c = store.createCategory("Bossing");
        store.setCategoryColor(c.getId(), 0x4080CC);
        assertEquals(Integer.valueOf(0x4080CC),
            store.getLibrary().getCategories().stream()
                .filter(x -> x.getId().equals(c.getId())).findFirst().get().getColor());

        store.setCategoryColor(c.getId(), null);
        assertNull(store.getLibrary().getCategories().stream()
            .filter(x -> x.getId().equals(c.getId())).findFirst().get().getColor());
    }

    @Test
    public void moveWaypointsAlreadyInTargetBumpsToTail()
    {
        UUID uId = store.getUncategorized().getId();
        Waypoint a = store.createWaypoint(1, "A", uId);
        Waypoint b = store.createWaypoint(2, "B", uId);
        // Move A (already in Uncategorized) — it should relocate to the tail, after B.
        store.moveWaypointsToCategory(java.util.Collections.singletonList(a.getId()), uId);
        java.util.List<Waypoint> after = store.getWaypointsInCategory(uId);
        assertEquals("B", after.get(0).getName());
        assertEquals("A", after.get(1).getName());
    }
}
