package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointStore;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.game.SpriteManager;
import org.junit.Before;
import org.junit.Test;

public class InlineEditPanelTest
{
    private static final class CapturingToasts implements Toasts
    {
        final List<String> messages = new ArrayList<>();
        final List<Severity> severities = new ArrayList<>();
        @Override public void show(String text) { show(text, Severity.SUCCESS); }
        @Override public void show(String text, String actionLabel, Runnable onClick)
        {
            show(text, Severity.SUCCESS);
        }
        @Override public void show(String text, Severity severity)
        {
            messages.add(text);
            severities.add(severity);
        }
    }

    private WaypointStore store;
    private CapturingToasts toasts;
    private Waypoint waypoint;
    private InlineEditPanel panel;

    @Before
    public void setUp()
    {
        store = new WaypointStore();
        store.bootstrap(new Library());
        waypoint = store.createWaypoint(WorldPointPacker.pack(2301, 3690, 0), "Spot",
            store.getUncategorized().getId());
        toasts = new CapturingToasts();
        panel = new InlineEditPanel(waypoint, store, mock(WaypointCapture.class),
            mock(SpriteManager.class), null, toasts, null, null);
    }

    @Test
    public void editingCoordinatesMovesWaypoint()
    {
        assertEquals("2301, 3690", panel.getCoordinatesText());
        panel.setCoordinatesText("2400 3500 1");
        panel.flushPending();

        Waypoint after = store.getWaypointById(waypoint.getId());
        assertNotNull(after);
        assertEquals(WorldPointPacker.pack(2400, 3500, 1), after.getPackedWorldPoint());
        assertEquals("2400, 3500, 1", panel.getCoordinatesText());
    }

    @Test
    public void invalidCoordinatesRevert()
    {
        panel.setCoordinatesText("99999, 5");
        panel.flushPending();

        assertEquals(WorldPointPacker.pack(2301, 3690, 0),
            store.getWaypointById(waypoint.getId()).getPackedWorldPoint());
        assertEquals("2301, 3690", panel.getCoordinatesText());
        assertEquals("x and y must be between 0 and 16383.", toasts.messages.get(0));
        assertEquals(Toasts.Severity.WARN, toasts.severities.get(0));
    }
}
