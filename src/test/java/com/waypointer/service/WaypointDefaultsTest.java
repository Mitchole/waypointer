package com.waypointer.service;

import com.google.gson.Gson;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Library;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaypointDefaultsTest
{
    private WaypointStore store;
    private WaypointDefaults defaults;

    @Before
    public void setUp()
    {
        store = new WaypointStore();
        store.bootstrap(new Library());
        Gson gson = new Gson();
        LibraryJsonCodec codec = new LibraryJsonCodec(gson);
        defaults = new WaypointDefaults(codec, store);
    }

    @Test
    public void loadsBundledResourceAsLibrary()
    {
        Library lib = defaults.loadBundled();
        assertNotNull(lib);
        // The placeholder JSON has zero categories and zero waypoints; later additions are fine.
        assertNotNull(lib.getCategories());
        assertNotNull(lib.getWaypoints());
    }

    @Test
    public void importIsIdempotent()
    {
        WaypointStore.ImportResult first = defaults.importIntoStore();
        WaypointStore.ImportResult second = defaults.importIntoStore();
        // Whatever gets imported the first time, the second pass must skip all of it.
        assertEquals(0, second.waypointsAdded);
        assertEquals(first.waypointsAdded, second.waypointsSkipped);
    }
}
