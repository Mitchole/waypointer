package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.client.eventbus.Subscribe;

/**
 * Starts a path back to the tile the local player died on. Off by default; enabled via the
 * "Auto-path to death location" config toggle. Detection bridges {@link ActorDeath} to
 * whichever router {@link Navigator} picks for the tile.
 */
@Singleton
public class DeathAutoPathfinder
{
    private static final String DEATH_DESTINATION_NAME = "Death location";

    private final Client client;
    private final WaypointPathfinder pathfinder;
    private final Navigator navigator;
    private final WaypointerConfig config;
    private final PlayerPosition playerPosition;

    @Inject
    public DeathAutoPathfinder(Client client, WaypointPathfinder pathfinder, Navigator navigator,
        WaypointerConfig config, PlayerPosition playerPosition)
    {
        this.playerPosition = playerPosition;
        this.client = client;
        this.pathfinder = pathfinder;
        this.navigator = navigator;
        this.config = config;
    }

    @Subscribe
    public void onActorDeath(ActorDeath e)
    {
        if (!config.autoPathOnDeath()) return;

        Player local = client.getLocalPlayer();
        // Only the local player's death matters; ActorDeath also fires for NPCs and other players.
        if (local == null || e.getActor() != local) return;

        // Instance coords don't map to real-world tiles, and instances have no death-tile
        // gravestone (items go to Death's office / the instance exit), so pathing would target
        // a meaningless tile.
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView != null && worldView.isInstance()) return;

        WorldPoint deathLocation = playerPosition.current();
        if (deathLocation == null) return;

        // Event handlers run on the client thread, so the router can be read directly.
        int target = WorldPointPacker.pack(deathLocation);
        Navigator.Router router = navigator.routerFor(target);
        if (router == Navigator.Router.SHORTEST_PATH && !pathfinder.isAvailable()) return;

        // This bypasses the wilderness-confirm gate (that lives in the panel's row handler),
        // which is the intended "auto-path anyway, no dialog" behavior on death.
        navigator.go(target, DEATH_DESTINATION_NAME, router);
    }
}
