package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.waypointer.codec.RouteShareCodec;
import com.waypointer.model.Library;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.service.RouteRecorder;
import com.waypointer.service.RouteStore;
import com.waypointer.service.RouteStorePersistence;
import com.waypointer.service.WaypointStore;
import com.waypointer.util.Listeners;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;

public class RoutesPanelToastsTest
{
    private static final class CapturingToasts implements Toasts
    {
        String lastText;
        int count;

        @Override public void show(String text) { this.lastText = text; this.count++; }
        @Override public void show(String text, String actionLabel, Runnable onClick)
        {
            this.lastText = text;
            this.count++;
        }
    }

    private static RoutesPanel buildPanel(WaypointStore waypointStore)
    {
        RouteStore routeStore = mock(RouteStore.class);
        when(routeStore.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        when(routeStore.getRoutesOrdered()).thenReturn(Collections.emptyList());
        RoutePlaybackEngine engine = mock(RoutePlaybackEngine.class);
        when(engine.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        RouteRecorder recorder = mock(RouteRecorder.class);
        RouteStorePersistence persistence = mock(RouteStorePersistence.class);
        when(persistence.isRefusingSaves()).thenReturn(false);
        return new RoutesPanel(routeStore, engine, recorder,
            mock(RouteShareCodec.class), waypointStore, persistence);
    }

    @Test
    public void setToastsIsStored()
    {
        RoutesPanel panel = buildPanel(emptyStore());
        CapturingToasts toasts = new CapturingToasts();
        panel.setToasts(toasts);
        assertEquals(toasts, panel.getToastsForTest());
    }

    @Test
    public void addFromLibraryWithEmptyLibraryShowsToastNotModal()
    {
        RoutesPanel panel = buildPanel(emptyStore());
        CapturingToasts toasts = new CapturingToasts();
        panel.setToasts(toasts);

        // Empty library -> guard fires and returns before opening any modal picker.
        panel.addFromLibraryForTest(UUID.randomUUID());

        assertEquals(1, toasts.count);
        assertEquals("No saved waypoints to choose from.", toasts.lastText);
    }

    private static WaypointStore emptyStore()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        return store;
    }
}
