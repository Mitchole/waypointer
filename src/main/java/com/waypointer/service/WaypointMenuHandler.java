package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.ui.TabHost;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.eventbus.Subscribe;

// Adds a "Save as Waypoint" right-click entry in three contexts:
//   1. Over the world-map widget (Shift held); converts cursor canvas position to a world tile.
//   2. On a 3D-world tile when the corresponding config is on (Shift held).
//   3. On an NPC or game object when the corresponding config is on. Objects require Shift;
//      NPCs do not, because holding Shift hides NPCs from selection.
// Mirrors the world-map coord math from shortest-path's ShortestPathPlugin#calculateMapPoint.
@Slf4j
@Singleton
@SuppressWarnings("deprecation") // ComponentID + getSelectedSceneTile still used by shortest-path; migrate together later.
public class WaypointMenuHandler
{
    private static final String OPTION = "Save as Waypoint";
    private static final String TARGET = "<col=ff9040>Waypointer</col>";

    private final Client client;
    private final WaypointerConfig config;
    private final TabHost tabHost;

    private Point lastMenuOpenedPoint;

    @Inject
    public WaypointMenuHandler(Client client, WaypointerConfig config, TabHost tabHost)
    {
        this.client = client;
        this.config = config;
        this.tabHost = tabHost;
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        // Cache the cursor position at menu-open time; by the time the user clicks "Save as
        // Waypoint" the cursor has moved. Mirrors shortest-path.
        lastMenuOpenedPoint = client.getMouseCanvasPosition();
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (tryAddWorldMapEntry()) return;
        if (tryAddTileEntry(event)) return;
        tryAddEntityEntry(event);
    }

    private boolean tryAddWorldMapEntry()
    {
        if (!client.isKeyPressed(KeyCode.KC_SHIFT)) return false;
        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (map == null) return false;
        Point mouse = client.getMouseCanvasPosition();
        if (mouse == null) return false;
        if (!map.getBounds().contains(mouse.getX(), mouse.getY())) return false;
        addEntry(0, this::onSaveFromMap);
        return true;
    }

    private boolean tryAddTileEntry(MenuEntryAdded event)
    {
        if (!config.tileRightClickEnabled()) return false;
        if (!client.isKeyPressed(KeyCode.KC_SHIFT)) return false;
        if (event.getType() != MenuAction.WALK.getId()) return false;
        addEntry(1, this::onSaveFromTile);
        return true;
    }

    private void tryAddEntityEntry(MenuEntryAdded event)
    {
        if (!config.entityRightClickEnabled()) return;

        MenuEntry source = event.getMenuEntry();
        net.runelite.api.NPC npc = source.getNpc();
        if (npc != null)
        {
            // No Shift gate for NPCs: holding Shift in RuneLite suppresses NPC hover and
            // selection, so a Shift-only entry could never be reached. The entry appears on a
            // plain right-click whenever the setting is on.
            addEntry(1, e -> onSaveFromNpc(npc));
            return;
        }
        // Objects keep the Shift gate so the entry doesn't crowd ordinary object menus.
        if (!client.isKeyPressed(KeyCode.KC_SHIFT)) return;
        if (isObjectAction(source.getType()))
        {
            int sceneX = source.getParam0();
            int sceneY = source.getParam1();
            int plane = client.getPlane();
            String name = net.runelite.client.util.Text.removeTags(source.getTarget());
            addEntry(1, e -> onSaveFromObject(sceneX, sceneY, plane, name));
        }
    }

    private static boolean isObjectAction(MenuAction type)
    {
        return type == MenuAction.GAME_OBJECT_FIRST_OPTION
            || type == MenuAction.GAME_OBJECT_SECOND_OPTION
            || type == MenuAction.GAME_OBJECT_THIRD_OPTION
            || type == MenuAction.GAME_OBJECT_FOURTH_OPTION
            || type == MenuAction.GAME_OBJECT_FIFTH_OPTION;
    }

    private void addEntry(int idx, java.util.function.Consumer<MenuEntry> onClick)
    {
        // MenuEntryAdded fires once per game-generated entry every cycle, so a menu with
        // several entries would otherwise accumulate one "Save as Waypoint" per entry.
        // Bail if ours is already present this cycle. Mirrors shortest-path's addMenuEntry.
        for (MenuEntry existing : client.getMenu().getMenuEntries())
        {
            if (OPTION.equals(existing.getOption()) && TARGET.equals(existing.getTarget()))
            {
                return;
            }
        }
        client.getMenu().createMenuEntry(idx)
            .setOption(OPTION)
            .setTarget(TARGET)
            .setType(MenuAction.RUNELITE)
            .onClick(onClick::accept);
    }

