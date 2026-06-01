package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.PresetOverrides;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointPathfinder;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Constructor;
import java.util.Collections;
import javax.swing.JButton;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DevToolsNavigationTest
{
    @Mock private BboxIndex bboxIndex;
    @Mock private LandmarkOverrides landmarkOverrides;
    @Mock private PresetCatalog catalog;
    @Mock private PresetOverrides presetOverrides;
    @Mock private WaypointCapture capture;
    @Mock private WaypointPathfinder pathfinder;
    @Mock private AreaPreviewOverlay areaOverlay;
    @Mock private com.waypointer.codec.LandmarkOverridesCodec landmarkCodec;
    @Mock private com.waypointer.codec.PresetOverridesCodec presetCodec;

    @Test
    public void presetWaypointRowGoButtonFiresOnNavigate()
    {
        PresetWaypoint wp = new PresetWaypoint("Edgeville", "", 3093, 3496, 0);
        PresetWaypoint[] navigated = new PresetWaypoint[1];
        PresetWaypointRow row = new PresetWaypointRow("Banks", wp,
            w -> navigated[0] = w, w -> {}, w -> {});

        JButton go = findButton(row, "Go");
        assertNotNull("row should expose a Go button", go);
        go.doClick();

        assertSame(wp, navigated[0]);
    }

    @Test
    public void landmarkRowGoButtonFiresOnNavigate() throws Exception
    {
        BboxIndex.Entry entry = newEntry(3200, 3200, 3200, 3200, 0, "Varrock West Bank");
        BboxIndex.Entry[] navigated = new BboxIndex.Entry[1];
        LandmarkRow row = new LandmarkRow(LandmarkType.BANK, entry,
            e -> navigated[0] = e, e -> {}, e -> {});

        JButton go = findButton(row, "Go");
        assertNotNull("row should expose a Go button", go);
        go.doClick();

        assertSame(entry, navigated[0]);
    }

    @Test
    public void presetEditorNavigateRequestsPathAtWaypointTile()
    {
        when(catalog.getPresets()).thenReturn(Collections.emptyList());
        when(pathfinder.isAvailable()).thenReturn(true);
        PresetEditorPanel panel = new PresetEditorPanel(catalog, presetOverrides, capture, pathfinder, presetCodec);

        panel.navigateTo(new PresetWaypoint("Lumbridge", "", 3222, 3218, 0));

        verify(pathfinder).requestPath(WorldPointPacker.pack(3222, 3218, 0), "Lumbridge");
    }

    // The Shortest-Path-unavailable branch shows a modal JOptionPane, which would block the
    // (non-headless) test JVM, so it is exercised manually rather than in an automated test.

    @Test
    public void landmarkEditorNavigateRequestsPathAtEntryCentre() throws Exception
    {
        when(bboxIndex.editableOfType(any(), any())).thenReturn(Collections.emptyList());
        when(pathfinder.isAvailable()).thenReturn(true);
        LandmarkEditorPanel panel = new LandmarkEditorPanel(
            bboxIndex, landmarkOverrides, capture, pathfinder, areaOverlay, landmarkCodec);

        // Area entry: navigation targets the bbox centre, not a corner.
        panel.navigateTo(newEntry(3010, 3355, 3016, 3361, 1, "Edgeville Bank"));

        verify(pathfinder).requestPath(WorldPointPacker.pack(3013, 3358, 1), "Edgeville Bank");
    }

    @Test
    public void devToolsWindowDisposeWithoutOpenDisposesEditors()
    {
        // dispose() before any open() must still tear down the editor subscriptions and not
        // touch a (never-built) frame.
        LandmarkEditorPanel landmarkEditor = org.mockito.Mockito.mock(LandmarkEditorPanel.class);
        PresetEditorPanel presetEditor = org.mockito.Mockito.mock(PresetEditorPanel.class);
        DevToolsWindow window = new DevToolsWindow(landmarkEditor, presetEditor);

        window.dispose();

        verify(landmarkEditor).dispose();
        verify(presetEditor).dispose();
    }

    private static JButton findButton(Container c, String text)
    {
        for (Component child : c.getComponents())
        {
            if (child instanceof JButton && text.equals(((JButton) child).getText()))
            {
                return (JButton) child;
            }
            if (child instanceof Container)
            {
                JButton found = findButton((Container) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static BboxIndex.Entry newEntry(int x1, int y1, int x2, int y2, int plane, String name)
        throws Exception
    {
        Constructor<BboxIndex.Entry> ctor = BboxIndex.Entry.class.getDeclaredConstructor(
            int.class, int.class, int.class, int.class, int.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(x1, y1, x2, y2, plane, name);
    }
}
