package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Outlines and labels the NPC a saved NPC-waypoint points at, but only while that waypoint is
 * the active path target. Deliberately renders nothing otherwise. Cyan is used rather than
 * brand orange, which signals active-path status elsewhere in the plugin.
 */
@Singleton
public class NpcHighlightOverlay extends Overlay
{
    private static final Color HIGHLIGHT = new Color(0, 200, 220);

    private final Client client;
    private final WaypointStore store;
    private final WaypointPathfinder pathfinder;

    @Inject
    public NpcHighlightOverlay(Client client, WaypointStore store, WaypointPathfinder pathfinder)
    {
        this.client = client;
        this.store = store;
        this.pathfinder = pathfinder;
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
    }

    /**
     * The NPC name to highlight given the active path target, or null if there is no active
     * path or its target is not an NPC waypoint. Pure; unit-tested.
     */
    @Nullable
    static String activeNpcName(Library lib, int activeTarget)
    {
        if (activeTarget == WorldPointPacker.UNDEFINED) return null;
        // Copy: render() runs on the client thread while WaypointStore is mutated from the EDT,
        // so iterating the live list directly could throw ConcurrentModificationException.
        for (Waypoint w : new java.util.ArrayList<>(lib.getWaypoints()))
        {
            if (w.getPackedWorldPoint() == activeTarget && w.getTargetNpcName() != null)
            {
                return w.getTargetNpcName();
            }
        }
        return null;
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        String name = activeNpcName(store.getLibrary(), pathfinder.getActiveTarget());
        if (name == null) return null;

        WorldView wv = client.getTopLevelWorldView();
        if (wv == null) return null;

        NPC match = null;
        for (NPC npc : wv.npcs())
        {
            if (npc != null && name.equals(npc.getName()))
            {
                match = npc;
                break;
            }
        }
        if (match == null) return null;

        OverlayUtil.renderActorOverlay(g, match, name, HIGHLIGHT);
        return null;
    }
}
