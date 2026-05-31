package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.ui.WaypointerPanel;
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
    private WaypointStore store;
    private WaypointCapture capture;
    private WaypointerConfig config;
    private WaypointerPanel panel;
    private WaypointMenuHandler handler;
    private MenuEntry sourceEntry;

    @Before
    public void setUp()
    {
        client = mock(Client.class);
        menu = mock(net.runelite.api.Menu.class);
        store = mock(WaypointStore.class);
        capture = mock(WaypointCapture.class);
        config = mock(WaypointerConfig.class);
        panel = mock(WaypointerPanel.class);
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
        handler = new WaypointMenuHandler(client, store, capture, config, panel);
    }

    @Test
    public void addsMenuEntryWhenCursorOverWorldMap()
    {
        Widget map = mock(Widget.class);
        when(map.getBounds()).thenReturn(new Rectangle(0, 0, 800, 600));
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(map);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(400, 300));

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu).createMenuEntry(0);
    }

    @Test
    public void doesNotDuplicateWorldMapEntryWhenAlreadyPresent()
    {
        Widget map = mock(Widget.class);
        when(map.getBounds()).thenReturn(new Rectangle(0, 0, 800, 600));
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(map);
        when(client.getMouseCanvasPosition()).thenReturn(new Point(400, 300));

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

        handler.onMenuEntryAdded(new MenuEntryAdded(sourceEntry));

        verify(menu, never()).createMenuEntry(anyInt());
    }

    @Test
    public void doesNotAddEntryWhenWorldMapWidgetMissing()
    {
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
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
    public void npcFlowDoesNothingWithoutShift()
    {
        when(config.entityRightClickEnabled()).thenReturn(true);
        when(client.getWidget(ComponentID.WORLD_MAP_MAPVIEW)).thenReturn(null);
        when(client.isKeyPressed(net.runelite.api.KeyCode.KC_SHIFT)).thenReturn(false);
        when(sourceEntry.getNpc()).thenReturn(mock(net.runelite.api.NPC.class));

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
