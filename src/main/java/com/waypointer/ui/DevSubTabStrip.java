package com.waypointer.ui;

import java.awt.Insets;
import java.util.Arrays;
import java.util.function.Consumer;
import net.runelite.client.ui.FontManager;

// Dev-tab sub-strip. Small font, 1-px underline, tighter insets than the main TabStrip.
final class DevSubTabStrip extends EnumTabStrip<DevSubTabStrip.SubTab>
{
    enum SubTab { LANDMARKS, PRESETS }

    DevSubTabStrip(Consumer<SubTab> onSelect)
    {
        super(SubTab.class, Arrays.asList(SubTab.LANDMARKS, SubTab.PRESETS),
            SubTab.LANDMARKS, onSelect, DevSubTabStrip::displayName,
            FontManager.getRunescapeSmallFont(), 1, new Insets(4, 0, 4, 0));
    }

    private static String displayName(SubTab t)
    {
        switch (t)
        {
            case LANDMARKS: return "Landmarks";
            case PRESETS:   return "Presets";
        }
        return t.name();
    }
}
