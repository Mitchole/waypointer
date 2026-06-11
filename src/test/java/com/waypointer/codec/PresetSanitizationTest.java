package com.waypointer.codec;

import com.google.gson.Gson;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetWaypoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Guards that every bundled preset waypoint survives ModelSanitizer. A preset waypoint at a
 * coordinate the sanitizer rejects (UNDEFINED or (0,0)) or with a null name would survive its
 * first session in memory, then vanish when the library is re-decoded on the next startup.
 */
public class PresetSanitizationTest
{
    @Test
    public void bundledPresetWaypointsAllSurviveSanitization()
    {
        List<PresetWaypoint> presetWaypoints = new ArrayList<>();
        for (Preset p : new PresetCatalog(new Gson()).getPresets())
        {
            presetWaypoints.addAll(p.getWaypoints());
        }
        assertFalse("preset catalog must not be empty", presetWaypoints.isEmpty());

        List<Waypoint> asLibrary = new ArrayList<>();
        for (PresetWaypoint pw : presetWaypoints)
        {
            asLibrary.add(new Waypoint(
                UUID.randomUUID(), pw.getName(),
                WorldPointPacker.pack(pw.getX(), pw.getY(), pw.getPlane()),
                UUID.randomUUID(), null, "", Instant.parse("2026-06-01T00:00:00Z"),
                0, false, null, false));
        }

        List<Waypoint> kept = ModelSanitizer.sanitizeWaypoints(asLibrary);

        assertEquals("a bundled preset waypoint would be dropped on reload",
            asLibrary.size(), kept.size());
    }
}
