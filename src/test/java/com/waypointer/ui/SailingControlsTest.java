package com.waypointer.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import com.waypointer.sailing.SailingBoatPin;
import com.waypointer.sailing.SailingBoats;
import com.waypointer.sailing.SailingDock;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;

public class SailingControlsTest
{
    @Test
    public void pinningABoatStoresItsBareName()
    {
        SailingBoatPin pin = mock(SailingBoatPin.class);
        SailingControls controls = new SailingControls(mock(SailingBoats.class), pin, mock(Client.class),
            mock(ClientThread.class));
        controls.render(Arrays.asList(
            new SailingBoats.BoatInfo(1, "Skiff", SailingDock.PORT_SARIM),
            new SailingBoats.BoatInfo(3, "Sloop", SailingDock.CATHERBY)), false, 3);

        item(controls.buildMenu(), "Sloop (Catherby)").doClick();

        verify(pin).set(true, 3, "Sloop");
    }

    @Test
    public void menuMarksThePinnedBoat()
    {
        SailingControls controls = new SailingControls(mock(SailingBoats.class), mock(SailingBoatPin.class),
            mock(Client.class), mock(ClientThread.class));
        controls.render(Arrays.asList(
            new SailingBoats.BoatInfo(1, "Skiff", SailingDock.PORT_SARIM),
            new SailingBoats.BoatInfo(3, "Sloop", SailingDock.CATHERBY)), true, 3);

        JMenu menu = controls.buildMenu();

        assertTrue(item(menu, "Sloop (Catherby)").isSelected());
        assertFalse(item(menu, "Skiff (Port Sarim)").isSelected());
        assertFalse(item(menu, SailingControls.NEAREST).isSelected());
    }

    @Test
    public void choosingNearestClearsThePin()
    {
        SailingBoatPin pin = mock(SailingBoatPin.class);
        SailingControls controls = new SailingControls(mock(SailingBoats.class), pin, mock(Client.class),
            mock(ClientThread.class));
        controls.render(Arrays.asList(
            new SailingBoats.BoatInfo(3, "Sloop", SailingDock.CATHERBY)), true, 3);

        item(controls.buildMenu(), SailingControls.NEAREST).doClick();

        verify(pin).set(false, -1, "");
        assertTrue(item(controls.buildMenu(), SailingControls.NEAREST).isSelected());
    }

    @Test
    public void menuWithNoBoatsOffersOnlyTheDefault()
    {
        SailingControls controls = new SailingControls(mock(SailingBoats.class), mock(SailingBoatPin.class),
            mock(Client.class), mock(ClientThread.class));

        JMenu menu = controls.buildMenu();

        assertTrue(item(menu, SailingControls.NEAREST).isSelected());
        assertFalse(item(menu, "No boats moored at a port").isEnabled());
    }

    private static JMenuItem item(JMenu menu, String text)
    {
        for (java.awt.Component c : menu.getMenuComponents())
        {
            if (c instanceof JMenuItem && text.equals(((JMenuItem) c).getText())) return (JMenuItem) c;
        }
        throw new AssertionError("no menu item: " + text);
    }

    @Test
    public void refreshDoesNothingWhenNotLoggedIn()
    {
        SailingBoats boats = mock(SailingBoats.class);
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        SailingControls controls = new SailingControls(boats, mock(SailingBoatPin.class), client, clientThread);

        controls.refresh();

        verify(clientThread, never()).invoke(any(BooleanSupplier.class));
        verify(clientThread, never()).invoke(any(Runnable.class));
        verifyNoInteractions(boats);
    }

    @Test
    public void refreshReadsTheBoatsOnceLoggedIn()
    {
        Client client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        SailingControls controls = new SailingControls(mock(SailingBoats.class), mock(SailingBoatPin.class),
            client, clientThread);

        controls.refresh();

        verify(clientThread).invoke(any(BooleanSupplier.class));
    }
}
