package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointPathfinder;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
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

        Container iconRow = (Container) bar.getComponent(0);
        int buttonCount = 0;
        for (Component c : iconRow.getComponents())
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

        assertNotNull("expected toast on null hit", toasts.lastText);
        assertTrue("expected toast to mention bank, got: " + toasts.lastText,
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
        assertNotNull("expected a toast to be shown", toasts.lastText);
        assertTrue("expected toast to mention shortest path, got: " + toasts.lastText,
            toasts.lastText.toLowerCase().contains("shortest path"));
    }

    @Test
    public void setLoggedInTogglesLandmarkButtonEnabledState()
    {
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        // Bar contents: 4 landmark buttons then the customize (▾) button. ▾ is always enabled.
        List<JButton> allButtons = findLandmarkButtons(bar);

        for (JButton b : allButtons)
        {
            assertFalse("expected disabled at construction", b.isEnabled());
        }

        bar.setLoggedIn(true);
        for (JButton b : allButtons)
        {
            assertTrue("expected enabled after setLoggedIn(true)", b.isEnabled());
        }

        bar.setLoggedIn(false);
        for (JButton b : allButtons)
        {
            assertFalse("expected disabled after setLoggedIn(false)", b.isEnabled());
        }
    }

    @Test
    public void barWithEmptySelection_showsOnlyOverflowButton()
    {
        // Stub config to return a valid JSON with empty selection (all unchecked).
        com.google.gson.Gson realGson = new com.google.gson.Gson();
        LandmarkSelection empty = LandmarkSelection.canonicalDefault();
        for (LandmarkType t : LandmarkType.values())
        {
            empty = empty.withSelected(t, false);
        }
        String emptyJson = empty.toJson(realGson);
        when(config.landmarkSelectionJson()).thenReturn(emptyJson);
        // Use the real Gson here so the constructor's parse(...) reconstructs the empty selection.
        NearestLandmarkBar bar = new NearestLandmarkBar(bbox, pathfinder, client, clientThread, spriteManager, config, realGson);

        Container iconRow = (Container) bar.getComponent(0);
        int buttonCount = 0;
        for (Component c : iconRow.getComponents())
        {
            if (c instanceof JButton) buttonCount++;
        }
        assertEquals("expected only the overflow button when nothing is selected", 1, buttonCount);
    }

    // Returns only the landmark buttons in the icon row (all buttons except the trailing overflow).
    private static List<JButton> findLandmarkButtons(NearestLandmarkBar bar)
    {
        Container iconRow = (Container) bar.getComponent(0);
        List<JButton> all = new ArrayList<>();
        for (Component c : iconRow.getComponents())
        {
            if (c instanceof JButton) all.add((JButton) c);
        }
        // Drop trailing overflow button.
        if (!all.isEmpty()) all.remove(all.size() - 1);
        return all;
    }

    @Test
    public void landmarkButtonsHaveGrayFilteredDisabledIcon()
        throws java.lang.reflect.InvocationTargetException, InterruptedException,
               NoSuchFieldException, IllegalAccessException
    {
        // The spec wants disabled landmark buttons to read as obviously disabled. Swing
        // swaps to the disabled icon when isEnabled() == false; this test checks that
        // applySprite seeded one alongside the enabled icon. The test simulates sprite
        // arrival by capturing the SpriteManager.getSpriteAsync callback and feeding it
        // a small placeholder BufferedImage.
        BufferedImage stub = new BufferedImage(
            16, 16, BufferedImage.TYPE_INT_ARGB);
        ArgumentCaptor<Consumer> callback =
            ArgumentCaptor.forClass(Consumer.class);

        NearestLandmarkBar bar = new NearestLandmarkBar(
            bbox, pathfinder, client, clientThread, spriteManager, config, gson);

        verify(spriteManager, atLeastOnce()).getSpriteAsync(anyInt(), anyInt(), callback.capture());
        for (Consumer<BufferedImage> cb : callback.getAllValues())
        {
            cb.accept(stub);
        }
        // Drain the EDT so the invokeLater that sets the icons runs.
        SwingUtilities.invokeAndWait(() -> {});

        Container iconRow = (Container) bar.getComponent(0);
        // Read AbstractButton.disabledIcon via reflection so the assertion checks an
        // explicit setDisabledIcon call rather than the LAF's lazy auto-synthesis on
        // first getDisabledIcon() invocation. Without this peek the test would pass
        // even if applySprite never called setDisabledIcon.
        java.lang.reflect.Field disabledIconField =
            AbstractButton.class.getDeclaredField("disabledIcon");
        disabledIconField.setAccessible(true);

        int landmarkCount = 0;
        for (Component c : iconRow.getComponents())
        {
            if (!(c instanceof JButton)) continue;
            JButton jb = (JButton) c;
            if (jb.getIcon() == null) continue; // overflow button has no icon
            landmarkCount++;
            Object explicit = disabledIconField.get(jb);
            assertNotNull(
                "applySprite must explicitly setDisabledIcon (GrayFilter); got null",
                explicit);
        }
        assertTrue("at least one landmark button rendered", landmarkCount > 0);
    }
}
