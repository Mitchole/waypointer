package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaypointFilterTest
{
    private static Category cat(String name)
    {
        return new Category(UUID.randomUUID(), name, 0, false, null, false);
    }

    private static Waypoint wp(String name, String notes, UUID catId)
    {
        return new Waypoint(UUID.randomUUID(), name, 0, catId, null,
            notes == null ? "" : notes, Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);
    }

    @Test
    public void emptyFilterMatchesEverything()
    {
        Category c = cat("Bossing");
        Waypoint w = wp("Vorkath", "drops dragonbone", c.getId());
        assertTrue(WaypointFilter.matches(w, c, ""));
        assertTrue(WaypointFilter.matches(w, c, null));
    }

    @Test
    public void nameMatchIsCaseInsensitive()
    {
        Category c = cat("Bossing");
        Waypoint w = wp("Vorkath", "", c.getId());
        assertTrue(WaypointFilter.matches(w, c, "vork"));
        assertTrue(WaypointFilter.matches(w, c, "VORK"));
        assertTrue(WaypointFilter.matches(w, c, "VorK"));
    }

    @Test
    public void notesMatchIsCaseInsensitive()
    {
        Category c = cat("Bossing");
        Waypoint w = wp("Vorkath", "Drops dragonbones for prayer", c.getId());
        assertTrue(WaypointFilter.matches(w, c, "prayer"));
        assertTrue(WaypointFilter.matches(w, c, "DRAGON"));
    }

    @Test
    public void categoryNameMatchReturnsTrueRegardlessOfWaypointContents()
    {
        Category c = cat("Bossing");
        Waypoint w = wp("Vorkath", "drops dragonbone", c.getId());
        // "boss" is not in waypoint name or notes, but is in category name.
        assertTrue(WaypointFilter.matches(w, c, "boss"));
    }

    @Test
    public void nonMatchReturnsFalse()
    {
        Category c = cat("Bossing");
        Waypoint w = wp("Vorkath", "drops dragonbones", c.getId());
        assertFalse(WaypointFilter.matches(w, c, "skilling"));
        assertFalse(WaypointFilter.matches(w, c, "xyz"));
    }

    @Test
    public void nullCategoryStillMatchesOnWaypointFields()
    {
        Waypoint w = wp("Vorkath", "drops dragonbone", UUID.randomUUID());
        assertTrue(WaypointFilter.matches(w, null, "vork"));
        assertFalse(WaypointFilter.matches(w, null, "boss"));
    }

    @Test
    public void categoryNameMatchesHelper()
    {
        Category c = cat("Bossing");
        assertTrue(WaypointFilter.categoryNameMatches(c, "boss"));
        assertTrue(WaypointFilter.categoryNameMatches(c, "BOSS"));
        assertFalse(WaypointFilter.categoryNameMatches(c, "skilling"));
        // Empty / null filter is treated as "match all".
        assertTrue(WaypointFilter.categoryNameMatches(c, ""));
        assertTrue(WaypointFilter.categoryNameMatches(c, null));
    }

    @Test
    public void nullWaypointFieldsDoNotCrash()
    {
        Category c = cat("Bossing");
        // Use raw constructor to make name/notes null.
        Waypoint w = new Waypoint(UUID.randomUUID(), null, 0, c.getId(), null, null,
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);
        // Filter that matches category name still passes.
        assertTrue(WaypointFilter.matches(w, c, "boss"));
        // Filter that matches nothing returns false without NPE.
        assertFalse(WaypointFilter.matches(w, c, "vork"));
    }
}
