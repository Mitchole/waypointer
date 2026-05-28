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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NearestLandmarkBarTest
{
    @Mock private BboxIndex bbox;
    @Mock private WaypointPathfinder pathfinder;
    @Mock private Client client;
    @Mock private ClientThread clientThread;
    @Mock private SpriteManager spriteManager;
    @Mock private com.waypointer.WaypointerConfig config;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    @org.junit.Before
    public void setUpConfig()
    {
        when(config.landmarkSelectionJson()).thenReturn("");
    }

    private static final class CapturingToasts implements Toasts
    {
        String lastText;
        String lastActionLabel;
        Runnable lastOnClick;

        @Override
        public void show(String text)
        {
            this.lastText = text;
            this.lastActionLabel = null;
            this.lastOnClick = null;
        }

        @Override
        public void show(String text, String actionLabel, Runnable onClick)
        {
            this.lastText = text;
            this.lastActionLabel = actionLabel;
            this.lastOnClick = onClick;
        }
    }

    @Test
    public void barHasFivePrimaryButtonsAndOverflow()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        // Buttons live inside the icon row (first child panel), not directly on bar.
        java.awt.Container iconRow = (java.awt.Container) bar.getComponent(0);
        int buttonCount = 0;
        for (java.awt.Component c : iconRow.getComponents())
        {
            if (c instanceof JButton) buttonCount++;
        }
        // 5 primary type buttons + 1 overflow button == 6.
        assertEquals(6, buttonCount);
    }

    @Test
    public void overflowMenuContainsSixLongTailTypes()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

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

    @Test
    public void applyHitRequestsPath()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        BboxIndex.Hit hit = new BboxIndex.Hit(123456, "Edgeville Bank", 7);
        bar.applyHit(LandmarkType.BANK, hit);

        verify(pathfinder).requestPath(123456, "Edgeville Bank");
    }

    @Test
    public void applyHitWithNullHitSkipsPathRequest()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        bar.applyHit(LandmarkType.BANK, null);

        verify(pathfinder, never()).requestPath(anyInt(), anyString());
    }

    @Test
    public void applyHitWithNullHitShowsToast()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);
        CapturingToasts toasts = new CapturingToasts();
        bar.setToasts(toasts);

        bar.applyHit(LandmarkType.BANK, null);

        org.junit.Assert.assertNotNull("expected toast on null hit", toasts.lastText);
        org.junit.Assert.assertTrue("expected toast to mention bank, got: " + toasts.lastText,
            toasts.lastText.toLowerCase().contains("bank"));
    }

    @Test
    public void onPickShortcircuitsWhenShortestPathUnavailable()
    {
        when(pathfinder.isAvailable()).thenReturn(false);
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);
        CapturingToasts toasts = new CapturingToasts();
        bar.setToasts(toasts);

        bar.onPick(LandmarkType.BANK);

        verify(clientThread, never()).invoke(any(Runnable.class));
        verify(bbox, never()).nearest(any(LandmarkType.class), anyInt());
        org.junit.Assert.assertNotNull("expected a toast to be shown", toasts.lastText);
        org.junit.Assert.assertTrue("expected toast to mention shortest path, got: " + toasts.lastText,
            toasts.lastText.toLowerCase().contains("shortest path"));
    }

    @Test
    public void setLoggedInTogglesButtonEnabledState()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        // Default is disabled (constructor calls setButtonsEnabled(false)).
        for (java.awt.Component c : findAllButtons(bar))
        {
            org.junit.Assert.assertFalse("expected disabled at construction", c.isEnabled());
        }

        bar.setLoggedIn(true);
        for (java.awt.Component c : findAllButtons(bar))
        {
            org.junit.Assert.assertTrue("expected enabled after setLoggedIn(true)", c.isEnabled());
        }

        bar.setLoggedIn(false);
        for (java.awt.Component c : findAllButtons(bar))
        {
            org.junit.Assert.assertFalse("expected disabled after setLoggedIn(false)", c.isEnabled());
        }
    }

    private static java.util.List<java.awt.Component> findAllButtons(java.awt.Container root)
    {
        java.util.List<java.awt.Component> out = new java.util.ArrayList<>();
        for (java.awt.Component c : root.getComponents())
        {
            if (c instanceof JButton) out.add(c);
            else if (c instanceof java.awt.Container) out.addAll(findAllButtons((java.awt.Container) c));
        }
        return out;
    }
}
