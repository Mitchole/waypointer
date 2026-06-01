package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.ui.TabHost;
import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation") // mirrors WaypointMenuHandler's suppression.
public class WaypointMenuHandlerTest
{
    private Client client;
    private net.runelite.api.Menu menu;
    private WaypointerConfig config;
    private TabHost tabHost;
    private WaypointMenuHandler handler;
    private MenuEntry sourceEntry;

    @Before
    public void setUp()
    {
        client = mock(Client.class);
        menu = mock(net.runelite.api.Menu.class);
        config = mock(WaypointerConfig.class);
        tabHost = mock(TabHost.class);
        when(client.getMenu()).thenReturn(menu);
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[0]);
        MenuEntry stubEntry = mock(MenuEntry.class);
        when(stubEntry.setOption(anyString())).thenReturn(stubEntry);
        when(stubEntry.setTarget(anyString())).thenReturn(stubEntry);
        when(stubEntry.setType(any(MenuAction.class))).thenReturn(stubEntry);
        when(stubEntry.onClick(any())).thenReturn(stubEntry);
        when(menu.createMenuEntry(anyInt())).thenReturn(stubEntry);
        sourceEntry = mock(MenuEntry.class);
        when(sourceEntry.getOption()).thenReturn("opt");
        when(sourceEntry.getTarget()).thenReturn("tgt");
        when(sourceEntry.getType()).thenReturn(MenuAction.WALK);
        when(sourceEntry.getNpc()).thenReturn(null);
        handler = new WaypointMenuHandler(client, config, tabHost);
    }

    @Test
    public void addsMenuEntryWhenCursorOverWorldMapAndShiftHeld()
    {
        Widget map = mock(Widget.class);
        when(map.getBounds()).thenReturn(new Rectangle(0, 0, 800, 600));
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(map);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(400, 300));
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu).createMenuEntry(0);
    }

    @Test
    public void doesNotAddWorldMapEntryWithoutShift()
    {
        Widget map = mock(Widget.class);
        when(map.getBounds()).thenReturn(new Rectangle(0, 0, 800, 600));
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(map);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(400, 300));
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(false);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void doesNotDuplicateWorldMapEntryWhenAlreadyPresent()
    {
        Widget map = mock(Widget.class);
        when(map.getBounds()).thenReturn(new Rectangle(0, 0, 800, 600));
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(map);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(400, 300));
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);

        MenuEntry already = mock(MenuEntry.class);
        when(already.getOption()).thenReturn("Save as Waypoint");
        when(already.getTarget()).thenReturn("<col=ff9040>Waypointer</col>");
        when(menu.getMenuEntries()).thenReturn(new MenuEntry[]{already});

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void doesNotAddEntryWhenCursorNotOverWorldMap()
    {
        Widget map = mock(Widget.class);
        when(map.getBounds()).thenReturn(new Rectangle(0, 0, 800, 600));
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(map);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(900, 100));
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void doesNotAddEntryWhenWorldMapWidgetMissing()
    {
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);
        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));
        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void tileFlowDoesNothingWhenConfigDisabled()
    {
        when(config.tileRightClickEnabled()).thenReturn(false);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void tileFlowAddsEntryWhenConfigEnabledAndShiftHeldOnWalk()
    {
        when(config.tileRightClickEnabled()).thenReturn(true);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu).createMenuEntry(1);
    }

    @Test
    public void tileFlowDoesNothingWithoutShift()
    {
        when(config.tileRightClickEnabled()).thenReturn(true);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(false);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void npcFlowAddsEntryWhenConfigEnabledAndShiftHeld()
    {
        when(config.entityRightClickEnabled()).thenReturn(true);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);
        net.runelite.api.NPC npc = mock(net.runelite.api.NPC.class);
        when(sourceEntry.getNpc()).thenReturn(npc);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu).createMenuEntry(anyInt());
    }

    @Test
    public void npcFlowAddsEntryWithoutShift()
    {
        // Holding Shift hides NPCs from selection, so the NPC entry must appear on a plain
        // right-click whenever the setting is on -- no Shift required.
        when(config.entityRightClickEnabled()).thenReturn(true);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(false);
        when(sourceEntry.getNpc()).thenReturn(mock(net.runelite.api.NPC.class));

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu).createMenuEntry(anyInt());
    }

    @Test
    public void objectFlowDoesNothingWithoutShift()
    {
        // Objects keep the Shift gate, so without Shift the object entry is not added.
        when(config.entityRightClickEnabled()).thenReturn(true);
        when(config.tileRightClickEnabled()).thenReturn(false);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(false);
        when(sourceEntry.getNpc()).thenReturn(null);
        when(sourceEntry.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void entityFlowDoesNothingWhenConfigDisabled()
    {
        when(config.entityRightClickEnabled()).thenReturn(false);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);
        when(sourceEntry.getNpc()).thenReturn(mock(net.runelite.api.NPC.class));

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void objectFlowAddsEntryWhenConfigEnabledAndShiftHeld()
    {
        when(config.entityRightClickEnabled()).thenReturn(true);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);
        when(sourceEntry.getNpc()).thenReturn(null);
        when(sourceEntry.getType()).thenReturn(MenuAction.GAME_OBJECT_FIRST_OPTION);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu).createMenuEntry(anyInt());
    }

    @Test
    public void entityFlowIgnoresPlainEntryWhenNoNpcAndNotObjectAction()
    {
        when(config.entityRightClickEnabled()).thenReturn(true);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(true);
        when(config.tileRightClickEnabled()).thenReturn(false); // so tile branch doesn't fire
        when(sourceEntry.getNpc()).thenReturn(null);
        when(sourceEntry.getType()).thenReturn(MenuAction.WALK);

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }
}
