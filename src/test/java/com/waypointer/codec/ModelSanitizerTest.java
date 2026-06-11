package com.waypointer.codec;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModelSanitizerTest
{
    private static Waypoint wp(UUID id, String name, int packed)
    {
        return new Waypoint(id, name, packed, UUID.randomUUID(), null, "",
            Instant.parse("2026-06-01T00:00:00Z"), 0, false, null, false);
    }

    private static Category cat(UUID id, String name)
    {
        return new Category(id, name, 0, false, null, false);
    }

    @Test
    public void keepsValidWaypoint()
    {
        Waypoint good = wp(UUID.randomUUID(), "Bank", WorldPointPacker.pack(3200, 3200, 0));
        assertTrue(ModelSanitizer.isValidWaypoint(good));
    }

    @Test
    public void dropsWaypointWithNullIdOrName()
    {
        assertFalse(ModelSanitizer.isValidWaypoint(wp(null, "Bank", 42)));
        assertFalse(ModelSanitizer.isValidWaypoint(wp(UUID.randomUUID(), null, 42)));
    }

    @Test
    public void dropsWaypointWithUnusableCoordinate()
    {
        assertFalse("UNDEFINED coordinate must be rejected",
            ModelSanitizer.isValidWaypoint(wp(UUID.randomUUID(), "X", WorldPointPacker.UNDEFINED)));
        assertFalse("(0,0) null-island must be rejected",
            ModelSanitizer.isValidWaypoint(wp(UUID.randomUUID(), "X", 0)));
    }

    @Test
    public void sanitizeWaypointsDropsOnlyInvalid()
    {
        Waypoint good = wp(UUID.randomUUID(), "Bank", 42);
        List<Waypoint> in = Arrays.asList(good, wp(null, "NoId", 42), wp(UUID.randomUUID(), "Zero", 0));
        List<Waypoint> out = ModelSanitizer.sanitizeWaypoints(in);
        assertEquals(1, out.size());
        assertEquals("Bank", out.get(0).getName());
    }

    @Test
    public void sanitizeWaypointsHandlesNullList()
    {
        assertTrue(ModelSanitizer.sanitizeWaypoints(null).isEmpty());
    }

    @Test
    public void dropsCategoryWithNullIdOrName()
    {
        assertFalse(ModelSanitizer.isValidCategory(cat(null, "Bossing")));
        assertFalse(ModelSanitizer.isValidCategory(cat(UUID.randomUUID(), null)));
    }

    @Test
    public void keepsUncategorizedSentinelRegardlessOfFields()
    {
        Category sentinel = new Category(null, null, 0, true, null, false);
        assertTrue("uncategorized sentinel must never be dropped",
            ModelSanitizer.isValidCategory(sentinel));
    }

    @Test
    public void sanitizeCategoriesDropsOnlyInvalid()
    {
        Category good = cat(UUID.randomUUID(), "Bossing");
        List<Category> in = new ArrayList<>(Arrays.asList(good, cat(null, "Bad"), cat(UUID.randomUUID(), null)));
        List<Category> out = ModelSanitizer.sanitizeCategories(in);
        assertEquals(1, out.size());
        assertEquals("Bossing", out.get(0).getName());
    }

    @Test
    public void keepsValidStep()
    {
        RouteStep s = RouteStep.waypoint(WorldPointPacker.pack(3200, 3200, 0), "Bank");
        assertTrue(ModelSanitizer.isValidStep(s));
    }

    @Test
    public void dropsStepWithNullIdOrType()
    {
        RouteStep noId = RouteStep.manual("Go");
        noId.setId(null);
        assertFalse(ModelSanitizer.isValidStep(noId));

        RouteStep noType = RouteStep.manual("Go");
        noType.setType(null);
        assertFalse(ModelSanitizer.isValidStep(noType));
    }

    @Test
    public void dropsStepWithNoTextAtAll()
    {
        RouteStep blank = new RouteStep(UUID.randomUUID(), StepType.MANUAL, null,
            WorldPointPacker.UNDEFINED, null, null);
        assertFalse("boxTextOrLabel() would be null", ModelSanitizer.isValidStep(blank));
    }

    @Test
    public void dropsWaypointStepWithUnusableCoordinate()
    {
        RouteStep s = new RouteStep(UUID.randomUUID(), StepType.WAYPOINT, "Bank",
            WorldPointPacker.UNDEFINED, null, null);
        assertFalse(ModelSanitizer.isValidStep(s));
    }

    @Test
    public void manualStepNeedsNoCoordinate()
    {
        RouteStep s = RouteStep.manual("Withdraw seeds"); // packed = UNDEFINED by construction
        assertTrue(ModelSanitizer.isValidStep(s));
    }

    @Test
    public void dropsRouteWithNullIdOrName()
    {
        assertFalse(ModelSanitizer.isValidRoute(new Route(null, "R", new ArrayList<>(), false, null, 0)));
        assertFalse(ModelSanitizer.isValidRoute(new Route(UUID.randomUUID(), null, new ArrayList<>(), false, null, 0)));
    }

    @Test
    public void sanitizeRoutesDropsInvalidRoutesAndStripsBadSteps()
    {
        RouteStep good = RouteStep.manual("Withdraw seeds");
        RouteStep bad = new RouteStep(UUID.randomUUID(), StepType.MANUAL, null,
            WorldPointPacker.UNDEFINED, null, null);
        Route keep = new Route(UUID.randomUUID(), "Herb run",
            new ArrayList<>(Arrays.asList(good, bad)), false, null, 0);
        Route dropById = new Route(null, "Nameless route", new ArrayList<>(), false, null, 0);

        List<Route> out = ModelSanitizer.sanitizeRoutes(new ArrayList<>(Arrays.asList(keep, dropById)));

        assertEquals(1, out.size());
        assertEquals("Herb run", out.get(0).getName());
        assertEquals("bad step must be stripped from surviving route", 1, out.get(0).getSteps().size());
    }
}
