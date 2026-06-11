package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LibraryViewsTest
{
    private static Waypoint wp(UUID cat, int sortOrder)
    {
        return new Waypoint(UUID.randomUUID(), "W", 42, cat, null, "",
            Instant.parse("2026-06-01T00:00:00Z"), sortOrder, false, null, false);
    }

    @Test
    public void getCategoryByNameIsCaseInsensitive()
    {
        Library lib = new Library();
        UUID id = UUID.randomUUID();
        lib.getCategories().add(new Category(id, "Bossing", 0, false, null, false));
        LibraryViews views = new LibraryViews(() -> lib);
        assertEquals(id, views.getCategoryByName("bOsSiNg").getId());
        assertNull(views.getCategoryByName("Nope"));
    }

    @Test
    public void getUncategorizedReturnsSentinel()
    {
        Library lib = new Library();
        UUID id = UUID.randomUUID();
        lib.getCategories().add(new Category(id, "Uncategorized", 0, true, null, false));
        LibraryViews views = new LibraryViews(() -> lib);
        assertEquals(id, views.getUncategorized().getId());
    }

    @Test(expected = IllegalStateException.class)
    public void getUncategorizedThrowsWhenMissing()
    {
        new LibraryViews(() -> new Library()).getUncategorized();
    }

    @Test
    public void nextWaypointSortOrderIsMaxPlusOne()
    {
        Library lib = new Library();
        UUID cat = UUID.randomUUID();
        lib.getCategories().add(new Category(cat, "C", 0, false, null, false));
        LibraryViews views = new LibraryViews(() -> lib);
        assertEquals(0, views.nextWaypointSortOrder(cat));
        lib.getWaypoints().add(wp(cat, 5));
        assertEquals(6, views.nextWaypointSortOrder(cat));
    }
}
