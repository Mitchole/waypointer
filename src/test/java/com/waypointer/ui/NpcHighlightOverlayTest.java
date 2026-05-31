package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class NpcHighlightOverlayTest
{
    private static Waypoint npcWp(int packed, String npcName)
    {
        Waypoint w = new Waypoint(UUID.randomUUID(), npcName, packed, null, null, "",
            Instant.parse("2026-05-02T12:00:00Z"), 0, false, null, false);
        w.setTargetNpcName(npcName);
        return w;
    }

    @Test
    public void returnsNpcNameForActiveNpcWaypoint()
    {
        Library lib = new Library();
        int packed = WorldPointPacker.pack(3200, 3200, 0);
        lib.getWaypoints().add(npcWp(packed, "Banker"));

        assertEquals("Banker", NpcHighlightOverlay.activeNpcName(lib, packed));
    }

    @Test
    public void returnsNullWhenNoActivePath()
    {
        Library lib = new Library();
        assertNull(NpcHighlightOverlay.activeNpcName(lib, WorldPointPacker.UNDEFINED));
    }

    @Test
    public void returnsNullWhenActiveTargetIsNotAnNpcWaypoint()
    {
        Library lib = new Library();
        int packed = WorldPointPacker.pack(3200, 3200, 0);
        lib.getWaypoints().add(new Waypoint(UUID.randomUUID(), "Tile", packed, null, null, "",
            Instant.parse("2026-05-02T12:00:00Z"), 0, false, null, false));
        assertNull(NpcHighlightOverlay.activeNpcName(lib, packed));
    }
}
