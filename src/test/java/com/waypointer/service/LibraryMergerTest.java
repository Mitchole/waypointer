package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LibraryMergerTest
{
    private final LibraryMerger merger = new LibraryMerger();

    private static Library targetWithSentinel(UUID sentinelId)
    {
        Library lib = new Library();
        lib.getCategories().add(new Category(sentinelId, "Uncategorized", 0, true, null, false));
        return lib;
    }

    private static Waypoint wp(UUID id, UUID cat)
    {
        return new Waypoint(id, "W", 42, cat, null, "",
            Instant.parse("2026-06-01T00:00:00Z"), 0, false, null, false);
    }

    @Test
    public void addsNewCategoryAndWaypoint()
    {
        UUID sentinel = UUID.randomUUID();
        Library target = targetWithSentinel(sentinel);
        LibraryViews views = new LibraryViews(() -> target);

        Library incoming = new Library();
        UUID catId = UUID.randomUUID();
        incoming.getCategories().add(new Category(catId, "Bossing", 0, false, null, false));
        UUID wpId = UUID.randomUUID();
        incoming.getWaypoints().add(wp(wpId, catId));

        WaypointStore.ImportResult r = merger.merge(target, views, incoming);

        assertEquals(1, r.categoriesAdded);
        assertEquals(1, r.waypointsAdded);
        assertEquals(0, r.categoriesMerged);
        assertEquals(catId, target.getWaypoints().get(0).getCategoryId());
    }

    @Test
    public void remapsCategoryByNameAndCountsMerged()
    {
        UUID sentinel = UUID.randomUUID();
        Library target = targetWithSentinel(sentinel);
        UUID myCat = UUID.randomUUID();
        target.getCategories().add(new Category(myCat, "Bossing", 1, false, null, false));
        LibraryViews views = new LibraryViews(() -> target);

        Library incoming = new Library();
        UUID theirCat = UUID.randomUUID();
        incoming.getCategories().add(new Category(theirCat, "Bossing", 0, false, null, false));
        incoming.getWaypoints().add(wp(UUID.randomUUID(), theirCat));

        WaypointStore.ImportResult r = merger.merge(target, views, incoming);

        assertEquals(0, r.categoriesAdded);
        assertEquals(1, r.categoriesMerged);
        assertEquals(1, r.waypointsAdded);
        assertEquals(myCat, target.getWaypoints().get(0).getCategoryId());
    }

    @Test
    public void skipsWaypointWithDuplicateId()
    {
        UUID sentinel = UUID.randomUUID();
        Library target = targetWithSentinel(sentinel);
        UUID dupId = UUID.randomUUID();
        target.getWaypoints().add(wp(dupId, sentinel));
        LibraryViews views = new LibraryViews(() -> target);

        Library incoming = new Library();
        incoming.getCategories().add(new Category(sentinel, "Uncategorized", 0, true, null, false));
        incoming.getWaypoints().add(wp(dupId, sentinel));

        WaypointStore.ImportResult r = merger.merge(target, views, incoming);

        assertEquals(0, r.waypointsAdded);
        assertEquals(1, r.waypointsSkipped);
    }

    @Test
    public void orphanCategoryIdFallsToUncategorized()
    {
        UUID sentinel = UUID.randomUUID();
        Library target = targetWithSentinel(sentinel);
        LibraryViews views = new LibraryViews(() -> target);

        Library incoming = new Library();
        UUID ghostCat = UUID.randomUUID();
        incoming.getWaypoints().add(wp(UUID.randomUUID(), ghostCat));

        WaypointStore.ImportResult r = merger.merge(target, views, incoming);

        assertEquals(1, r.waypointsAdded);
        assertEquals(sentinel, target.getWaypoints().get(0).getCategoryId());
    }
}
