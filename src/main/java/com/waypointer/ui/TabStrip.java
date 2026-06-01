package com.waypointer.ui;

import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.client.ui.FontManager;

// Top-of-TabHost strip. Bold font, 2-px underline. The variable-tab constructor lets the
// host include the Dev tab only when dev mode is on.
final class TabStrip extends EnumTabStrip<TabStrip.Tab>
{
    enum Tab { MY_WAYPOINTS, PRESETS, ROUTES, DEV }

    TabStrip(Consumer<Tab> onSelect)
    {
        this(onSelect, Arrays.asList(Tab.MY_WAYPOINTS, Tab.PRESETS));
    }

    TabStrip(Consumer<Tab> onSelect, List<Tab> visible)
    {
        super(Tab.class, visible, Tab.MY_WAYPOINTS, onSelect, TabStrip::displayName,
            FontManager.getRunescapeBoldFont(), 2, new Insets(8, 0, 8, 0));
    }

    private static String displayName(Tab t)
    {
        switch (t)
        {
            case MY_WAYPOINTS: return "My waypoints";
            case PRESETS:      return "Presets";
            case ROUTES:       return "Routes";
            case DEV:          return "Dev";
        }
        return t.name();
    }
}
