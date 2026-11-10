package com.waypointer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;

public class RouteStoreTest
{
    private RouteStore store;

    @Before
    public void setUp()
    {
        store = new RouteStore();
        store.bootstrap(new RouteLibrary());
    }

    @Test
    public void addWaypointStepsAppendsInOrderWithOneFire()
    {
        Route r = store.createRoute("R");
        store.addManualStep(r.getId(), "first");
        com.waypointer.model.Waypoint a = new com.waypointer.model.Waypoint(UUID.randomUUID(), "A",
            WorldPointPacker.pack(1, 1, 0), UUID.randomUUID(), null, "",
            java.time.Instant.now(), 0, false, null, false);
        com.waypointer.model.Waypoint b = new com.waypointer.model.Waypoint(UUID.randomUUID(), "B",
            WorldPointPacker.pack(2, 2, 0), UUID.randomUUID(), null, "",
            java.time.Instant.now(), 1, false, null, false);
        AtomicInteger fires = new AtomicInteger();
        store.subscribe(fires::incrementAndGet);

        store.addWaypointSteps(r.getId(), Arrays.asList(a, b));

        assertEquals(1, fires.get());
        List<RouteStep> steps = store.getRouteById(r.getId()).getSteps();
        assertEquals(3, steps.size());
        assertEquals("A", steps.get(1).getLabel());
        assertEquals(a.getId(), steps.get(1).getSourceWaypointId());
        assertEquals(WorldPointPacker.pack(2, 2, 0), steps.get(2).getPackedWorldPoint());

        store.addWaypointSteps(r.getId(), java.util.Collections.emptyList());
        store.addWaypointSteps(UUID.randomUUID(), Arrays.asList(a));
        assertEquals(1, fires.get());
    }

    @Test
    public void createRouteAppearsInOrderedList()
    {
        Route r = store.createRoute("Herb run");
        assertEquals(1, store.getRoutesOrdered().size());
        assertEquals("Herb run", store.getRoutesOrdered().get(0).getName());
        assertEquals(r.getId(), store.getRouteById(r.getId()).getId());
    }

    @Test
    public void mutationsFireListeners()
    {
        AtomicInteger fires = new AtomicInteger();
        store.subscribe(fires::incrementAndGet);
        store.createRoute("A");
        assertEquals(1, fires.get());
    }

    @Test
    public void addWaypointAndManualSteps()
    {
        Route r = store.createRoute("R");
        store.addWaypointStep(r.getId(), WorldPointPacker.pack(3200, 3200, 0), "Bank", null);
        store.addManualStep(r.getId(), "Withdraw");
        List<RouteStep> steps = store.getRouteById(r.getId()).getSteps();
        assertEquals(2, steps.size());
        assertEquals(StepType.WAYPOINT, steps.get(0).getType());
        assertEquals(StepType.MANUAL, steps.get(1).getType());
    }

    @Test
    public void updateStepLabelChangesText()
    {
        Route r = store.createRoute("R");
        store.addManualStep(r.getId(), "old");
        UUID stepId = r.getSteps().get(0).getId();
        store.updateStepLabel(r.getId(), stepId, "new");
        assertEquals("new", store.getRouteById(r.getId()).getSteps().get(0).getLabel());
    }

    @Test
    public void deleteStepRemovesIt()
    {
        Route r = store.createRoute("R");
        store.addManualStep(r.getId(), "a");
        store.addManualStep(r.getId(), "b");
        UUID first = r.getSteps().get(0).getId();
        store.deleteStep(r.getId(), first);
        List<RouteStep> steps = store.getRouteById(r.getId()).getSteps();
        assertEquals(1, steps.size());
        assertEquals("b", steps.get(0).getLabel());
    }

