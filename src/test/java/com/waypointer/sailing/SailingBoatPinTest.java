package com.waypointer.sailing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SailingBoatPinTest
{
    @Mock private ConfigManager configManager;
    private SailingBoatPin pin;

    @Before
    public void setUp() { pin = new SailingBoatPin(configManager); }

    @Test
    public void readsEnabledSlotAndName()
    {
        when(configManager.getRSProfileConfiguration("waypointer", "sailingUseSpecificBoat")).thenReturn("true");
        when(configManager.getRSProfileConfiguration("waypointer", "sailingPinnedBoatSlot")).thenReturn("3");
        when(configManager.getRSProfileConfiguration("waypointer", "sailingPinnedBoatName")).thenReturn("Salty Gull");
        assertTrue(pin.isEnabled());
        assertEquals(3, pin.slot());
        assertEquals("Salty Gull", pin.name());
    }

    @Test
    public void defaultsWhenUnset()
    {
        assertFalse(pin.isEnabled());
        assertEquals(-1, pin.slot());
    }

    @Test
    public void writesAllThreeKeys()
    {
        pin.set(true, 2, "Brave Cog");
        verify(configManager).setRSProfileConfiguration("waypointer", "sailingUseSpecificBoat", "true");
        verify(configManager).setRSProfileConfiguration("waypointer", "sailingPinnedBoatSlot", "2");
        verify(configManager).setRSProfileConfiguration("waypointer", "sailingPinnedBoatName", "Brave Cog");
    }
}
