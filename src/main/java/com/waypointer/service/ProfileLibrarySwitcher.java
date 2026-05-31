package com.waypointer.service;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Swaps the in-memory library when the logged-in RuneScape account changes, so each account keeps
 * its own waypoints. Slots are keyed by {@link ConfigManager#getRSProfileKey()} and live in
 * {@link WaypointStorePersistence}. This class owns the ordering that keeps a pending debounced
 * save from leaking the outgoing account's data into the incoming slot.
 */
@Slf4j
@Singleton
public class ProfileLibrarySwitcher
{
    static final String CONFIG_GROUP = "waypointer";
    static final String LAST_KEY = "lastRsProfileKey";

    private final WaypointStore store;
    private final WaypointStorePersistence persistence;
    private final ConfigManager configManager;

    @Inject
    public ProfileLibrarySwitcher(WaypointStore store, WaypointStorePersistence persistence,
        ConfigManager configManager)
    {
        this.store = store;
        this.persistence = persistence;
        this.configManager = configManager;
    }

    /**
     * Resolve the slot to load at startup: the live RS profile key when already logged in, else the
     * last account used (may be null, meaning the default slot).
     */
    public String resolveStartupKey(String liveRsProfileKey)
    {
        return liveRsProfileKey != null
            ? liveRsProfileKey
            : configManager.getConfiguration(CONFIG_GROUP, LAST_KEY);
    }

    /** One-time load at plugin startUp. Always bootstraps, even for the default slot. */
    public void initialize(String key)
    {
        log.debug("Initialising library slot for profile {}", key);
        persistence.switchProfile(key);
        // seedFromDefault() is a no-op for the default slot (null key); kept here so the
        // startup sequence is identical to a runtime switch.
        persistence.seedFromDefault();
        store.bootstrap(persistence.loadOrEmpty());
        if (key != null)
        {
            configManager.setConfiguration(CONFIG_GROUP, LAST_KEY, key);
        }
    }

    /**
     * Handle an account change. A null key (logout) is ignored so the last-active library stays
     * loaded. A key equal to the active slot is a no-op. Otherwise flush the outgoing account to
     * its slot, retarget, seed if new, reload, and remember the key.
     */
    public void switchToProfile(String newKey)
    {
        if (newKey == null)
        {
            return;
        }
        if (Objects.equals(newKey, persistence.getActiveProfileKey()))
        {
            return;
        }
        log.debug("Switching library slot from {} to {}", persistence.getActiveProfileKey(), newKey);
        store.flushPendingSave();
        persistence.switchProfile(newKey);
        persistence.seedFromDefault();
        store.bootstrap(persistence.loadOrEmpty());
        configManager.setConfiguration(CONFIG_GROUP, LAST_KEY, newKey);
    }
}
