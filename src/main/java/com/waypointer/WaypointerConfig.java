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
        keyName = "entityRightClickEnabled",
        name = "Right-click NPCs and objects to save",
        description = "Adds 'Save as Waypoint' to the right-click menu on NPCs and game objects. "
            + "Requires holding Shift. The NPC's name and tile at click time are used as the default.",
        position = 11
    )
    default boolean entityRightClickEnabled() { return false; }

    @ConfigItem(
        keyName = "showNearestLandmarkBar",
        name = "Show 'nearest landmark' bar",
        description = "Adds a row of one-click shortcuts in the sidebar to path to the nearest "
            + "landmark. Click the customize button to choose which landmark types appear. "
            + "Off hides the row entirely.",
        position = 15
    )
    default boolean showNearestLandmarkBar() { return true; }

    @ConfigItem(
        keyName = "showPathingBanner",
        name = "Show 'Pathing to' banner",
        description = "Show a status strip at the top of the panel while a path is active.",
        position = 20
    )
    default boolean showPathingBanner() { return true; }

    @ConfigItem(
        keyName = "confirmBeforeWildernessPlay",
        name = "Confirm before pathing into Wilderness",
        description = "Show a confirm dialog when the destination tile is inside the Wilderness.",
        position = 25
    )
    default boolean confirmBeforeWildernessPlay() { return true; }

    @ConfigItem(
        keyName = "showWildernessGlyph",
        name = "Show wilderness skull on row",
        description = "Prefix waypoint rows with a skull when the destination is in the Wilderness.",
        position = 26
    )
    default boolean showWildernessGlyph() { return true; }

    @ConfigItem(
        keyName = "autoPathOnDeath",
        name = "Auto-path to death location",
        description = "When you die, automatically start a path back to the tile you "
            + "died on. Requires the Shortest Path plugin.",
        position = 27
    )
    default boolean autoPathOnDeath() { return false; }

    @ConfigItem(
        keyName = "newestPinAtTop",
        name = "Newest pin at top",
        description = "Order pinned waypoints with the most recently pinned at the top. Off puts newest at the bottom.",
        position = 30
    )
    default boolean newestPinAtTop() { return true; }

    @ConfigItem(
        keyName = "routesEnabled",
        name = "Enable Routes",
        description = "Show the Routes tab for building and running guided multi-step routes "
            + "(farm runs, agility laps). Off hides the feature entirely.",
        position = 40
    )
    default boolean routesEnabled() { return true; }

    @ConfigItem(
        keyName = "routeAdvanceHotkey",
        name = "Route: next step hotkey",
        description = "Key that advances (or skips) the current route step while a route is "
            + "running. Only fires while a route is active.",
        position = 41
    )
    default net.runelite.client.config.Keybind routeAdvanceHotkey()
    {
        return net.runelite.client.config.Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "showRouteOverlay",
        name = "Show in-game route box",
        description = "Draw a read-only step box in the game window while a route is running.",
        position = 42
    )
    default boolean showRouteOverlay() { return true; }

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

    // Hidden: JSON-encoded LandmarkSelection (order + selected subset).
    // Empty seeds with the canonical default on first read.
    @ConfigItem(
        keyName = "landmarkSelectionJson",
        name = "",
        description = "",
        hidden = true
    )
    default String landmarkSelectionJson() { return ""; }

    @ConfigItem(keyName = "landmarkSelectionJson", name = "", description = "")
    void setLandmarkSelectionJson(String v);

    @ConfigItem(
        keyName = "devModeEnabled",
        name = "Dev mode",
        description = "Show a Dev tab for editing bundled landmark tiles and preset waypoints. "
            + "For plugin authors and contributors. Off for normal play.",
        position = 100
    )
    default boolean devModeEnabled() { return false; }
}
