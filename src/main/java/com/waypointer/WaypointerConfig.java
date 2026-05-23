package com.waypointer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("waypointer")
public interface WaypointerConfig extends Config
{
    @ConfigItem(
        keyName = "tileRightClickEnabled",
        name = "Right-click tiles to save",
        description = "Adds 'Save as Waypoint' to the right-click menu on tiles in the 3D world. "
            + "Requires holding Shift to avoid menu clutter.",
        position = 10
    )
    default boolean tileRightClickEnabled() { return false; }

    @ConfigItem(
        keyName = "showPathingBanner",
        name = "Show 'Pathing to' banner",
        description = "Show a status strip at the top of the panel while a path is active.",
        position = 20
    )
    default boolean showPathingBanner() { return true; }

    // Hidden:JSON-encoded map of categoryId -> collapsed boolean. Empty = all expanded.
    @ConfigItem(
        keyName = "categoryCollapsedJson",
        name = "",
        description = "",
        hidden = true
    )
    default String categoryCollapsedJson() { return "{}"; }

    @ConfigItem(keyName = "categoryCollapsedJson", name = "", description = "")
    void setCategoryCollapsedJson(String v);

    // Hidden:set when user dismisses the 'shortest-path missing' banner.
    @ConfigItem(
        keyName = "shortestPathBannerDismissed",
        name = "",
        description = "",
        hidden = true
    )
    default boolean shortestPathBannerDismissed() { return false; }

    @ConfigItem(keyName = "shortestPathBannerDismissed", name = "", description = "")
    void setShortestPathBannerDismissed(boolean v);
}
