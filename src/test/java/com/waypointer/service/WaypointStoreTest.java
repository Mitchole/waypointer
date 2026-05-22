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
            Instant.parse("2026-05-02T00:00:00Z"), 0));

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
            Instant.parse("2026-05-02T00:00:00Z"), 0));

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
            Instant.parse("2026-05-02T00:00:00Z"), 0));

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
}
