package com.waypointer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

public class PlayerPositionTest
{
    private static final WorldPoint INTERIOR = new WorldPoint(3842, 6449, 0);
    private static final WorldPoint BOAT = new WorldPoint(2301, 3690, 0);

    private Client client;
    private Player player;
    private WorldView top;
    private WorldView boatView;
    private WorldEntity entity;
    private PlayerPosition position;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp()
    {
        client = mock(Client.class);
        player = mock(Player.class);
        top = mock(WorldView.class);
        boatView = mock(WorldView.class);
        entity = mock(WorldEntity.class);

        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getTopLevelWorldView()).thenReturn(top);
        when(top.isTopLevel()).thenReturn(true);
        when(boatView.isTopLevel()).thenReturn(false);
        when(boatView.getId()).thenReturn(42);
        when(player.getWorldLocation()).thenReturn(INTERIOR);

        IndexedObjectSet<WorldEntity> entities = mock(IndexedObjectSet.class);
        when(entities.iterator()).thenAnswer(i -> Collections.singletonList(entity).iterator());
        doReturn(entities).when(top).worldEntities();
        WorldView entityView = mock(WorldView.class);
        when(entityView.getId()).thenReturn(42);
        when(entity.getWorldView()).thenReturn(entityView);
        when(entity.getLocalLocation()).thenReturn(new LocalPoint(1, 1, -1));

        position = spy(new PlayerPosition(client));
        doReturn(BOAT).when(position).toTile(eq(top), any(LocalPoint.class));
    }

    @Test
    public void topLevelPlayerReturnsOwnTile()
    {
        when(player.getWorldView()).thenReturn(top);
        assertEquals(INTERIOR, position.current());
        assertFalse(position.isAboard());
    }

    @Test
    public void aboardReturnsBoatTileNotInteriorTile()
    {
        when(player.getWorldView()).thenReturn(boatView);
        assertEquals(BOAT, position.current());
        assertTrue(position.isAboard());
    }

    @Test
    public void aboardWithNoMatchingEntityReturnsNull()
    {
        when(player.getWorldView()).thenReturn(boatView);
        when(boatView.getId()).thenReturn(7);
        assertNull(position.current());
    }

    @Test
    public void noLocalPlayerReturnsNull()
    {
        when(client.getLocalPlayer()).thenReturn(null);
        assertNull(position.current());
        assertFalse(position.isAboard());
    }

    @Test
    public void noTopLevelViewReturnsNull()
    {
        when(player.getWorldView()).thenReturn(boatView);
        when(client.getTopLevelWorldView()).thenReturn(null);
        assertNull(position.current());
    }
}
