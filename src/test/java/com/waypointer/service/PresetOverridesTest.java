package com.waypointer.service;

import com.waypointer.service.PresetOverridesSnapshot.CategoryOverride;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import com.waypointer.util.Listeners.Subscription;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class PresetOverridesTest
{
    @Test
    public void upsertWaypointAddsWhenAbsent()
    {
        PresetOverrides ov = PresetOverrides.forTesting();
        ov.upsertWaypoint("Bosses", null, new Waypoint("Vorkath", "", 1, 2, 0));
        CategoryOverride bosses = ov.getSnapshot().getByCategory().get("Bosses");
        assertEquals(1, bosses.getWaypoints().size());
    }

    @Test
    public void upsertWaypointReplacesByOriginalTuple()
    {
        PresetOverrides ov = PresetOverrides.forTesting();
        Waypoint original = new Waypoint("Vorkath", "old", 1, 2, 0);
        ov.upsertWaypoint("Bosses", null, original);
        Waypoint updated = new Waypoint("Vorkath", "new", 9, 9, 0);
        ov.upsertWaypoint("Bosses", original, updated);
        Waypoint stored = ov.getSnapshot().getByCategory().get("Bosses").getWaypoints().get(0);
        assertEquals("new", stored.getDescription());
        assertEquals(9, stored.getX());
    }

    @Test
    public void addCategoryRejectsDuplicateName()
    {
        PresetOverrides ov = PresetOverrides.forTesting();
        assertTrue(ov.addCategory(new CategoryOverride("Bosses", "", null, new ArrayList<>())));
        assertFalse(ov.addCategory(new CategoryOverride("Bosses", "", null, new ArrayList<>())));
    }

    @Test
    public void deleteBundledWaypointAppendsToDeletions()
    {
        PresetOverrides ov = PresetOverrides.forTesting();
        ov.deleteBundledWaypoint("Skilling", "Bad", 1, 2, 0);
        assertEquals(1, ov.getSnapshot().getDeletedWaypoints().size());
    }

    @Test
    public void listenersFireOnMutation()
    {
        PresetOverrides ov = PresetOverrides.forTesting();
        int[] count = {0};
        Subscription sub = ov.subscribe(() -> count[0]++);
        ov.upsertWaypoint("Bosses", null, new Waypoint("V", "", 1, 1, 0));
        assertEquals(1, count[0]);
        sub.close();
    }
}
