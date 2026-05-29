package com.waypointer.service;

import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
import com.waypointer.util.Listeners.Subscription;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LandmarkOverridesTest
{
    @Test
    public void addEntryAppendsToTypeOverride()
    {
        LandmarkOverrides ov = LandmarkOverrides.forTesting();
        ov.addEntry("BANK", new Entry("New bank", 1, 2, 1, 2, 0));
        TypeOverride t = ov.getSnapshot().getByType().get("BANK");
        assertEquals(1, t.getEntries().size());
        assertEquals("New bank", t.getEntries().get(0).getName());
    }

    @Test
    public void replaceEntryRewritesByOriginalTuple()
    {
        LandmarkOverrides ov = LandmarkOverrides.forTesting();
        ov.addEntry("BANK", new Entry("A", 1, 1, 1, 1, 0));
        ov.replaceEntry("BANK",
            new Entry("A", 1, 1, 1, 1, 0),
            new Entry("A", 5, 5, 5, 5, 0));
        TypeOverride t = ov.getSnapshot().getByType().get("BANK");
        assertEquals(1, t.getEntries().size());
        assertEquals(5, t.getEntries().get(0).getX1());
    }

    @Test
    public void deleteEntryFromBundled()
    {
        LandmarkOverrides ov = LandmarkOverrides.forTesting();
        ov.deleteBundledEntry("BANK", "Bad bank", 1, 1, 1, 1, 0);
        assertEquals(1, ov.getSnapshot().getDeletions().size());
    }

    @Test
    public void listenersFireOnMutation()
    {
        LandmarkOverrides ov = LandmarkOverrides.forTesting();
        int[] count = {0};
        Subscription sub = ov.subscribe(() -> count[0]++);
        ov.addEntry("BANK", new Entry("x", 1, 1, 1, 1, 0));
        assertEquals(1, count[0]);
        sub.close();
        ov.addEntry("BANK", new Entry("y", 2, 2, 2, 2, 0));
        assertEquals(1, count[0]);
    }

    @Test
    public void undoLastRestoresPriorSnapshot()
    {
        LandmarkOverrides ov = LandmarkOverrides.forTesting();
        ov.addEntry("BANK", new Entry("x", 1, 1, 1, 1, 0));
        ov.addEntry("BANK", new Entry("y", 2, 2, 2, 2, 0));
        assertTrue(ov.undoLast());
        assertEquals(1, ov.getSnapshot().getByType().get("BANK").getEntries().size());
        assertEquals("x", ov.getSnapshot().getByType().get("BANK").getEntries().get(0).getName());
    }

    @Test
    public void undoTwiceIsNoOp()
    {
        LandmarkOverrides ov = LandmarkOverrides.forTesting();
        ov.addEntry("BANK", new Entry("x", 1, 1, 1, 1, 0));
        assertTrue(ov.undoLast());
        assertFalse(ov.undoLast());
    }
}
