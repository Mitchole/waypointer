package com.waypointer.service;

import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetWaypoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Stateless resolver: given currently-known presets and imported presets, compute conflicts
// (same category+name) and staged adds. Caller iterates conflicts() and calls resolve() for
// each; staged() returns the final list of imports to apply.
public final class PresetImportResolver
{
    public enum Choice { KEEP_EXISTING, REPLACE, SKIP }

    public static final class PendingConflict
    {
        public final String category;
        public final PresetWaypoint imported;
        public final PresetWaypoint existing;
        PendingConflict(String c, PresetWaypoint imp, PresetWaypoint ex)
        {
            this.category = c;
            this.imported = imp;
            this.existing = ex;
        }
    }

    private final List<PendingConflict> conflicts = new ArrayList<>();
    private final List<PresetWaypoint> staged = new ArrayList<>();
    private final Map<String, List<PresetWaypoint>> currentByCat = new HashMap<>();

    public PresetImportResolver(List<Preset> current, List<Preset> imported)
    {
        for (Preset p : current) currentByCat.put(p.getCategory(), p.getWaypoints());
        for (Preset p : imported)
        {
            List<PresetWaypoint> existing = currentByCat.getOrDefault(p.getCategory(), new ArrayList<>());
            for (PresetWaypoint imp : p.getWaypoints())
            {
                PresetWaypoint dup = findByName(existing, imp.getName());
                if (dup == null) staged.add(imp);
                else conflicts.add(new PendingConflict(p.getCategory(), imp, dup));
            }
        }
    }

    public List<PendingConflict> conflicts() { return conflicts; }
    public List<PresetWaypoint> staged() { return staged; }

    public void resolve(PendingConflict c, Choice choice)
    {
        if (choice == Choice.REPLACE) staged.add(c.imported);
    }

    private static PresetWaypoint findByName(List<PresetWaypoint> list, String name)
    {
        for (PresetWaypoint w : list) if (Objects.equals(w.getName(), name)) return w;
        return null;
    }
}
