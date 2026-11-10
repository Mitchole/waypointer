package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import com.waypointer.model.Library;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import com.waypointer.service.RouteStore;
import com.waypointer.service.WaypointStore;
import org.junit.Test;

public class RouteStepRowTest
{
    @Test
    public void waypointGlyphDiffersFromManual()
    {
        String wp = RouteStepRow.stepGlyph(StepType.WAYPOINT);
        String man = RouteStepRow.stepGlyph(StepType.MANUAL);
        assertNotEquals(wp, man);
    }

    @Test
    public void legLabelShownOnlyWhenNonNegative()
    {
        assertNull(RouteStepRow.legText(-1));
        assertEquals("0 tiles", RouteStepRow.legText(0));
        assertEquals("1 tile", RouteStepRow.legText(1));
        assertEquals("12 tiles", RouteStepRow.legText(12));
        assertEquals("1,250 tiles", RouteStepRow.legText(1250));
    }

    @Test
    public void summaryTextFormatsStopsAndDistance()
    {
        Route r = new Route(java.util.UUID.randomUUID(), "R", new java.util.ArrayList<>(), false,
            java.time.Instant.now(), 0);
        assertEquals("0 stops", RouteEditorPanel.summaryText(r));
        r.getSteps().add(RouteStep.waypoint(WorldPointPacker.pack(0, 0, 0), "a"));
        assertEquals("1 stop", RouteEditorPanel.summaryText(r));
        r.getSteps().add(RouteStep.waypoint(WorldPointPacker.pack(1500, 0, 0), "b"));
        assertEquals("2 stops, 1,500 tiles straight-line", RouteEditorPanel.summaryText(r));
    }

    @Test
    public void stepCoordinatesEditUpdatesStepOnly()
    {
        WaypointStore waypoints = new WaypointStore();
        waypoints.bootstrap(new Library());
        int original = WorldPointPacker.pack(2301, 3690, 0);
        com.waypointer.model.Waypoint source = waypoints.createWaypoint(original, "Dock",
            waypoints.getUncategorized().getId());

        RouteStore routes = new RouteStore();
        routes.bootstrap(new RouteLibrary());
        Route route = routes.createRoute("Trip");
        routes.addWaypointStep(route.getId(), original, "Dock", source.getId());
        RouteStep step = routes.getRouteById(route.getId()).getSteps().get(0);

        java.util.concurrent.atomic.AtomicInteger fires = new java.util.concurrent.atomic.AtomicInteger();
        routes.subscribe(fires::incrementAndGet);
        assertNull(RouteEditorPanel.applyStepEdit(routes, route.getId(), step, "Jetty", "Board here",
            "2400, 3500, 1"));

        // Text and tile both changed, saved as one change.
        assertEquals(1, fires.get());
        assertEquals(WorldPointPacker.pack(2400, 3500, 1), step.getPackedWorldPoint());
        assertEquals("Jetty", step.getLabel());
        assertEquals("Board here", step.getBoxText());
        assertEquals(original, waypoints.getWaypointById(source.getId()).getPackedWorldPoint());

        assertEquals("Plane must be between 0 and 3.",
            RouteEditorPanel.applyStepEdit(routes, route.getId(), step, "Jetty 2", "Board here", "1, 2, 9"));
        assertEquals(WorldPointPacker.pack(2400, 3500, 1), step.getPackedWorldPoint());
        assertEquals("Jetty 2", step.getLabel());
    }

    @Test
    public void manualStepEditKeepsItsTile()
    {
        RouteStore routes = new RouteStore();
        routes.bootstrap(new RouteLibrary());
        Route route = routes.createRoute("Trip");
        routes.addManualStep(route.getId(), "Bank");
        RouteStep step = routes.getRouteById(route.getId()).getSteps().get(0);
        int tile = step.getPackedWorldPoint();

        assertNull(RouteEditorPanel.applyStepEdit(routes, route.getId(), step, "Bank all", "", null));
        assertEquals("Bank all", step.getLabel());
        assertEquals(tile, step.getPackedWorldPoint());
    }
}
