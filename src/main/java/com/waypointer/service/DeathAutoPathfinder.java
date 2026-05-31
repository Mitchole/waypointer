package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.eventbus.Subscribe;

/**
 * Starts a path back to the tile the local player died on. Off by default; enabled via the
 * "Auto-path to death location" config toggle. Detection bridges {@link ActorDeath} to the
 * Shortest Path plugin through {@link WaypointPathfinder}.
 */
@Singleton
public class DeathAutoPathfinder
{
    private static final String DEATH_DESTINATION_NAME = "Death location";

    private final Client client;
    private final WaypointPathfinder pathfinder;
    private final WaypointerConfig config;

    @Inject
    public DeathAutoPathfinder(Client client, WaypointPathfinder pathfinder, WaypointerConfig config)
    {
        this.client = client;
        this.pathfinder = pathfinder;
        this.config = config;
    }

    @Subscribe
    public void onActorDeath(ActorDeath e)
    {
        Player local = client.getLocalPlayer();
        WorldPoint loc = local.getWorldLocation();
        pathfinder.requestPath(WorldPointPacker.pack(loc), DEATH_DESTINATION_NAME);
    }
}
