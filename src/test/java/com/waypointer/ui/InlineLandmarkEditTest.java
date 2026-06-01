package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.LandmarkOverridesSnapshot;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointCapture;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Constructor;
import java.util.function.IntConsumer;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class InlineLandmarkEditTest
{
    @Mock private LandmarkOverrides overrides;
    @Mock private WaypointCapture capture;

    @Test
    public void recaptureAsPointUsesSingleTile() throws Exception
    {
        // Player standing at (3000, 3100, 0).
        stubCaptureAt(WorldPointPacker.pack(3000, 3100, 0));
        // Original is a point, so the panel defaults to the Point radio.
        BboxIndex.Entry original = newEntry(50, 60, 50, 60, 0, "Old Bank");
        InlineLandmarkEdit panel = new InlineLandmarkEdit(
            LandmarkType.BANK, original, overrides, capture, s -> {}, () -> {});

        click(panel, "Recapture");

        LandmarkOverridesSnapshot.Entry updated = captureReplacement();
        assertEquals(3000, updated.getX1());
        assertEquals(3100, updated.getY1());
        assertEquals(3000, updated.getX2());
        assertEquals(3100, updated.getY2());
        assertEquals(0, updated.getPlane());
    }

    @Test
    public void recaptureAsAreaExpandsToNbyN() throws Exception
    {
        stubCaptureAt(WorldPointPacker.pack(3000, 3100, 0));
        BboxIndex.Entry original = newEntry(50, 60, 50, 60, 0, "Old Bank");
        InlineLandmarkEdit panel = new InlineLandmarkEdit(
            LandmarkType.BANK, original, overrides, capture, s -> {}, () -> {});

        selectArea(panel, 5);
        click(panel, "Recapture");

        LandmarkOverridesSnapshot.Entry updated = captureReplacement();
        assertEquals(3000, updated.getX1());
        assertEquals(3100, updated.getY1());
        assertEquals(3004, updated.getX2()); // 5x5 spans x..x+4
        assertEquals(3104, updated.getY2());
    }

    @Test
    public void areaEntryDefaultsToAreaRadioPrefilledFromSize() throws Exception
    {
        stubCaptureAt(WorldPointPacker.pack(2000, 2000, 0));
        // Original is a 4x4 area -> Area radio preselected, slider seeded to 4.
        BboxIndex.Entry original = newEntry(10, 10, 13, 13, 0, "Area Bank");
        InlineLandmarkEdit panel = new InlineLandmarkEdit(
            LandmarkType.BANK, original, overrides, capture, s -> {}, () -> {});

        JRadioButton area = find(panel, JRadioButton.class, "Area");
        assertNotNull(area);
        org.junit.Assert.assertTrue("area entry should preselect the Area radio", area.isSelected());

        // Recapture without touching the slider keeps the seeded 4x4 footprint.
        click(panel, "Recapture");
        LandmarkOverridesSnapshot.Entry updated = captureReplacement();
        assertEquals(2000, updated.getX1());
        assertEquals(2003, updated.getX2());
        assertEquals(2003, updated.getY2());
    }

    private LandmarkOverridesSnapshot.Entry captureReplacement()
    {
        ArgumentCaptor<LandmarkOverridesSnapshot.Entry> captor =
            ArgumentCaptor.forClass(LandmarkOverridesSnapshot.Entry.class);
        verify(overrides).replaceEntry(eq("BANK"), any(), captor.capture());
        return captor.getValue();
    }

    private void stubCaptureAt(int packed)
    {
        doAnswer(inv -> {
            IntConsumer cb = inv.getArgument(0);
            cb.accept(packed);
            return null;
        }).when(capture).readCurrentLocation(any());
    }

    private static void selectArea(Container root, int size)
    {
        JRadioButton area = find(root, JRadioButton.class, "Area");
        JSlider slider = find(root, JSlider.class, null);
        assertNotNull(area);
        assertNotNull(slider);
        area.doClick();
        slider.setValue(size);
    }

    private static void click(Container root, String buttonText)
    {
        JButton b = find(root, JButton.class, buttonText);
        assertNotNull("expected a '" + buttonText + "' button", b);
        b.doClick();
    }

    // Finds the first component of the given type; when text != null, matches the button/label text.
    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Container c, Class<T> type, String text)
    {
        for (Component child : c.getComponents())
        {
            if (type.isInstance(child))
            {
                if (text == null) return (T) child;
                if (child instanceof JButton && text.equals(((JButton) child).getText())) return (T) child;
                if (child instanceof JRadioButton && text.equals(((JRadioButton) child).getText())) return (T) child;
            }
            if (child instanceof Container)
            {
                T found = find((Container) child, type, text);
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
