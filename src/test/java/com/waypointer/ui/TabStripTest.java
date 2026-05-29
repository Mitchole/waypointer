package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;

public class TabStripTest
{
    @Test
    public void initialActiveTabIsMyWaypoints()
    {
        TabStrip strip = new TabStrip(t -> { });
        assertEquals(TabStrip.Tab.MY_WAYPOINTS, strip.getActive());
        assertActiveBorderIsBrandOrange(strip, TabStrip.Tab.MY_WAYPOINTS);
        assertInactiveBorderIsDarkerGray(strip, TabStrip.Tab.PRESETS);
    }

    @Test
    public void setActiveSwapsBorders()
    {
        TabStrip strip = new TabStrip(t -> { });
        strip.setActive(TabStrip.Tab.PRESETS);
        assertEquals(TabStrip.Tab.PRESETS, strip.getActive());
        assertActiveBorderIsBrandOrange(strip, TabStrip.Tab.PRESETS);
        assertInactiveBorderIsDarkerGray(strip, TabStrip.Tab.MY_WAYPOINTS);
    }

    @Test
    public void clickFiresCallbackWithCorrectTab()
    {
        @SuppressWarnings("unchecked")
        Consumer<TabStrip.Tab> callback = mock(Consumer.class);
        TabStrip strip = new TabStrip(callback);

        clickLabel(strip.labelFor(TabStrip.Tab.PRESETS));
        verify(callback, times(1)).accept(TabStrip.Tab.PRESETS);

        clickLabel(strip.labelFor(TabStrip.Tab.MY_WAYPOINTS));
        verify(callback, times(1)).accept(TabStrip.Tab.MY_WAYPOINTS);
    }

    @Test
    public void labelsExistForBothTabs()
    {
        TabStrip strip = new TabStrip(t -> { });
        assertNotNull(strip.labelFor(TabStrip.Tab.MY_WAYPOINTS));
        assertNotNull(strip.labelFor(TabStrip.Tab.PRESETS));
    }

    @Test
    public void variableTabConstructorRendersThreeTabs()
    {
        TabStrip strip = new TabStrip(t -> {},
            java.util.Arrays.asList(TabStrip.Tab.MY_WAYPOINTS,
                TabStrip.Tab.PRESETS, TabStrip.Tab.DEV));
        assertNotNull(strip.labelFor(TabStrip.Tab.DEV));
    }

    private static void assertActiveBorderIsBrandOrange(TabStrip strip, TabStrip.Tab tab)
    {
        JLabel l = strip.labelFor(tab);
        assertNotNull("label must have a border", l.getBorder());
        CompoundBorder cb = (CompoundBorder) l.getBorder();
        MatteBorder mb = (MatteBorder) cb.getOutsideBorder();
        assertSame(ColorScheme.BRAND_ORANGE, mb.getMatteColor());
    }

    private static void assertInactiveBorderIsDarkerGray(TabStrip strip, TabStrip.Tab tab)
    {
        JLabel l = strip.labelFor(tab);
        CompoundBorder cb = (CompoundBorder) l.getBorder();
        MatteBorder mb = (MatteBorder) cb.getOutsideBorder();
        assertSame(ColorScheme.DARKER_GRAY_COLOR, mb.getMatteColor());
    }

    private static void clickLabel(JLabel label)
    {
        MouseEvent click = new MouseEvent(label, MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(), 0, 5, 5, 1, false);
        for (java.awt.event.MouseListener ml : label.getMouseListeners())
        {
            ml.mouseClicked(click);
        }
    }
}
