package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeathAutoPathfinderTest
{
    private Client client;
    private WaypointPathfinder pathfinder;
    private WaypointerConfig config;
    private Player localPlayer;
    private WorldView worldView;
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
        worldView = mock(WorldView.class);
        when(client.getTopLevelWorldView()).thenReturn(worldView);
        when(worldView.isInstance()).thenReturn(false);
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

    @Test
    public void doesNothingWhenDisabled()
    {
        when(config.autoPathOnDeath()).thenReturn(false);
        subject.onActorDeath(new ActorDeath(localPlayer));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void doesNothingWhenShortestPathUnavailable()
    {
        when(pathfinder.isAvailable()).thenReturn(false);
        subject.onActorDeath(new ActorDeath(localPlayer));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void ignoresNonLocalActorDeath()
    {
        Actor other = mock(Actor.class);
        subject.onActorDeath(new ActorDeath(other));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void doesNothingInInstancedRegion()
    {
        when(worldView.isInstance()).thenReturn(true);
        subject.onActorDeath(new ActorDeath(localPlayer));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void doesNothingWhenLocalPlayerNull()
    {
        when(client.getLocalPlayer()).thenReturn(null);
        subject.onActorDeath(new ActorDeath(localPlayer));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void doesNothingWhenWorldLocationNull()
    {
        when(localPlayer.getWorldLocation()).thenReturn(null);
        subject.onActorDeath(new ActorDeath(localPlayer));
        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }
}
