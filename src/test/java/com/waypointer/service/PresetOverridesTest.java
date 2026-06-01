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
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    @Test
    public void upsertWaypointAddsWhenAbsent()
    {
        PresetOverrides ov = PresetOverrides.forTesting(GSON);
        ov.upsertWaypoint("Bosses", null, new Waypoint("Vorkath", "", 1, 2, 0));
        CategoryOverride bosses = ov.getSnapshot().getByCategory().get("Bosses");
        assertEquals(1, bosses.getWaypoints().size());
    }

    @Test
    public void upsertWaypointReplacesByOriginalTuple()
    {
        PresetOverrides ov = PresetOverrides.forTesting(GSON);
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
        PresetOverrides ov = PresetOverrides.forTesting(GSON);
        assertTrue(ov.addCategory(new CategoryOverride("Bosses", "", null, new ArrayList<>())));
        assertFalse(ov.addCategory(new CategoryOverride("Bosses", "", null, new ArrayList<>())));
    }

    @Test
    public void deleteBundledWaypointAppendsToDeletions()
    {
        PresetOverrides ov = PresetOverrides.forTesting(GSON);
        ov.deleteBundledWaypoint("Skilling", "Bad", 1, 2, 0);
        assertEquals(1, ov.getSnapshot().getDeletedWaypoints().size());
    }

    @Test
    public void listenersFireOnMutation()
    {
        PresetOverrides ov = PresetOverrides.forTesting(GSON);
        int[] count = {0};
        Subscription sub = ov.subscribe(() -> count[0]++);
        ov.upsertWaypoint("Bosses", null, new Waypoint("V", "", 1, 1, 0));
        assertEquals(1, count[0]);
        sub.close();
    }

    @Test
    public void undoLastRestoresPriorSnapshot()
    {
        PresetOverrides ov = PresetOverrides.forTesting(GSON);
        ov.upsertWaypoint("Bosses", null, new Waypoint("A", "", 1, 1, 0));
        ov.upsertWaypoint("Bosses", null, new Waypoint("B", "", 2, 2, 0));
        assertTrue(ov.undoLast());
        assertEquals(1, ov.getSnapshot().getByCategory().get("Bosses").getWaypoints().size());
    }

    @Test
    public void undoTwiceIsNoOp()
    {
        PresetOverrides ov = PresetOverrides.forTesting(GSON);
        ov.upsertWaypoint("Bosses", null, new Waypoint("A", "", 1, 1, 0));
        assertTrue(ov.undoLast());
        assertFalse(ov.undoLast());
    }

    @Test
    public void blockingSaveAndLoadRoundTrip() throws Exception
    {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("po-disk");
        com.waypointer.codec.PresetOverridesCodec codec =
            new com.waypointer.codec.PresetOverridesCodec(new com.google.gson.Gson());
        PresetOverrides ov = PresetOverrides.forTesting(dir, codec);
        ov.upsertWaypoint("Bosses", null, new Waypoint("V", "", 9, 9, 0));
        ov.flushBlocking();

        PresetOverrides reread = PresetOverrides.forTesting(dir, codec);
        reread.loadFromDisk();
        assertEquals(1, reread.getSnapshot().getByCategory().get("Bosses").getWaypoints().size());

        ov.close();
        reread.close();
        java.nio.file.Files.walk(dir).sorted(java.util.Comparator.reverseOrder())
            .forEach(p -> { try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignored) {} });
    }
}
