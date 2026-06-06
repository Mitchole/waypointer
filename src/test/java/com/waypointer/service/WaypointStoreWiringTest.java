package com.waypointer.service;

import com.waypointer.model.Library;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

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
