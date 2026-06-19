package com.waypointer.sailing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SailingBoatsTest
{
    @Mock private Client client;
    private SailingBoats boats;

    @Before
    public void setUp()
    {
        boats = new SailingBoats(client);
        // Default everything to 0 (no boats) so individual tests only set what they need.
        lenient().when(client.getVarbitValue(org.mockito.ArgumentMatchers.anyInt())).thenReturn(0);
    }

    @Test
    public void boardedReadsVarbit()
    {
        when(client.getVarbitValue(VarbitID.SAILING_BOARDED_BOAT)).thenReturn(1);
        assertTrue(boats.isBoarded());
    }

    @Test
    public void unnamedDockedBoatGetsSlotLabelAndDock()
    {
        // Slot 1 owned, docked at Port Sarim (port id 0), no name set.
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_OWNED)).thenReturn(1);
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_PORT)).thenReturn(0);
        List<SailingBoats.BoatInfo> moored = boats.mooredBoats();
        assertEquals(1, moored.size());
        assertEquals(SailingDock.PORT_SARIM, moored.get(0).dock);
        assertEquals("Boat 1", moored.get(0).name);
    }

    @Test
    public void bottledBoatIsNotMoored()
    {
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_OWNED)).thenReturn(1);
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_PORT)).thenReturn(255); // Bottled sentinel
        assertTrue(boats.mooredBoats().isEmpty());
        assertNull(boats.boat(1));
    }

    @Test
    public void composesNameFromOptionTables()
    {
        // Slot 1 owned at Catherby (port 6); name indices prefix=2, descriptor=0(none), noun=2.
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_OWNED)).thenReturn(1);
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_PORT)).thenReturn(6);
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_NAME_1)).thenReturn(2);
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_NAME_2)).thenReturn(1); // -> index 0, skipped
        when(client.getVarbitValue(VarbitID.SAILING_BOAT_1_NAME_3)).thenReturn(2);
        when(client.getDBTableField(
            DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_PREFIX_OPTIONS,
            DBTableID.SailingBoatNameOptions.COL_OPTION, 0))
            .thenReturn(new Object[]{ "", "Salty" });
        when(client.getDBTableField(
            DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_NOUN_OPTIONS,
            DBTableID.SailingBoatNameOptions.COL_OPTION, 0))
            .thenReturn(new Object[]{ "", "Gull" });
        assertEquals("Salty Gull", boats.boat(1).name);
    }
}
