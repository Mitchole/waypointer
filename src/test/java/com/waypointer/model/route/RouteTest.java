package com.waypointer.model.route;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RouteTest
{
    @Test
    public void constructorCopiesStepsSoCallerCannotMutateBehindTheRoute()
    {
        List<RouteStep> original = new ArrayList<>();
        original.add(RouteStep.manual("first"));
        Route route = new Route(UUID.randomUUID(), "r", original, false, null, 0);

        original.clear();

        assertEquals(1, route.getSteps().size());
    }

    @Test
    public void constructorTreatsNullStepsAsEmpty()
    {
        Route route = new Route(UUID.randomUUID(), "r", null, false, null, 0);
        assertTrue(route.getSteps().isEmpty());
    }
}
