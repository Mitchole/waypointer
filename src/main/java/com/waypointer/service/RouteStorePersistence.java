package com.waypointer.service;

import com.waypointer.codec.RouteJsonCodec;
import com.waypointer.model.route.RouteLibrary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Atomic load/save for routes.json (account-global, single slot). Falls back to a .bak on primary
 * corruption; on both unreadable returns an empty library. Mirrors {@link WaypointStorePersistence}
 * but without profile slots.
 */
@Slf4j
@Singleton
public class RouteStorePersistence implements JsonSnapshotSink<RouteLibrary>
{
    static final String ROUTES_FILENAME = "routes.json";
    static final String BACKUP_FILENAME = "routes.json.bak";

    private final Path dir;
    private final RouteJsonCodec codec;
    private volatile boolean refuseSavesUntilReset = false;

    @Inject
    public RouteStorePersistence(RouteJsonCodec codec)
    {
        this(RuneLite.RUNELITE_DIR.toPath().resolve("waypointer"), codec);
    }

    public RouteStorePersistence(Path dir, RouteJsonCodec codec)
    {
        this.dir = dir;
        this.codec = codec;
        try { Files.createDirectories(dir); }
        catch (IOException e) { log.warn("Could not create waypointer dir {}", dir, e); }
    }

    public Path routesFile() { return dir.resolve(ROUTES_FILENAME); }
    public Path backupFile() { return dir.resolve(BACKUP_FILENAME); }

    public boolean isRefusingSaves() { return refuseSavesUntilReset; }

    /** Clears the corrupt-state freeze so the next save proceeds; driven by the Routes reset banner. */
    public void allowSavesAfterReset() { refuseSavesUntilReset = false; }

    // tryLoad returns null for an absent OR transiently-unreadable file (a clean miss) and only
    // sets refuseSavesUntilReset on confirmed parse corruption. So a transient IO error falls
    // through to an empty library with saves still enabled; only real corruption freezes saves.
    public RouteLibrary loadOrEmpty()
    {
        RouteLibrary primary = tryLoad(routesFile());
        if (primary != null) return primary;

        log.warn("Primary routes file unreadable; trying backup at {}", backupFile());
        RouteLibrary backup = tryLoad(backupFile());
        if (backup != null)
        {
            log.warn("Loaded routes from backup; will overwrite primary on next save");
            return backup;
        }
        return new RouteLibrary();
    }

    private RouteLibrary tryLoad(Path f)
    {
        String json = AtomicJsonFile.tryRead(f);
        if (json == null) return null;
        try
        {
            return codec.decode(json);
        }
        catch (RuntimeException e)
        {
            log.warn("Parse failure reading {}: {}", f, e.getMessage());
            refuseSavesUntilReset = true;
            return null;
        }
    }

    @Override
    public String serialize(RouteLibrary lib)
    {
        return codec.encode(lib);
    }

    @Override
    public boolean writeBlocking(String json)
    {
        if (refuseSavesUntilReset)
        {
            log.warn("Route save refused: files are in a corrupt state pending reset");
            return false;
        }
        Path tmp = dir.resolve(ROUTES_FILENAME + ".tmp");
        return AtomicJsonFile.write(tmp, routesFile(), backupFile(), json);
    }

    @Override
    public boolean saveBlocking(RouteLibrary lib)
    {
        return writeBlocking(serialize(lib));
    }
}
