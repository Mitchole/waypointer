package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class RouteOverlayTest
{
    private Route route(boolean repeating, RouteStep... steps)
    {
        return new Route(UUID.randomUUID(), "Herb run", Arrays.asList(steps), repeating, Instant.now(), 0);
    }

    @Test
    public void linesShowStepCounterAndCurrentText()
    {
        Route r = route(false,
            RouteStep.waypoint(WorldPointPacker.pack(1, 1, 0), "Bank"),
            RouteStep.manual("Withdraw seeds"),
            RouteStep.manual("Plant"));
        List<String> lines = RouteOverlay.buildLines(r, 1, 1);
        assertEquals("Step 2 / 3", lines.get(0));
        assertTrue(lines.contains("Withdraw seeds"));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("Next:")));
    }

    @Test
    public void repeatingShowsLapInCounter()
    {
        Route r = route(true, RouteStep.manual("a"), RouteStep.manual("b"));
        List<String> lines = RouteOverlay.buildLines(r, 0, 3);
        assertEquals("Step 1 / 2  -  Lap 3", lines.get(0));
    }

    @Test
    public void lastStepHasNoNextLine()
    {
        Route r = route(false, RouteStep.manual("only"));
        List<String> lines = RouteOverlay.buildLines(r, 0, 1);
        assertTrue(lines.stream().noneMatch(l -> l.startsWith("Next:")));
    }

    @Test
    public void boxTextOverridesLabelForCurrentAndNextLines()
    {
        RouteStep s0 = RouteStep.manual("Bank");
        s0.setBoxText("Withdraw 5 seeds");
        RouteStep s1 = RouteStep.manual("Plant");
        s1.setBoxText("Plant in patch 3");
        Route r = route(false, s0, s1);

        List<String> lines = RouteOverlay.buildLines(r, 0, 1);

        assertTrue(lines.contains("Withdraw 5 seeds"));
        assertFalse(lines.contains("Bank"));
        assertTrue(lines.contains("Next: Plant in patch 3"));
    }

    @Test
    public void fallsBackToLabelWhenNoBoxText()
    {
        Route r = route(false, RouteStep.manual("Withdraw seeds"));
        List<String> lines = RouteOverlay.buildLines(r, 0, 1);
        assertTrue(lines.contains("Withdraw seeds"));
    }
}
