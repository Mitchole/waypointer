package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;

// Verifies the generic EnumTabStrip behaviour through the DevSubTabStrip subclass:
// initial active state, border swap on setActive, the 1-px underline thickness, and the
// click-fires-callback seam.
public class EnumTabStripTest
{
    @Test
    public void initialActiveTabIsLandmarks()
    {
        DevSubTabStrip strip = new DevSubTabStrip(t -> { });
        assertEquals(DevSubTabStrip.SubTab.LANDMARKS, strip.getActive());
        assertSame(ColorScheme.BRAND_ORANGE, underline(strip, DevSubTabStrip.SubTab.LANDMARKS).getMatteColor());
        assertSame(ColorScheme.DARKER_GRAY_COLOR, underline(strip, DevSubTabStrip.SubTab.PRESETS).getMatteColor());
    }

    @Test
    public void setActiveSwapsBorders()
    {
        DevSubTabStrip strip = new DevSubTabStrip(t -> { });
        strip.setActive(DevSubTabStrip.SubTab.PRESETS);
        assertEquals(DevSubTabStrip.SubTab.PRESETS, strip.getActive());
        assertSame(ColorScheme.BRAND_ORANGE, underline(strip, DevSubTabStrip.SubTab.PRESETS).getMatteColor());
        assertSame(ColorScheme.DARKER_GRAY_COLOR, underline(strip, DevSubTabStrip.SubTab.LANDMARKS).getMatteColor());
    }

    @Test
    public void underlineThicknessIsOnePixel()
    {
        DevSubTabStrip strip = new DevSubTabStrip(t -> { });
        assertEquals(1, underline(strip, DevSubTabStrip.SubTab.LANDMARKS).getBorderInsets(strip).bottom);
    }

    @Test
    public void clickFiresCallbackWithCorrectTab()
    {
        @SuppressWarnings("unchecked")
        Consumer<DevSubTabStrip.SubTab> callback = mock(Consumer.class);
        DevSubTabStrip strip = new DevSubTabStrip(callback);
        clickLabel(strip.labelFor(DevSubTabStrip.SubTab.PRESETS));
        verify(callback, times(1)).accept(DevSubTabStrip.SubTab.PRESETS);
    }

    @Test
    public void labelsExistForBothTabs()
    {
        DevSubTabStrip strip = new DevSubTabStrip(t -> { });
        assertNotNull(strip.labelFor(DevSubTabStrip.SubTab.LANDMARKS));
        assertNotNull(strip.labelFor(DevSubTabStrip.SubTab.PRESETS));
    }

    private static MatteBorder underline(DevSubTabStrip strip, DevSubTabStrip.SubTab tab)
    {
        JLabel l = strip.labelFor(tab);
        assertNotNull("label must have a border", l.getBorder());
        return (MatteBorder) ((CompoundBorder) l.getBorder()).getOutsideBorder();
    }

    private static void clickLabel(JLabel label)
    {
        MouseEvent click = new MouseEvent(label, MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(), 0, 5, 5, 1, false);
        for (MouseListener ml : label.getMouseListeners())
        {
            ml.mouseClicked(click);
        }
    }
}
