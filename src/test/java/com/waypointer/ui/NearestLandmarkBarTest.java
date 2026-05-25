package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointPathfinder;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class NearestLandmarkBarTest
{
    @Mock private BboxIndex bbox;
    @Mock private WaypointPathfinder pathfinder;
    @Mock private Client client;
    @Mock private ClientThread clientThread;
    @Mock private SpriteManager spriteManager;

    @Test
    public void barHasFivePrimaryButtonsAndOverflow()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager);

        int buttonCount = 0;
        for (java.awt.Component c : bar.getComponents())
        {
            if (c instanceof JButton) buttonCount++;
        }
        // 5 primary type buttons + 1 overflow button == 6.
        assertEquals(6, buttonCount);
    }

    @Test
    public void overflowMenuContainsSixLongTailTypes()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager);

        JPopupMenu menu = bar.buildOverflowMenu();
        assertEquals(6, menu.getComponentCount());

        // Order matches OVERFLOW: Anvil, Furnace, Loom, Spinning wheel, Tanner, Charter ship.
        assertEquals("Anvil",          ((JMenuItem) menu.getComponent(0)).getText());
        assertEquals("Furnace",        ((JMenuItem) menu.getComponent(1)).getText());
        assertEquals("Loom",           ((JMenuItem) menu.getComponent(2)).getText());
        assertEquals("Spinning wheel", ((JMenuItem) menu.getComponent(3)).getText());
        assertEquals("Tanner",         ((JMenuItem) menu.getComponent(4)).getText());
        assertEquals("Charter ship",   ((JMenuItem) menu.getComponent(5)).getText());
    }
}
