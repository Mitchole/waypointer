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

        assertNull(RouteEditorPanel.applyStepCoordinates(routes, route.getId(), step, "2400, 3500, 1"));

        assertEquals(WorldPointPacker.pack(2400, 3500, 1), step.getPackedWorldPoint());
        assertEquals("Dock", step.getLabel());
        assertEquals(original, waypoints.getWaypointById(source.getId()).getPackedWorldPoint());

        assertEquals("Plane must be between 0 and 3.",
            RouteEditorPanel.applyStepCoordinates(routes, route.getId(), step, "1, 2, 9"));
        assertEquals(WorldPointPacker.pack(2400, 3500, 1), step.getPackedWorldPoint());
    }
}
