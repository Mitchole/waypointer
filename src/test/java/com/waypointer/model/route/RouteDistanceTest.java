package com.waypointer.model.route;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import com.waypointer.model.WorldPointPacker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.Test;

public class RouteDistanceTest
{
    private static Route route(boolean repeating, RouteStep... steps)
    {
        Route r = new Route(UUID.randomUUID(), "R", new ArrayList<>(), repeating, Instant.now(), 0);
        for (RouteStep s : steps) r.getSteps().add(s);
        return r;
    }

    private static RouteStep at(int x, int y, int plane)
    {
        return RouteStep.waypoint(WorldPointPacker.pack(x, y, plane), "p");
    }

    @Test
    public void sumsChebyshevLegs()
    {
        Route r = route(false, at(0, 0, 0), at(3, 10, 0), at(13, 10, 0));
        assertEquals(20, RouteDistance.total(r));
        assertArrayEquals(new int[] {-1, 10, 10}, RouteDistance.legs(r));
        assertEquals(3, RouteDistance.stops(r));
    }

    @Test
    public void manualStepsAreSkipped()
    {
        Route r = route(false, at(0, 0, 0), RouteStep.manual("Eat"), at(7, 2, 0));
        assertArrayEquals(new int[] {-1, -1, 7}, RouteDistance.legs(r));
        assertEquals(7, RouteDistance.total(r));
        assertEquals(2, RouteDistance.stops(r));
    }

    @Test
    public void repeatingAddsClosingLeg()
    {
        Route r = route(true, at(0, 0, 0), at(10, 0, 0), at(10, 5, 0));
        assertEquals(10 + 5 + 10, RouteDistance.total(r));
    }

    @Test
    public void nothingToMeasure()
    {
        assertEquals(0, RouteDistance.total(route(false)));
        assertEquals(0, RouteDistance.total(route(true)));
        assertEquals(0, RouteDistance.legs(route(false)).length);
        assertEquals(0, RouteDistance.total(route(false, RouteStep.manual("a"))));
        assertEquals(0, RouteDistance.total(route(true, RouteStep.manual("a"))));
        assertEquals(0, RouteDistance.total(route(false, at(5, 5, 0))));
        assertEquals(0, RouteDistance.total(route(true, at(5, 5, 0))));
        assertEquals(1, RouteDistance.stops(route(true, at(5, 5, 0))));
    }

    @Test
    public void planeIsIgnored()
    {
        Route r = route(false, at(0, 0, 0), at(4, 1, 2));
        assertEquals(4, RouteDistance.total(r));
    }

    @Test
    public void undefinedTileIsNotAStop()
    {
        RouteStep bad = new RouteStep(UUID.randomUUID(), StepType.WAYPOINT, "x",
            WorldPointPacker.UNDEFINED, null, null);
        Route r = route(false, at(0, 0, 0), bad, at(2, 0, 0));
        assertArrayEquals(new int[] {-1, -1, 2}, RouteDistance.legs(r));
        assertEquals(2, RouteDistance.stops(r));
    }
}
