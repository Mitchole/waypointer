package com.waypointer.ui;

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

        controls.useSpecific.doClick();

        verify(pin).set(true, 3, "Sloop");
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
