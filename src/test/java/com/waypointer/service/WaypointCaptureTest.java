package com.waypointer.service;

import com.waypointer.model.Library;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.Waypoint;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WaypointCaptureTest
{
    private Client client;
    private WaypointStore store;
    private WaypointCapture capture;

    @Before
    public void setUp()
    {
        client = mock(Client.class);
        store = new WaypointStore();
        store.bootstrap(new Library());
        ClientThread clientThread = mock(ClientThread.class);
        LandmarkLookup lookup = new LandmarkLookup();
        capture = new WaypointCapture(client, store, clientThread, lookup);
    }

    /**
     * The method below is package-private and runs synchronously (caller is supposed to be on
     * the client thread). Testing it directly avoids the thread-hop+EDT-callback dance.
     */
    @Test
    public void readReturnsNullWhenLoggedOut()
    {
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        assertNull(capture.readCurrentPackedPointOnClientThread());
    }

    @Test
    public void readReturnsNullWhenLocalPlayerMissing()
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(null);
        assertNull(capture.readCurrentPackedPointOnClientThread());
    }

    @Test
    public void readReturnsPackedTileWhenLoggedIn()
    {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        Player p = mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(p);
        when(p.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));

        Integer packed = capture.readCurrentPackedPointOnClientThread();
        assertNotNull(packed);
        assertEquals(WorldPointPacker.pack(3200, 3200, 0), packed.intValue());
    }

    @Test
    public void captureFromPackedPointLandsInUncategorizedWithCoordsName()
    {
        int packed = WorldPointPacker.pack(2500, 9800, 1);
        Waypoint w = capture.captureFromPackedPoint(packed);
        assertNotNull(w);
        assertEquals("(2500, 9800)", w.getName());
        assertEquals(packed, w.getPackedWorldPoint());
        assertEquals(store.getUncategorized().getId(), w.getCategoryId());
    }
}
