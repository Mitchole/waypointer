package com.waypointer.service;

import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WaypointStoreWiringTest
{
    @Test
    public void saverFiresOnEachMutation()
    {
        WaypointStore store = new WaypointStore();
        int[] count = {0};
        store.enablePersistence(() -> count[0]++);
        store.createCategory("A");
        store.createCategory("B");
        assertEquals(2, count[0]);
    }

    @Test
    public void enablePersistenceIsIdempotent()
    {
        WaypointStore store = new WaypointStore();
        int[] count = {0};
        Runnable saver = () -> count[0]++;
        store.enablePersistence(saver);
        store.enablePersistence(saver); // second call ignored
        store.createCategory("A");
        assertEquals(1, count[0]);
    }

    @Test
    public void disablePersistenceStopsTheSaver()
    {
        WaypointStore store = new WaypointStore();
        int[] count = {0};
        store.enablePersistence(() -> count[0]++);
        store.disablePersistence();
        store.createCategory("A");
        assertEquals(0, count[0]);
    }

    @Test
    public void batchCoalescesMutationsIntoOneSave()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        int[] count = {0};
        store.enablePersistence(() -> count[0]++);
        store.batch(() -> {
            store.createCategory("A");
            store.createCategory("B");
        });
        // Two mutations inside one batch must produce exactly one save, not two.
        assertEquals(1, count[0]);
    }

    @Test
    public void batchStillFiresOnceWhenSomethingChanged()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        int[] count = {0};
        store.enablePersistence(() -> count[0]++);
        store.batch(() -> store.createCategory("A"));
        assertEquals(1, count[0]);
    }

    @Test
    public void emptyBatchDoesNotSave()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        int[] count = {0};
        store.enablePersistence(() -> count[0]++);
        store.batch(() -> {});
        assertEquals(0, count[0]);
    }

    @Test
    public void batchSeesEarlierMutationsThroughViews()
    {
        // The cross-category drag path reparents a waypoint and then reads the target
        // category's contents to reorder it. Inside a batch the listener fire is deferred,
        // but the views cache must still reflect the reparent so the reorder sees it.
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        UUID catA = store.createCategory("A").getId();
        UUID catB = store.createCategory("B").getId();
        Waypoint w = store.createWaypoint(1, "w", catA);
        store.batch(() -> {
            store.moveWaypointToCategory(w.getId(), catB);
            assertTrue("views must reflect the reparent mid-batch",
                store.getWaypointsInCategory(catB).stream().anyMatch(x -> x.getId().equals(w.getId())));
        });
    }

    @Test
    public void saverReadsLiveLibraryAfterBootstrap()
    {
        WaypointStore store = new WaypointStore();
        int[] lastSeenCategoryCount = {-1};
        store.enablePersistence(() -> lastSeenCategoryCount[0] = store.getLibrary().getCategories().size());
        store.bootstrap(new Library());           // fires; saver must read the freshly-bootstrapped lib
        // bootstrap ensures the Uncategorized sentinel, so the saver sees at least one category.
        assertEquals(store.getLibrary().getCategories().size(), lastSeenCategoryCount[0]);
    }
}
