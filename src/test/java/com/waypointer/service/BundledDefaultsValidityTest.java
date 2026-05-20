package com.waypointer.service;

import com.google.gson.Gson;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.Waypoint;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class BundledDefaultsValidityTest
{
    @Test
    public void bundledDefaultsParseAndAreInternallyConsistent()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        WaypointDefaults defaults = new WaypointDefaults(new LibraryJsonCodec(new Gson()), store);
        Library lib = defaults.loadBundled();

        Set<UUID> categoryIds = new HashSet<>();
        for (Category c : lib.getCategories())
        {
            assertNotNull("Category id must be non-null", c.getId());
            assertNotNull("Category name must be non-null", c.getName());
            assertTrue("Duplicate category id: " + c.getId(), categoryIds.add(c.getId()));
        }

        Set<UUID> waypointIds = new HashSet<>();
        for (Waypoint w : lib.getWaypoints())
        {
            assertNotNull("Waypoint id must be non-null", w.getId());
            assertNotNull("Waypoint name must be non-null", w.getName());
            assertTrue("Duplicate waypoint id: " + w.getId(), waypointIds.add(w.getId()));
            assertTrue("Waypoint references unknown category " + w.getCategoryId(),
                categoryIds.contains(w.getCategoryId()));
            int plane = WorldPointPacker.getPlane(w.getPackedWorldPoint());
            assertTrue("Plane out of range: " + plane, plane >= 0 && plane <= 3);
        }
    }
}
