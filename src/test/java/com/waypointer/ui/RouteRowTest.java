package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;

public class RouteRowTest
{
    @Test
    public void subtitleShowsStepCountAndRepeating()
    {
        Route r = new Route(UUID.randomUUID(), "R",
            Arrays.asList(RouteStep.manual("a"), RouteStep.manual("b")), true, Instant.now(), 0);
        assertEquals("2 steps  -  repeating", RouteRow.subtitle(r));
    }

    @Test
    public void subtitleSingularAndNonRepeating()
    {
        Route r = new Route(UUID.randomUUID(), "R",
            Collections.singletonList(RouteStep.manual("a")), false, Instant.now(), 0);
        assertEquals("1 step", RouteRow.subtitle(r));
    }
}
