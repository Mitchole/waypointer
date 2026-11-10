package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointStore;
import com.waypointer.model.Library;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import net.runelite.client.game.SpriteManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PinnedSectionTest
{
    private final List<String> actions = new ArrayList<>();
    private Waypoint bank;
    private Waypoint altar;

    @Before
    public void setUp()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        bank = store.createWaypoint(WorldPointPacker.pack(3164, 3487, 0), "Bank",
            store.getUncategorized().getId());
        altar = store.createWaypoint(WorldPointPacker.pack(3253, 3486, 0), "Altar",
            store.getUncategorized().getId());
    }

    private PinnedSection section(int activeTarget, Waypoint editing)
    {
        List<Waypoint> pinned = new ArrayList<>();
        pinned.add(bank);
        pinned.add(altar);
        return new PinnedSection(pinned, activeTarget,
            (w, a) -> actions.add(w.getName() + ":" + a),
            w -> w == editing ? new JLabel("editor") : null,
            mock(SpriteManager.class));
    }

    private static List<PinnedSection.Tile> tiles(Container c)
    {
        List<PinnedSection.Tile> out = new ArrayList<>();
        for (Component child : c.getComponents())
        {
            if (child instanceof PinnedSection.Tile) out.add((PinnedSection.Tile) child);
            else if (child instanceof Container) out.addAll(tiles((Container) child));
        }
        return out;
    }

    @Test
    public void oneTilePerPinnedWaypoint()
    {
        List<PinnedSection.Tile> tiles = tiles(section(WorldPointPacker.UNDEFINED, null));
        assertEquals(2, tiles.size());
        assertEquals(bank.getPackedWorldPoint(), tiles.get(0).getPackedWorldPoint());
        assertEquals(altar.getPackedWorldPoint(), tiles.get(1).getPackedWorldPoint());
    }

    @Test
    public void onlyTheActiveTargetIsMarkedActive()
    {
        List<PinnedSection.Tile> tiles = tiles(section(altar.getPackedWorldPoint(), null));
        assertFalse(tiles.get(0).isActiveForTest());
        assertTrue(tiles.get(1).isActiveForTest());

        tiles.get(1).setActive(false);
        assertFalse(tiles.get(1).isActiveForTest());
    }

    @Test
    public void leftClickPathsToTheWaypoint()
    {
        PinnedSection.Tile tile = tiles(section(WorldPointPacker.UNDEFINED, null)).get(0);
        MouseEvent click = new MouseEvent(tile, MouseEvent.MOUSE_CLICKED, 0, 0, 5, 5, 1, false,
            MouseEvent.BUTTON1);
        for (MouseListener l : tile.getMouseListeners()) l.mouseClicked(click);

        assertEquals(1, actions.size());
        assertEquals("Bank:PLAY", actions.get(0));
    }

    @Test
    public void rightClickDoesNotPath()
    {
        PinnedSection.Tile tile = tiles(section(WorldPointPacker.UNDEFINED, null)).get(0);
        MouseEvent click = new MouseEvent(tile, MouseEvent.MOUSE_CLICKED, 0,
            MouseEvent.BUTTON3_DOWN_MASK, 5, 5, 1, true, MouseEvent.BUTTON3);
        for (MouseListener l : tile.getMouseListeners()) l.mouseClicked(click);

        assertTrue(actions.isEmpty());
    }

    @Test
    public void menuOffersEditUnpinDelete()
    {
        PinnedSection.Tile tile = tiles(section(WorldPointPacker.UNDEFINED, null)).get(1);
        JPopupMenu menu = tile.getComponentPopupMenu();
        for (Component c : menu.getComponents())
        {
            if (c instanceof JMenuItem) ((JMenuItem) c).doClick();
        }

        assertEquals(3, actions.size());
        assertEquals("Altar:EXPAND_PINNED", actions.get(0));
        assertEquals("Altar:TOGGLE_PIN", actions.get(1));
        assertEquals("Altar:DELETE", actions.get(2));
    }

    @Test
    public void openEditorSitsUnderTheTiles()
    {
        PinnedSection closed = section(WorldPointPacker.UNDEFINED, null);
        PinnedSection open = section(WorldPointPacker.UNDEFINED, altar);

        Component last = open.getComponent(open.getComponentCount() - 1);
        assertTrue(last instanceof JLabel);
        assertEquals("editor", ((JLabel) last).getText());
        assertSame(open, last.getParent());
        assertTrue(open.getComponentCount() > closed.getComponentCount());
    }
}
