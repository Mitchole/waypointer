package com.waypointer.preset;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.waypointer.util.Listeners;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private List<Preset> cached;
    private final Listeners listeners = new Listeners();

    @Inject
    public PresetCatalog(Gson gson)
    {
        this.gson = gson;
    }

    public Listeners.Subscription subscribe(Runnable r)
    {
        return listeners.subscribe(r);
    }

    /** Every bundled preset. Loaded on first access and cached. Empty if the resource is missing. */
    public List<Preset> getPresets()
    {
        if (cached == null)
        {
            cached = loadBundled();
        }
        return cached;
    }

    public static PresetCatalog forTesting(Gson gson, List<Preset> bundled)
    {
        PresetCatalog c = new PresetCatalog(gson);
        c.cached = new ArrayList<>(bundled);
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
    public List<Preset> parse(String json)
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
