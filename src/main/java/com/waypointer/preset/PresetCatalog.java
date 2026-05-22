package com.waypointer.preset;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
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

    @Inject
    public PresetCatalog(Gson gson)
    {
        this.gson = gson;
    }

    /** Every bundled preset. Returns an empty list if the resource is missing or unreadable. */
    public List<Preset> getPresets()
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
