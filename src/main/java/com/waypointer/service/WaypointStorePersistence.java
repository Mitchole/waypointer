package com.waypointer.service;

import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Library;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Per-account library persistence backed by the RuneLite config system (RSProfile-scoped),
 * replacing the old on-disk library.json. {@link #load()} / {@link #save(Library)} read and write a
 * single JSON blob under (group {@code waypointer}, key {@code library}) for the logged-in account;
 * {@link #clear()} unsets it. A decode failure freezes saves so a corrupt value is not overwritten
 * before the user resets via the panel banner.
 */
@Slf4j
@Singleton
public class WaypointStorePersistence
{
    static final String GROUP = "waypointer";
    static final String KEY = "library";

    private final ConfigManager configManager;
    private final LibraryJsonCodec codec;
    private volatile boolean refuseSavesUntilReset = false;

    @Inject
    public WaypointStorePersistence(ConfigManager configManager, LibraryJsonCodec codec)
    {
        this.configManager = configManager;
        this.codec = codec;
    }

    /**
     * Loads the current account's library from RSProfile config. Returns an empty library when not
     * logged in (no RS profile) or when nothing has been saved. Only a decode failure (corruption)
     * freezes saves; an absent value is a clean empty.
     */
    public Library load()
    {
        String json = configManager.getRSProfileConfiguration(GROUP, KEY);
        if (json == null || json.isEmpty()) return new Library();
        try
        {
            return codec.decode(json);
        }
        catch (RuntimeException e)
        {
            log.error("Library config value unreadable; refusing saves until reset", e);
            refuseSavesUntilReset = true;
            return new Library();
        }
    }

    /** Write-through save of the live library to the logged-in account's RSProfile config. */
    public void save(Library lib)
    {
        if (refuseSavesUntilReset)
        {
            log.warn("Save refused: library config is in a corrupt state pending user reset");
            return;
        }
        configManager.setRSProfileConfiguration(GROUP, KEY, codec.encode(lib));
    }

    /** Clears the current account's stored library and lifts the corrupt-state freeze. */
    public void clear()
    {
        configManager.unsetRSProfileConfiguration(GROUP, KEY);
        refuseSavesUntilReset = false;
    }

    public boolean isRefusingSaves() { return refuseSavesUntilReset; }

    public void allowSavesAfterReset() { refuseSavesUntilReset = false; }
}
