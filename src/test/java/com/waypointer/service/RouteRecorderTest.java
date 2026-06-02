package com.waypointer.service;

import static org.junit.Assert.assertEquals;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.model.route.StepType;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

public class RouteRecorderTest
{
    private RouteStore store;

    @Before
    public void setUp()
    {
        store = new RouteStore();
        store.bootstrap(new RouteLibrary());
    }

    // Test recorder: tile supplied directly, naming stubbed to a fixed string so we can assert
    // the label comes from the naming function (not a hardcoded "Waypoint").
    private RouteRecorder recorderAt(WorldPoint tile)
    {
        return new RouteRecorder(store, () -> tile, packed -> "Auto name");
    }

    @Test
    public void addCurrentLocationToAppendsAutoNamedWaypoint()
    {
        Route r = store.createRoute("R");
        recorderAt(new WorldPoint(3200, 3200, 0)).addCurrentLocationTo(r.getId());

        Route saved = store.getRouteById(r.getId());
        assertEquals(1, saved.getSteps().size());
        assertEquals(StepType.WAYPOINT, saved.getSteps().get(0).getType());
        assertEquals(WorldPointPacker.pack(3200, 3200, 0),
            saved.getSteps().get(0).getPackedWorldPoint());
        assertEquals("Auto name", saved.getSteps().get(0).getLabel());
    }

    @Test
    public void addCurrentLocationToIsNoOpWhenNotLoggedIn()
    {
        Route r = store.createRoute("R");
        recorderAt(null).addCurrentLocationTo(r.getId());
        assertEquals(0, store.getRouteById(r.getId()).getSteps().size());
    }

    @Test
    public void addCurrentLocationToIsNoOpForNullRoute()
    {
        Route r = store.createRoute("R");
        recorderAt(new WorldPoint(3200, 3200, 0)).addCurrentLocationTo(null);
        assertEquals(0, store.getRouteById(r.getId()).getSteps().size());
    }
}
