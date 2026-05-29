package com.waypointer.service;

import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.PresetImportResolver.Choice;
import com.waypointer.service.PresetImportResolver.PendingConflict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PresetImportConflictTest
{
    @Test
    public void noConflictsStageAllAsAdds()
    {
        List<Preset> current = new ArrayList<>();
        List<Preset> imported = Arrays.asList(new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("Vorkath", "", 1, 1, 0))));
        PresetImportResolver r = new PresetImportResolver(current, imported);
        assertTrue(r.conflicts().isEmpty());
        assertEquals(1, r.staged().size());
    }

    @Test
    public void duplicateWaypointSurfacesConflict()
    {
        List<Preset> current = Arrays.asList(new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("Vorkath", "old", 1, 1, 0))));
        List<Preset> imported = Arrays.asList(new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("Vorkath", "new", 2, 2, 0))));
        PresetImportResolver r = new PresetImportResolver(current, imported);
        assertEquals(1, r.conflicts().size());
    }

    @Test
    public void replaceResolutionStagesImport()
    {
        List<Preset> current = Arrays.asList(new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("Vorkath", "old", 1, 1, 0))));
        List<Preset> imported = Arrays.asList(new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("Vorkath", "new", 2, 2, 0))));
        PresetImportResolver r = new PresetImportResolver(current, imported);
        for (PendingConflict c : r.conflicts()) r.resolve(c, Choice.REPLACE);
        assertEquals(1, r.staged().size());
        assertEquals("new", r.staged().get(0).getDescription());
    }

    @Test
    public void skipResolutionStagesNothing()
    {
        List<Preset> current = Arrays.asList(new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("Vorkath", "old", 1, 1, 0))));
        List<Preset> imported = Arrays.asList(new Preset("Bosses", null, null,
            Arrays.asList(new PresetWaypoint("Vorkath", "new", 2, 2, 0))));
        PresetImportResolver r = new PresetImportResolver(current, imported);
        for (PendingConflict c : r.conflicts()) r.resolve(c, Choice.SKIP);
        assertTrue(r.staged().isEmpty());
    }
}