    @Test
    public void reorderStepsAppliesOrder()
    {
        Route r = store.createRoute("R");
        store.addManualStep(r.getId(), "a");
        store.addManualStep(r.getId(), "b");
        UUID a = r.getSteps().get(0).getId();
        UUID b = r.getSteps().get(1).getId();
        store.reorderSteps(r.getId(), Arrays.asList(b, a));
        List<RouteStep> steps = store.getRouteById(r.getId()).getSteps();
        assertEquals("b", steps.get(0).getLabel());
        assertEquals("a", steps.get(1).getLabel());
    }

    @Test
    public void setRepeatingToggles()
    {
        Route r = store.createRoute("R");
        assertFalse(r.isRepeating());
        store.setRepeating(r.getId(), true);
        assertTrue(store.getRouteById(r.getId()).isRepeating());
    }

    @Test
    public void duplicateRouteCopiesStepsWithNewIds()
    {
        Route r = store.createRoute("R");
        store.addManualStep(r.getId(), "a");
        Route copy = store.duplicateRoute(r.getId());
        assertEquals(2, store.getRoutesOrdered().size());
        assertEquals(1, copy.getSteps().size());
        assertFalse(copy.getSteps().get(0).getId().equals(r.getSteps().get(0).getId()));
        assertFalse(copy.getId().equals(r.getId()));
    }

    @Test
    public void deleteRouteRemovesIt()
    {
        Route r = store.createRoute("R");
        store.deleteRoute(r.getId());
        assertEquals(0, store.getRoutesOrdered().size());
        assertNull(store.getRouteById(r.getId()));
    }

    @Test
    public void reorderRoutesAppliesSortOrder()
    {
        Route a = store.createRoute("A");
        Route b = store.createRoute("B");
        store.reorderRoutes(Arrays.asList(b.getId(), a.getId()));
        assertEquals("B", store.getRoutesOrdered().get(0).getName());
        assertEquals("A", store.getRoutesOrdered().get(1).getName());
    }

    @Test
    public void updateStepTextSetsBothFieldsAndFiresOnce()
    {
        Route r = store.createRoute("R");
        store.addManualStep(r.getId(), "old");
        UUID stepId = r.getSteps().get(0).getId();

        AtomicInteger fires = new AtomicInteger();
        store.subscribe(fires::incrementAndGet);
        store.updateStepText(r.getId(), stepId, "Bank", "Withdraw 5 seeds");

        RouteStep s = store.getRouteById(r.getId()).getSteps().get(0);
        assertEquals("Bank", s.getLabel());
        assertEquals("Withdraw 5 seeds", s.getBoxText());
        assertEquals(1, fires.get());
    }

    @Test
    public void updateStepSetsTextAndTileAndFiresOnce()
    {
        Route r = store.createRoute("R");
        store.addWaypointStep(r.getId(), WorldPointPacker.pack(3200, 3200, 0), "old", null);
        UUID stepId = r.getSteps().get(0).getId();

        AtomicInteger fires = new AtomicInteger();
        store.subscribe(fires::incrementAndGet);
        store.updateStep(r.getId(), stepId, "Bank", "Withdraw 5 seeds", WorldPointPacker.pack(3100, 3100, 1));

        RouteStep s = store.getRouteById(r.getId()).getSteps().get(0);
        assertEquals("Bank", s.getLabel());
        assertEquals("Withdraw 5 seeds", s.getBoxText());
        assertEquals(WorldPointPacker.pack(3100, 3100, 1), s.getPackedWorldPoint());
        assertEquals(1, fires.get());
    }

    @Test
    public void duplicateRouteCopiesBoxText()
    {
        Route r = store.createRoute("R");
        store.addManualStep(r.getId(), "Bank");
        UUID stepId = r.getSteps().get(0).getId();
        store.updateStepText(r.getId(), stepId, "Bank", "Withdraw 5 seeds");

        Route copy = store.duplicateRoute(r.getId());
        assertEquals("Withdraw 5 seeds", copy.getSteps().get(0).getBoxText());
    }
}
