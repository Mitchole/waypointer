package com.waypointer.model.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import com.waypointer.model.WorldPointPacker;
import java.util.UUID;
import org.junit.Test;

public class RouteModelTest
{
    @Test
    public void waypointFactorySetsTypeAndTile()
    {
        RouteStep s = RouteStep.waypoint(WorldPointPacker.pack(3200, 3200, 0), "Bank");
        assertEquals(StepType.WAYPOINT, s.getType());
        assertEquals("Bank", s.getLabel());
        assertEquals(WorldPointPacker.pack(3200, 3200, 0), s.getPackedWorldPoint());
        assertNull(s.getSourceWaypointId());
    }

    @Test
    public void manualFactorySetsTypeAndUndefinedTile()
    {
        RouteStep s = RouteStep.manual("Withdraw seeds");
        assertEquals(StepType.MANUAL, s.getType());
        assertEquals("Withdraw seeds", s.getLabel());
        assertEquals(WorldPointPacker.UNDEFINED, s.getPackedWorldPoint());
    }

    @Test
    public void stepEqualityIsByIdentity()
    {
        UUID id = UUID.randomUUID();
        RouteStep a = new RouteStep(id, StepType.MANUAL, "x", WorldPointPacker.UNDEFINED, null, null);
        RouteStep b = new RouteStep(id, StepType.MANUAL, "different", WorldPointPacker.UNDEFINED, null, null);
        RouteStep c = new RouteStep(UUID.randomUUID(), StepType.MANUAL, "x", WorldPointPacker.UNDEFINED, null, null);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    public void routeEqualityIsByIdentity()
    {
        UUID id = UUID.randomUUID();
        Route a = new Route(id, "A", new java.util.ArrayList<>(), false, java.time.Instant.now(), 0);
        Route b = new Route(id, "B", new java.util.ArrayList<>(), true, java.time.Instant.now(), 9);
        assertEquals(a, b);
    }

    @Test
    public void newRouteLibraryHasCurrentSchemaAndEmptyRoutes()
    {
        RouteLibrary lib = new RouteLibrary();
        assertEquals(RouteLibrary.CURRENT_SCHEMA_VERSION, lib.getSchemaVersion());
        assertEquals(0, lib.getRoutes().size());
    }
}
