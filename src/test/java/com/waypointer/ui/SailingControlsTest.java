package com.waypointer.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.waypointer.sailing.SailingBoatPin;
import com.waypointer.sailing.SailingBoats;
import com.waypointer.sailing.SailingDock;
import java.util.Arrays;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;

public class SailingControlsTest
{
    @Test
    public void pinningABoatStoresItsBareName()
    {
        SailingBoatPin pin = mock(SailingBoatPin.class);
        SailingControls controls = new SailingControls(mock(SailingBoats.class), pin, mock(ClientThread.class));
        controls.render(Arrays.asList(
            new SailingBoats.BoatInfo(1, "Skiff", SailingDock.PORT_SARIM),
            new SailingBoats.BoatInfo(3, "Sloop", SailingDock.CATHERBY)), false, 3);

        controls.useSpecific.doClick();

        verify(pin).set(true, 3, "Sloop");
    }
}
