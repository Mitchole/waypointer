package com.waypointer.model;

import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class LibraryTest
{
    @Test
    public void emptyLibraryHasNoWaypointsAndCurrentSchemaVersion()
    {
        Library lib = new Library();
        assertEquals(Library.CURRENT_SCHEMA_VERSION, lib.getSchemaVersion());
        assertTrue(lib.getCategories().isEmpty());
        assertTrue(lib.getWaypoints().isEmpty());
    }

    @Test
    public void canAddCategoryAndWaypoint()
    {
        Library lib = new Library();
        Category c = new Category(UUID.randomUUID(), "Bossing", 0, false, null, false);
        lib.getCategories().add(c);

        Waypoint w = new Waypoint(
            UUID.randomUUID(), "Vorkath",
            42, c.getId(), null, "", Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);
        lib.getWaypoints().add(w);

        assertEquals(1, lib.getCategories().size());
        assertEquals(1, lib.getWaypoints().size());
        assertEquals(c.getId(), lib.getWaypoints().get(0).getCategoryId());
    }

    @Test
    public void uncategorizedSentinelDetectableViaFlag()
    {
        Category uncat = new Category(UUID.randomUUID(), "Uncategorized", 0, true, null, false);
        Category bossing = new Category(UUID.randomUUID(), "Bossing", 1, false, null, false);
        assertTrue(uncat.isUncategorized());
        assertFalse(bossing.isUncategorized());
    }

    @Test
    public void newFieldsDefaultToFalseAndNullViaNoArgsConstructor()
    {
        Waypoint w = new Waypoint();
        assertFalse(w.isPinned());
        assertNull(w.getPinnedAt());
        assertFalse(w.isBypassWildernessConfirm());
    }
}
