package com.waypointer.preset;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PresetFileValidityTest
{
    @Test
    public void bundledPresetsAreInternallyConsistent()
    {
        List<Preset> presets = new PresetCatalog(new Gson()).getPresets();
        assertFalse("preset catalog must not be empty", presets.isEmpty());
        for (Preset p : presets)
        {
            assertNotNull("preset category must be set", p.getCategory());
            assertFalse("preset category must be non-blank", p.getCategory().trim().isEmpty());
            assertNotNull("preset waypoint list must be set", p.getWaypoints());
            for (PresetWaypoint wp : p.getWaypoints())
            {
                assertNotNull("waypoint name must be set", wp.getName());
                assertFalse("waypoint name must be non-blank", wp.getName().trim().isEmpty());
                assertTrue("x out of range: " + wp.getX(), wp.getX() >= 0 && wp.getX() <= 32767);
                assertTrue("y out of range: " + wp.getY(), wp.getY() >= 0 && wp.getY() <= 32767);
                assertTrue("plane out of range: " + wp.getPlane(),
                    wp.getPlane() >= 0 && wp.getPlane() <= 3);
            }
        }
    }
}
