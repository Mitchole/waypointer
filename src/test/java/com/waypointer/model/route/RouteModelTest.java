package com.waypointer.model.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import com.waypointer.model.WorldPointPacker;
import java.time.Instant;
import java.util.ArrayList;
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
        Route a = new Route(id, "A", new ArrayList<>(), false, Instant.now(), 0);
        Route b = new Route(id, "B", new ArrayList<>(), true, Instant.now(), 9);
        assertEquals(a, b);
    }

    @Test
    public void newRouteLibraryHasCurrentSchemaAndEmptyRoutes()
    {
        RouteLibrary lib = new RouteLibrary();
        assertEquals(RouteLibrary.CURRENT_SCHEMA_VERSION, lib.getSchemaVersion());
        assertEquals(0, lib.getRoutes().size());
    }

    @Test
    public void boxTextOrLabelFallsBackToLabelWhenUnset()
    {
        RouteStep s = RouteStep.manual("Withdraw seeds");
        assertNull(s.getBoxText());
        assertEquals("Withdraw seeds", s.boxTextOrLabel());
    }

    @Test
    public void boxTextOrLabelFallsBackToLabelWhenEmpty()
    {
        RouteStep s = RouteStep.manual("Withdraw seeds");
        s.setBoxText("");
        assertEquals("Withdraw seeds", s.boxTextOrLabel());
    }

    @Test
    public void boxTextOrLabelUsesBoxTextWhenSet()
    {
        RouteStep s = RouteStep.manual("Bank");
        s.setBoxText("Withdraw 5 ranarr seeds");
        assertEquals("Withdraw 5 ranarr seeds", s.boxTextOrLabel());
    }
}
