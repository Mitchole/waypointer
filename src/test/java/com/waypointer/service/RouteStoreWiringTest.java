package com.waypointer.service;

import com.waypointer.model.route.RouteLibrary;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RouteStoreWiringTest
{
    @Test
    public void saverFiresOnEachMutation()
    {
        RouteStore store = new RouteStore();
        int[] count = {0};
        store.enablePersistence(() -> count[0]++);
        store.createRoute("A");
        store.createRoute("B");
        assertEquals(2, count[0]);
    }

    @Test
    public void enablePersistenceIsIdempotent()
    {
        RouteStore store = new RouteStore();
        int[] count = {0};
        Runnable saver = () -> count[0]++;
        store.enablePersistence(saver);
        store.enablePersistence(saver);
        store.createRoute("A");
        assertEquals(1, count[0]);
    }

    @Test
    public void disablePersistenceStopsTheSaver()
    {
        RouteStore store = new RouteStore();
        int[] count = {0};
        store.enablePersistence(() -> count[0]++);
        store.disablePersistence();
        store.createRoute("A");
        assertEquals(0, count[0]);
    }

    @Test
    public void saverReadsLiveLibraryAfterBootstrap()
    {
        RouteStore store = new RouteStore();
        int[] lastSeenRouteCount = {-1};
        store.enablePersistence(() -> lastSeenRouteCount[0] = store.getLibrary().getRoutes().size());
        store.bootstrap(new RouteLibrary());
        assertEquals(store.getLibrary().getRoutes().size(), lastSeenRouteCount[0]);
    }
}
