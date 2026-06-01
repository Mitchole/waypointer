package com.waypointer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RoutePlaybackEngineTest
{
    @Mock private WaypointPathfinder pathfinder;
    @Mock private RouteStore store;

    private RoutePlaybackEngine engine;

    private static final int BANK = WorldPointPacker.pack(3200, 3200, 0);
    private static final int PATCH = WorldPointPacker.pack(3000, 3000, 0);

    @Before
    public void setUp()
    {
        engine = new RoutePlaybackEngine(pathfinder, store);
    }

    private Route route(boolean repeating, RouteStep... steps)
    {
        return new Route(UUID.randomUUID(), "R", Arrays.asList(steps), repeating, Instant.now(), 0);
    }

    @Test
    public void startEntersFirstStepAndPathsWhenWaypoint()
    {
        Route r = route(false, RouteStep.waypoint(BANK, "Bank"), RouteStep.manual("Withdraw"));
        engine.start(r);
        assertTrue(engine.isActive());
        assertEquals(0, engine.getCurrentIndex());
        verify(pathfinder).requestPath(BANK, "Bank");
    }

    @Test
    public void manualFirstStepDoesNotPath()
    {
        Route r = route(false, RouteStep.manual("Read me"));
        engine.start(r);
        verify(pathfinder).clearPath();
    }

    @Test
    public void advanceMovesToNextStep()
    {
        Route r = route(false, RouteStep.manual("a"), RouteStep.manual("b"));
        engine.start(r);
        engine.advance();
        assertEquals(1, engine.getCurrentIndex());
        assertEquals("b", engine.getCurrentStep().getLabel());
    }

    @Test
    public void advancePastEndStopsWhenNotRepeating()
    {
        Route r = route(false, RouteStep.manual("only"));
        engine.start(r);
        engine.advance();
        assertFalse(engine.isActive());
        assertNull(engine.getCurrentStep());
    }

    @Test
    public void advancePastEndRestartsAndIncrementsLapWhenRepeating()
    {
        Route r = route(true, RouteStep.manual("a"), RouteStep.manual("b"));
        engine.start(r);
        assertEquals(1, engine.getLap());
        engine.advance();   // -> b
        engine.advance();   // wrap -> a, lap 2
        assertTrue(engine.isActive());
        assertEquals(0, engine.getCurrentIndex());
        assertEquals(2, engine.getLap());
    }

    @Test
    public void backMovesToPreviousStepClampedAtZero()
    {
        Route r = route(false, RouteStep.manual("a"), RouteStep.manual("b"));
        engine.start(r);
        engine.advance();
        engine.back();
        assertEquals(0, engine.getCurrentIndex());
        engine.back();
        assertEquals(0, engine.getCurrentIndex());
    }

    @Test
    public void arrivalWithinRadiusAdvancesWaypointStep()
    {
        Route r = route(false, RouteStep.waypoint(BANK, "Bank"), RouteStep.manual("done"));
        engine.start(r);
        engine.handleTick(new WorldPoint(3201, 3202, 0)); // within 3 tiles
        assertEquals(1, engine.getCurrentIndex());
    }

    @Test
    public void arrivalIgnoredWhenTooFarOrWrongPlane()
    {
        Route r = route(false, RouteStep.waypoint(BANK, "Bank"));
        engine.start(r);
        engine.handleTick(new WorldPoint(3260, 3260, 0)); // far
        assertEquals(0, engine.getCurrentIndex());
        engine.handleTick(new WorldPoint(3200, 3200, 1)); // right tile, wrong plane
        assertEquals(0, engine.getCurrentIndex());
    }

    @Test
    public void manualStepDoesNotAutoAdvanceOnTick()
    {
        Route r = route(false, RouteStep.manual("read"), RouteStep.manual("next"));
        engine.start(r);
        engine.handleTick(new WorldPoint(3200, 3200, 0));
        assertEquals(0, engine.getCurrentIndex());
    }

    @Test
    public void stopClearsPathAndDeactivates()
    {
        Route r = route(false, RouteStep.waypoint(BANK, "Bank"));
        engine.start(r);
        engine.stop();
        assertFalse(engine.isActive());
        verify(pathfinder).clearPath();
    }
}
