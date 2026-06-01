package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class NpcHighlightOverlayTest
{
    @Test
    public void returnsNpcNameForActiveNpcWaypoint()
    {
        int packed = WorldPointPacker.pack(3200, 3200, 0);
        Map<Integer, String> snapshot = new HashMap<>();
        snapshot.put(packed, "Banker");

        assertEquals("Banker", NpcHighlightOverlay.activeNpcName(snapshot, packed));
    }

    @Test
    public void returnsNullWhenNoActivePath()
    {
        Map<Integer, String> snapshot = new HashMap<>();
        snapshot.put(WorldPointPacker.pack(3200, 3200, 0), "Banker");

        assertNull(NpcHighlightOverlay.activeNpcName(snapshot, WorldPointPacker.UNDEFINED));
    }

    @Test
    public void returnsNullWhenActiveTargetIsNotAnNpcWaypoint()
    {
        // Snapshot only ever holds NPC waypoints; a plain-tile target is simply absent.
        Map<Integer, String> snapshot = Collections.emptyMap();
        int packed = WorldPointPacker.pack(3200, 3200, 0);

        assertNull(NpcHighlightOverlay.activeNpcName(snapshot, packed));
    }
}
