package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.util.Listeners;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;

/** Bridge to the shortest-path plugin via PluginMessage. Detects shortest-path by
 *  listening for inbound messages in its namespace, or by probing ConfigManager.
 *  Class.forName can't see across plugin-hub classloaders. Availability is sticky
 *  once detected. */
@Slf4j
@Singleton
public class WaypointPathfinder
{
    private static final String SP_NAMESPACE = "shortestpath";
    private static final String MSG_PATH = "path";
    private static final String MSG_CLEAR = "clear";
    private static final String DATA_TARGET = "target";
    private static final int ARRIVAL_RADIUS_TILES = 3;
    // Config keys shortest-path persists early. recalculateDistance is read every game tick;
    // drawTransports is a fallback probe.
    private static final String[] CONFIG_PROBE_KEYS = { "recalculateDistance", "drawTransports" };

    private final EventBus eventBus;
    private final ClientThread clientThread;
    private final Client client;
    private final ConfigManager configManager;
    private final Listeners listeners = new Listeners();
    private volatile boolean available;
    private volatile int activeTarget = WorldPointPacker.UNDEFINED;
    private volatile String activeName = null;

    @Inject
    public WaypointPathfinder(EventBus eventBus, ClientThread clientThread, Client client,
        ConfigManager configManager)
    {
        this.eventBus = eventBus;
        this.clientThread = clientThread;
        this.client = client;
        this.configManager = configManager;
        if (probeConfig()) markAvailable("config probe at construction");
    }

    public boolean isAvailable() { return available; }

    public int getActiveTarget() { return activeTarget; }

    @Nullable
    public String getActiveName() { return activeName; }

    public void addListener(Runnable r) { listeners.add(r); }
    public void removeListener(Runnable r) { listeners.remove(r); }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        // Drop the active target on logout/hop/login screen, otherwise the banner sticks
        // until the player happens to path elsewhere.
        if (activeTarget == WorldPointPacker.UNDEFINED) return;
        GameState s = e.getGameState();
        if (s == GameState.LOGIN_SCREEN || s == GameState.HOPPING || s == GameState.CONNECTION_LOST)
        {
            activeTarget = WorldPointPacker.UNDEFINED;
            activeName = null;
            listeners.fire();
        }
    }

    @Subscribe
    public void onGameTick(GameTick e)
    {
        if (activeTarget == WorldPointPacker.UNDEFINED) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;
        Player p = client.getLocalPlayer();
        if (p == null) return;
        WorldPoint pp = p.getWorldLocation();
        if (pp == null) return;
        // Don't auto-clear across planes, or a 2nd-floor waypoint vanishes the moment the
        // player walks under the building.
        if (pp.getPlane() != WorldPointPacker.getPlane(activeTarget)) return;
        int dx = pp.getX() - WorldPointPacker.getX(activeTarget);
        int dy = pp.getY() - WorldPointPacker.getY(activeTarget);
        if (dx * dx + dy * dy <= ARRIVAL_RADIUS_TILES * ARRIVAL_RADIUS_TILES)
        {
            activeTarget = WorldPointPacker.UNDEFINED;
            activeName = null;
            listeners.fire();
        }
    }

    public void requestPath(int packedTarget, String displayName)
    {
        requestPath(packedTarget, displayName, null);
    }

    public void requestPath(int packedTarget, String displayName, Map<String, Object> configOverride)
    {
        Map<String, Object> data = new HashMap<>();
        data.put(DATA_TARGET, packedTarget);
        if (configOverride != null && !configOverride.isEmpty())
        {
            data.put("config", new HashMap<>(configOverride));
        }
        // shortest-path's handler reads client state, so post from the client thread.
        clientThread.invoke(() -> {
            eventBus.post(new PluginMessage(SP_NAMESPACE, MSG_PATH, data));
            this.activeTarget = packedTarget;
            this.activeName = displayName;
            listeners.fire();
            return true;
        });
    }

    public void clearPath()
    {
        clientThread.invoke(() -> {
            eventBus.post(new PluginMessage(SP_NAMESPACE, MSG_CLEAR, new HashMap<>()));
            this.activeTarget = WorldPointPacker.UNDEFINED;
            this.activeName = null;
            listeners.fire();
            return true;
        });
    }

    @Subscribe
    public void onPluginMessage(PluginMessage e)
    {
        if (available) return;
        if (!SP_NAMESPACE.equals(e.getNamespace())) return;
        // EventBus echoes our own posts back, so filter out the path/clear messages we just sent.
        // Only inbound messages shortest-path itself emits (e.g. transports) count as proof.
        String name = e.getName();
        if (MSG_PATH.equals(name) || MSG_CLEAR.equals(name)) return;
        markAvailable("inbound PluginMessage: " + name);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!available && SP_NAMESPACE.equals(e.getGroup()))
        {
            markAvailable("ConfigChanged in shortestpath group");
        }
    }

    private boolean probeConfig()
    {
        if (configManager == null) return false;
        for (String key : CONFIG_PROBE_KEYS)
        {
            if (configManager.getConfiguration(SP_NAMESPACE, key) != null) return true;
        }
        return false;
    }

    private void markAvailable(String reason)
    {
        available = true;
        log.info("shortest-path detected via {}", reason);
        listeners.fire();
    }
}
