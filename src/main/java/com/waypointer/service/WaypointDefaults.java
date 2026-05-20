package com.waypointer.service;

import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Library;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class WaypointDefaults
{
    static final String RESOURCE_PATH = "/com/waypointer/bundled-defaults.json";

    private final LibraryJsonCodec codec;
    private final WaypointStore store;

    @Inject
    public WaypointDefaults(LibraryJsonCodec codec, WaypointStore store)
    {
        this.codec = codec;
        this.store = store;
    }

    /** Loads the bundled defaults as a fresh Library. Returns an empty library on failure. */
    public Library loadBundled()
    {
        try (InputStream in = WaypointDefaults.class.getResourceAsStream(RESOURCE_PATH))
        {
            if (in == null)
            {
                log.warn("Bundled defaults resource not found at {}", RESOURCE_PATH);
                return new Library();
            }
            String json;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                json = r.lines().collect(Collectors.joining("\n"));
            }
            return codec.decode(json);
        }
        catch (IOException | RuntimeException e)
        {
            log.warn("Failed to load bundled defaults", e);
            return new Library();
        }
    }

    /** Loads the bundled defaults and merges them into the store. Idempotent (dedupes by UUID). */
    public WaypointStore.ImportResult importIntoStore()
    {
        Library bundled = loadBundled();
        return store.importMerge(bundled);
    }
}
