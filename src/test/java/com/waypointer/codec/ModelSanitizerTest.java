package com.waypointer.codec;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelSanitizerTest
{
    private static Waypoint wp(UUID id, String name, int packed)
    {
        return new Waypoint(id, name, packed, UUID.randomUUID(), null, "",
            Instant.parse("2026-06-01T00:00:00Z"), 0, false, null, false);
    }

    private static Category cat(UUID id, String name)
    {
        return new Category(id, name, 0, false, null, false);
    }

    @Test
    public void keepsValidWaypoint()
    {
        Waypoint good = wp(UUID.randomUUID(), "Bank", WorldPointPacker.pack(3200, 3200, 0));
        assertTrue(ModelSanitizer.isValidWaypoint(good));
    }

    @Test
    public void dropsWaypointWithNullIdOrName()
    {
        assertFalse(ModelSanitizer.isValidWaypoint(wp(null, "Bank", 42)));
        assertFalse(ModelSanitizer.isValidWaypoint(wp(UUID.randomUUID(), null, 42)));
    }

    @Test
    public void dropsWaypointWithUnusableCoordinate()
    {
        assertFalse("UNDEFINED coordinate must be rejected",
            ModelSanitizer.isValidWaypoint(wp(UUID.randomUUID(), "X", WorldPointPacker.UNDEFINED)));
        assertFalse("(0,0) null-island must be rejected",
            ModelSanitizer.isValidWaypoint(wp(UUID.randomUUID(), "X", 0)));
    }

    @Test
    public void sanitizeWaypointsDropsOnlyInvalid()
    {
        Waypoint good = wp(UUID.randomUUID(), "Bank", 42);
        List<Waypoint> in = Arrays.asList(good, wp(null, "NoId", 42), wp(UUID.randomUUID(), "Zero", 0));
        List<Waypoint> out = ModelSanitizer.sanitizeWaypoints(in);
        assertEquals(1, out.size());
        assertEquals("Bank", out.get(0).getName());
    }

    @Test
    public void sanitizeWaypointsHandlesNullList()
    {
        assertTrue(ModelSanitizer.sanitizeWaypoints(null).isEmpty());
    }

    @Test
    public void dropsCategoryWithNullIdOrName()
    {
        assertFalse(ModelSanitizer.isValidCategory(cat(null, "Bossing")));
        assertFalse(ModelSanitizer.isValidCategory(cat(UUID.randomUUID(), null)));
    }

    @Test
    public void keepsUncategorizedSentinelRegardlessOfFields()
    {
        Category sentinel = new Category(null, null, 0, true, null, false);
        assertTrue("uncategorized sentinel must never be dropped",
            ModelSanitizer.isValidCategory(sentinel));
    }

    @Test
    public void sanitizeCategoriesDropsOnlyInvalid()
    {
        Category good = cat(UUID.randomUUID(), "Bossing");
        List<Category> in = new ArrayList<>(Arrays.asList(good, cat(null, "Bad"), cat(UUID.randomUUID(), null)));
        List<Category> out = ModelSanitizer.sanitizeCategories(in);
        assertEquals(1, out.size());
        assertEquals("Bossing", out.get(0).getName());
    }
}
