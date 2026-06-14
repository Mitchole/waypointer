package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.waypointer.codec.RouteShareCodec;
import com.waypointer.model.Library;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.service.RouteRecorder;
import com.waypointer.service.RouteStore;
import com.waypointer.service.RouteStorePersistence;
import com.waypointer.service.WaypointStore;
import com.waypointer.util.Listeners;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.Test;

public class RoutesPanelInlineFormTest
{
    private static final class CapturingToasts implements Toasts
    {
        String lastText;
        int count;
        @Override public void show(String text) { lastText = text; count++; }
        @Override public void show(String text, String actionLabel, Runnable onClick)
        {
            lastText = text;
            count++;
        }
    }

    private static RouteStore realRouteStore()
    {
        RouteStore store = new RouteStore();
        store.bootstrap(new RouteLibrary());
        return store;
    }

    private static RoutesPanel buildPanel(RouteStore routeStore, RouteShareCodec shareCodec,
        Toasts toasts)
    {
        RoutePlaybackEngine engine = mock(RoutePlaybackEngine.class);
        when(engine.subscribe(any())).thenReturn(mock(Listeners.Subscription.class));
        RouteRecorder recorder = mock(RouteRecorder.class);
        RouteStorePersistence persistence = mock(RouteStorePersistence.class);
        when(persistence.isRefusingSaves()).thenReturn(false);
        WaypointStore waypointStore = new WaypointStore();
        waypointStore.bootstrap(new Library());
        RoutesPanel panel = new RoutesPanel(routeStore, engine, recorder, shareCodec,
            waypointStore, persistence);
        panel.setToasts(toasts);
        return panel;
    }

    @Test
    public void newRouteFormDisablesSubmitForEmptyName()
    {
        RouteStore store = realRouteStore();
        RoutesPanel panel = buildPanel(store, mock(RouteShareCodec.class), new CapturingToasts());

        panel.openNewRouteFormForTest();
        InlineInputForm form = panel.inlineFormForTest();

        assertFalse(form.isSubmitEnabled());
        form.setText("   ");
        assertFalse(form.isSubmitEnabled());
        assertEquals(0, store.getRoutesOrdered().size());
    }

    @Test
    public void newRouteFormCreatesRouteWithTypedName()
    {
        RouteStore store = realRouteStore();
        RoutesPanel panel = buildPanel(store, mock(RouteShareCodec.class), new CapturingToasts());

        panel.openNewRouteFormForTest();
        InlineInputForm form = panel.inlineFormForTest();
        form.setText("My Route");
        form.clickSubmit();

        assertEquals(1, store.getRoutesOrdered().size());
        assertEquals("My Route", store.getRoutesOrdered().get(0).getName());
    }

    @Test
    public void importFormBadCodeShowsInlineErrorAndCreatesNoRoute()
    {
        RouteStore store = realRouteStore();
        RouteShareCodec codec = mock(RouteShareCodec.class);
        when(codec.decodeRoute(anyString()))
            .thenThrow(new RouteShareCodec.MalformedCodeException("bad"));
        RoutesPanel panel = buildPanel(store, codec, new CapturingToasts());

        panel.openImportFormForTest();
        InlineInputForm form = panel.inlineFormForTest();
        form.setText("RT1:garbage");
        form.clickSubmit();

        assertTrue(form.isVisible());
        assertTrue("error mentions RT1:, got: " + form.getErrorText(),
            form.getErrorText().contains("RT1:"));
        assertEquals(0, store.getRoutesOrdered().size());
    }

    @Test
    public void importFormValidCodeCreatesRouteAndToasts()
    {
        RouteStore store = realRouteStore();
        RouteShareCodec codec = mock(RouteShareCodec.class);
        Route imported = new Route(UUID.randomUUID(), "Imported Run",
            new ArrayList<>(), false, Instant.now(), 0);
        when(codec.decodeRoute(anyString())).thenReturn(imported);
        CapturingToasts toasts = new CapturingToasts();
        RoutesPanel panel = buildPanel(store, codec, toasts);

        panel.openImportFormForTest();
        InlineInputForm form = panel.inlineFormForTest();
        form.setText("RT1:valid");
        form.clickSubmit();

        assertEquals(1, store.getRoutesOrdered().size());
        assertEquals("Imported Run", store.getRoutesOrdered().get(0).getName());
        assertEquals("Imported route: Imported Run", toasts.lastText);
        assertFalse(form.isVisible());
    }
}
