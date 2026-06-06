package com.waypointer.service;

import com.waypointer.codec.RouteJsonCodec;
import com.waypointer.model.route.RouteLibrary;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Account-global route persistence backed by the RuneLite config system, replacing the old on-disk
 * routes.json. Stores a single JSON blob under (group {@code waypointer}, key {@code routes}). A
 * decode failure freezes saves until the routes reset banner clears it.
 */
@Slf4j
@Singleton
public class RouteStorePersistence
{
    static final String GROUP = "waypointer";
    static final String KEY = "routes";

    private final ConfigManager configManager;
    private final RouteJsonCodec codec;
    private volatile boolean refuseSavesUntilReset = false;

    @Inject
    public RouteStorePersistence(ConfigManager configManager, RouteJsonCodec codec)
    {
        this.configManager = configManager;
        this.codec = codec;
    }

    public RouteLibrary load()
    {
        String json = configManager.getConfiguration(GROUP, KEY);
        if (json == null || json.isEmpty()) return new RouteLibrary();
        try
        {
            return codec.decode(json);
        }
        catch (RuntimeException e)
        {
            log.error("Routes config value unreadable; refusing saves until reset", e);
            refuseSavesUntilReset = true;
            return new RouteLibrary();
        }
    }

    public void save(RouteLibrary lib)
    {
        if (refuseSavesUntilReset)
        {
            log.warn("Route save refused: config is in a corrupt state pending reset");
            return;
        }
        configManager.setConfiguration(GROUP, KEY, codec.encode(lib));
    }

    public void clear()
    {
        configManager.unsetConfiguration(GROUP, KEY);
        refuseSavesUntilReset = false;
    }

    public boolean isRefusingSaves() { return refuseSavesUntilReset; }

    public void allowSavesAfterReset() { refuseSavesUntilReset = false; }
}
