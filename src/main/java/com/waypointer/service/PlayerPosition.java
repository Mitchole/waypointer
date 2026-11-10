package com.waypointer.service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * The player's real world tile. While aboard a boat the player lives in the boat's own world
 * view, so their own tile is an interior one; the boat's tile in the top-level view is used
 * instead. Client thread only.
 */
@Singleton
public class PlayerPosition
{
    private final Client client;

    @Inject
    public PlayerPosition(Client client)
    {
        this.client = client;
    }

    @Nullable
    public WorldPoint current()
    {
        Player player = client.getLocalPlayer();
        if (player == null) return null;
        WorldView top = client.getTopLevelWorldView();
        if (top == null) return null;
        WorldView view = player.getWorldView();
        if (view == null || view.isTopLevel()) return player.getWorldLocation();

        int id = view.getId();
        for (WorldEntity entity : top.worldEntities())
        {
            WorldView entityView = entity.getWorldView();
            if (entityView != null && entityView.getId() == id)
            {
                return toTile(top, entity.getLocalLocation());
            }
        }
        return null;
    }

    public boolean isAboard()
    {
        Player player = client.getLocalPlayer();
        if (player == null) return false;
        WorldView view = player.getWorldView();
        return view != null && !view.isTopLevel();
    }

    WorldPoint toTile(WorldView top, LocalPoint point)
    {
        return WorldPoint.fromLocal(top, point.getX(), point.getY(), top.getPlane());
    }
}