    private void onSaveFromMap(MenuEntry entry)
    {
        Point p = lastMenuOpenedPoint != null ? lastMenuOpenedPoint : client.getMouseCanvasPosition();
        if (p == null) return;
        int packed = calculateMapPoint(p.getX(), p.getY());
        if (packed == WorldPointPacker.UNDEFINED) return;
        openCapture(packed);
    }

    private void onSaveFromTile(MenuEntry entry)
    {
        Tile tile = client.getSelectedSceneTile();
        if (tile == null) return;
        LocalPoint local = tile.getLocalLocation();
        if (local == null) return;
        WorldPoint wp = WorldPoint.fromLocalInstance(client, local);
        if (wp == null) return;
        openCapture(WorldPointPacker.pack(wp));
    }

    private void onSaveFromNpc(net.runelite.api.NPC npc)
    {
        // Menu click callbacks run on the client thread, so reading the NPC location is safe.
        WorldPoint wp = npc.getWorldLocation();
        if (wp == null) return;
        String npcName = npc.getName();
        openCapture(WorldPointPacker.pack(wp), npcName, npcName);
    }

    private void onSaveFromObject(int sceneX, int sceneY, int plane, String name)
    {
        WorldPoint wp = WorldPoint.fromScene(client, sceneX, sceneY, plane);
        if (wp == null) return;
        openCapture(WorldPointPacker.pack(wp), name, null);
    }

    private void openCapture(int packed)
    {
        openCapture(packed, null, null);
    }

    private void openCapture(int packed, String defaultName, String targetNpcName)
    {
        if (packed == WorldPointPacker.UNDEFINED) return;
        SwingUtilities.invokeLater(() -> tabHost.openToCapture(packed, defaultName, targetNpcName));
    }

    // Converts canvas (x, y) over the world map widget to a packed WorldPoint.
    // Faithful copy of shortest-path's calculateMapPoint.
    int calculateMapPoint(int pointX, int pointY)
    {
        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) return WorldPointPacker.UNDEFINED;
        float zoom = worldMap.getWorldMapZoom();
        if (zoom <= 0f) return WorldPointPacker.UNDEFINED;

        Point centerWp = worldMap.getWorldMapPosition();
        if (centerWp == null) return WorldPointPacker.UNDEFINED;
        int centerPacked = WorldPointPacker.pack(centerWp.getX(), centerWp.getY(), 0);
        int middleX = mapWorldPointToGraphicsPointX(centerPacked);
        int middleY = mapWorldPointToGraphicsPointY(centerPacked);
        if (pointX == Integer.MIN_VALUE || pointY == Integer.MIN_VALUE
            || middleX == Integer.MIN_VALUE || middleY == Integer.MIN_VALUE)
        {
            return WorldPointPacker.UNDEFINED;
        }
        int dx = (int) ((pointX - middleX) / zoom);
        int dy = (int) ((-(pointY - middleY)) / zoom);
        int x = WorldPointPacker.getX(centerPacked) + dx;
        int y = WorldPointPacker.getY(centerPacked) + dy;
        if (x < 0 || y < 0) return WorldPointPacker.UNDEFINED;
        return WorldPointPacker.pack(x, y, 0);
    }

    private int mapWorldPointToGraphicsPointX(int packed)
    {
        WorldMap worldMap = client.getWorldMap();
        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (worldMap == null || map == null) return Integer.MIN_VALUE;
        float zoom = worldMap.getWorldMapZoom();
        Point center = worldMap.getWorldMapPosition();
        if (center == null) return Integer.MIN_VALUE;
        int dx = WorldPointPacker.getX(packed) - center.getX();
        return (int) (map.getBounds().x + map.getBounds().width / 2.0f + dx * zoom);
    }

    private int mapWorldPointToGraphicsPointY(int packed)
    {
        WorldMap worldMap = client.getWorldMap();
        Widget map = client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);
        if (worldMap == null || map == null) return Integer.MIN_VALUE;
        float zoom = worldMap.getWorldMapZoom();
        Point center = worldMap.getWorldMapPosition();
        if (center == null) return Integer.MIN_VALUE;
        int dy = WorldPointPacker.getY(packed) - center.getY();
        return (int) (map.getBounds().y + map.getBounds().height / 2.0f - dy * zoom);
    }
}
