package com.waypointer.preset;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.waypointer.service.PresetOverrides;
import com.waypointer.service.PresetOverridesSnapshot;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Loads the bundled curated waypoint presets shipped in {@code preset-waypoints.json}. */
@Slf4j
@Singleton
public class PresetCatalog
{
    static final String RESOURCE_PATH = "/com/waypointer/preset-waypoints.json";

    private final Gson gson;
    private List<Preset> bundled = null;
    private List<Preset> cached;
    private PresetOverridesSnapshot lastOverrides = PresetOverridesSnapshot.empty();
    private final com.waypointer.util.Listeners listeners = new com.waypointer.util.Listeners();

    @Inject
    public PresetCatalog(Gson gson, PresetOverrides overrides)
    {
        this.gson = gson;
        applyOverrides(overrides.getSnapshot());
        overrides.subscribe(() -> reload(overrides.getSnapshot()));
    }

    public com.waypointer.util.Listeners.Subscription subscribe(Runnable r)
    {
        return listeners.subscribe(r);
    }

    public void reload(PresetOverridesSnapshot s)
    {
        applyOverrides(s);
        listeners.fire();
    }

    /**
     * Every bundled preset, with any dev-mode overrides layered on top. The bundled
     * resource is loaded on first access and cached; subsequent overrides rebuild
     * the cached final list without reparsing the resource. Returns an empty list
     * if the resource is missing or unreadable.
     */
    public List<Preset> getPresets()
    {
        if (cached == null)
        {
            rebuild();
        }
        return cached;
    }

    public void applyOverrides(PresetOverridesSnapshot s)
    {
        this.lastOverrides = s;
        rebuild();
    }

    private void rebuild()
    {
        if (bundled == null)
        {
            bundled = loadBundled();
        }
        LinkedHashMap<String, Preset> out = new LinkedHashMap<>();
        for (Preset p : bundled)
        {
            out.put(p.getCategory(), p);
        }

        for (Map.Entry<String, PresetOverridesSnapshot.CategoryOverride> e :
            lastOverrides.getByCategory().entrySet())
        {
            PresetOverridesSnapshot.CategoryOverride co = e.getValue();
            List<PresetWaypoint> wps = new ArrayList<>();
            for (PresetOverridesSnapshot.Waypoint w : co.getWaypoints())
            {
                wps.add(new PresetWaypoint(w.getName(), w.getDescription(), w.getX(), w.getY(), w.getPlane()));
            }
            out.put(e.getKey(), new Preset(e.getKey(), co.getDescription(), co.getIcon(), wps));
        }

        for (String name : lastOverrides.getDeletedCategories())
        {
            out.remove(name);
        }

        for (PresetOverridesSnapshot.DeletedWaypoint d : lastOverrides.getDeletedWaypoints())
        {
            if (lastOverrides.getByCategory().containsKey(d.getCategory()))
            {
                continue;
            }
            Preset p = out.get(d.getCategory());
            if (p == null)
            {
                continue;
            }
            List<PresetWaypoint> filtered = new ArrayList<>();
            for (PresetWaypoint w : p.getWaypoints())
            {
                if (w.getName().equals(d.getName())
                    && w.getX() == d.getX() && w.getY() == d.getY()
                    && w.getPlane() == d.getPlane())
                {
                    continue;
                }
                filtered.add(w);
            }
            out.put(d.getCategory(), new Preset(p.getCategory(), p.getDescription(), p.getIcon(), filtered));
        }

        for (PresetOverridesSnapshot.CategoryOverride co : lastOverrides.getAddedCategories())
        {
            List<PresetWaypoint> wps = new ArrayList<>();
            for (PresetOverridesSnapshot.Waypoint w : co.getWaypoints())
            {
                wps.add(new PresetWaypoint(w.getName(), w.getDescription(), w.getX(), w.getY(), w.getPlane()));
            }
            out.put(co.getCategory(), new Preset(co.getCategory(), co.getDescription(), co.getIcon(), wps));
        }

        cached = new ArrayList<>(out.values());
    }

    public static PresetCatalog forTesting(List<Preset> bundled)
    {
        PresetCatalog c = new PresetCatalog(new Gson(), PresetOverrides.forTesting());
        c.bundled = new ArrayList<>(bundled);
        c.rebuild();
        return c;
    }

    private List<Preset> loadBundled()
    {
        try (InputStream in = PresetCatalog.class.getResourceAsStream(RESOURCE_PATH))
        {
            if (in == null)
            {
                log.warn("Preset resource not found at {}", RESOURCE_PATH);
                return Collections.emptyList();
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                return parse(r.lines().collect(Collectors.joining("\n")));
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to load preset waypoints", e);
            return Collections.emptyList();
        }
    }

    /** Parses preset JSON into a list. Returns an empty list on malformed or empty input. */
    List<Preset> parse(String json)
    {
        try
        {
            PresetFile file = gson.fromJson(json, PresetFile.class);
            if (file == null || file.presets == null)
            {
                return Collections.emptyList();
            }
            List<Preset> out = new ArrayList<>();
            for (Preset p : file.presets)
            {
                if (p == null || p.getCategory() == null)
                {
                    continue;
                }
                List<PresetWaypoint> wps = p.getWaypoints() == null
                    ? Collections.emptyList() : p.getWaypoints();
                out.add(new Preset(p.getCategory(), p.getDescription(), p.getIcon(), wps));
            }
            return out;
        }
        catch (JsonParseException e)
        {
            log.warn("Malformed preset JSON", e);
            return Collections.emptyList();
        }
    }

    private static final class PresetFile
    {
        List<Preset> presets;
    }
}
