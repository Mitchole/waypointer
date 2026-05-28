package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointPathfinder;
import javax.swing.JButton;
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
    public void barHasFourDefaultButtonsPlusOverflow()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        java.awt.Container iconRow = (java.awt.Container) bar.getComponent(0);
        int buttonCount = 0;
        for (java.awt.Component c : iconRow.getComponents())
        {
            if (c instanceof JButton) buttonCount++;
        }
        // 4 default-selected type buttons + 1 overflow button.
        assertEquals(5, buttonCount);
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
    public void setLoggedInTogglesLandmarkButtonEnabledState()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        // Bar contents: 4 landmark buttons then the customize (▾) button. ▾ is always enabled.
        java.util.List<JButton> allButtons = findLandmarkButtons(bar);

        for (JButton b : allButtons)
        {
            org.junit.Assert.assertFalse("expected disabled at construction", b.isEnabled());
        }

        bar.setLoggedIn(true);
        for (JButton b : allButtons)
        {
            org.junit.Assert.assertTrue("expected enabled after setLoggedIn(true)", b.isEnabled());
        }

        bar.setLoggedIn(false);
        for (JButton b : allButtons)
        {
            org.junit.Assert.assertFalse("expected disabled after setLoggedIn(false)", b.isEnabled());
        }
    }

    // Returns only the landmark buttons in the icon row (all buttons except the trailing overflow).
    private static java.util.List<JButton> findLandmarkButtons(NearestLandmarkBar bar)
    {
        java.awt.Container iconRow = (java.awt.Container) bar.getComponent(0);
        java.util.List<JButton> all = new java.util.ArrayList<>();
        for (java.awt.Component c : iconRow.getComponents())
        {
            if (c instanceof JButton) all.add((JButton) c);
        }
        // Drop trailing overflow button.
        if (!all.isEmpty()) all.remove(all.size() - 1);
        return all;
    }
}
