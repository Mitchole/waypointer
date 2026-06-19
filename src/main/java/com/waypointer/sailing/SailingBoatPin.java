package com.waypointer.sailing;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** The optional "always use a specific boat" pin, stored per-account (RSProfile) under the
 *  waypointer config group. Identifies the boat by slot (the only stable handle) plus a
 *  remembered name used to detect reordering. */
@Singleton
public class SailingBoatPin
{
    private static final String GROUP = "waypointer";
    private static final String KEY_ENABLED = "sailingUseSpecificBoat";
    private static final String KEY_SLOT = "sailingPinnedBoatSlot";
    private static final String KEY_NAME = "sailingPinnedBoatName";

    private final ConfigManager configManager;

    @Inject
    public SailingBoatPin(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    public boolean isEnabled()
    {
        return "true".equals(configManager.getRSProfileConfiguration(GROUP, KEY_ENABLED));
    }

    public int slot()
    {
        String s = configManager.getRSProfileConfiguration(GROUP, KEY_SLOT);
        if (s == null || s.isEmpty()) return -1;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return -1; }
    }

    public String name()
    {
        return configManager.getRSProfileConfiguration(GROUP, KEY_NAME);
    }

    public void set(boolean enabled, int slot, String name)
    {
        configManager.setRSProfileConfiguration(GROUP, KEY_ENABLED, Boolean.toString(enabled));
        configManager.setRSProfileConfiguration(GROUP, KEY_SLOT, Integer.toString(slot));
        configManager.setRSProfileConfiguration(GROUP, KEY_NAME, name == null ? "" : name);
    }
}
