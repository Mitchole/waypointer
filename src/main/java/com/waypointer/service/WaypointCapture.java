package com.waypointer.service;

import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

/**
 * Encapsulates waypoint creation paths so the panel can stay UI-only.
 *
 * Reads of the local player's location must happen on RuneLite's client thread (the assertion
 * {@code must be called on client thread} fires otherwise in dev mode), so the public capture
 * methods are async: they hop to the client thread, read the WorldPoint, then call back on the
 * EDT where Swing components and the WaypointStore can be touched safely.
 */
@Singleton
public class WaypointCapture
{
    private final Client client;
    private final WaypointStore store;
    private final ClientThread clientThread;
    private final LandmarkLookup landmarkLookup;

    @Inject
    public WaypointCapture(Client client, WaypointStore store, ClientThread clientThread,
        LandmarkLookup landmarkLookup)
    {
        this.client = client;
        this.store = store;
        this.clientThread = clientThread;
        this.landmarkLookup = landmarkLookup;
    }

    /**
     * Computes the smart default name for a packed tile: "&lt;Landmark&gt; (x, y)" if the tile
     * matches a known landmark, otherwise just "(x, y)".
     */
    public String defaultName(int packed)
    {
        String landmark = landmarkLookup.lookup(packed);
        int x = WorldPointPacker.getX(packed);
        int y = WorldPointPacker.getY(packed);
        if (landmark != null) return String.format("%s (%d, %d)", landmark, x, y);
        return String.format("(%d, %d)", x, y);
    }

    /**
     * Reads the local player's tile on the client thread, creates a waypoint in Uncategorized,
     * and invokes {@code onResult} on the EDT with the new waypoint (or null if not logged in).
     */
    public void captureCurrentLocation(Consumer<Waypoint> onResult)
    {
        clientThread.invoke(() -> {
            Integer packed = readCurrentPackedPointOnClientThread();
            SwingUtilities.invokeLater(() ->
                onResult.accept(packed == null ? null : captureFromPackedPoint(packed)));
            return true;
        });
    }

    /**
     * Reads the local player's packed tile on the client thread and invokes {@code onResult}
     * on the EDT with the packed int (or {@link WorldPointPacker#UNDEFINED} if not logged in).
     * Used by recapture flows that don't want to create a new waypoint.
     */
    public void readCurrentLocation(IntConsumer onResult)
    {
        clientThread.invoke(() -> {
            Integer packed = readCurrentPackedPointOnClientThread();
            SwingUtilities.invokeLater(() ->
                onResult.accept(packed == null ? WorldPointPacker.UNDEFINED : packed));
            return true;
        });
    }

    /** Captures from an arbitrary packed point (used by world-map and tile right-click flows). */
    public Waypoint captureFromPackedPoint(int packed)
    {
        String name = defaultName(packed);
        return store.createWaypoint(packed, name, store.getUncategorized().getId());
    }

    /**
     * Caller MUST already be on the client thread. Returns the packed local player tile, or
     * {@code null} if not logged in / player not present.
     */
    @Nullable
    Integer readCurrentPackedPointOnClientThread()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return null;
        Player p = client.getLocalPlayer();
        if (p == null) return null;
        WorldPoint wp = p.getWorldLocation();
        if (wp == null) return null;
        return WorldPointPacker.pack(wp);
    }
}
