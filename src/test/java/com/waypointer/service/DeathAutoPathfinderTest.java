package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeathAutoPathfinderTest
{
    private Client client;
    private WaypointPathfinder pathfinder;
    private WaypointerConfig config;
    private Player localPlayer;
    private DeathAutoPathfinder subject;

    @Before
    public void setUp()
    {
        client = mock(Client.class);
        pathfinder = mock(WaypointPathfinder.class);
        config = mock(WaypointerConfig.class);
        localPlayer = mock(Player.class);

        // Default "happy" wiring; individual tests override what they need.
        when(config.autoPathOnDeath()).thenReturn(true);
        when(pathfinder.isAvailable()).thenReturn(true);
        when(client.isInInstancedRegion()).thenReturn(false);
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(localPlayer.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));

        subject = new DeathAutoPathfinder(client, pathfinder, config);
    }

    @Test
    public void pathsToDeathTileOnLocalPlayerDeath()
    {
        subject.onActorDeath(new ActorDeath(localPlayer));

        int expected = WorldPointPacker.pack(3200, 3200, 0);
        verify(pathfinder).requestPath(eq(expected), eq("Death location"));
    }
}
