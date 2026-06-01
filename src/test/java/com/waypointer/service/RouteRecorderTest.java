package com.waypointer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
    private RouteRecorder recorder;
    private WorldPoint playerTile;

    @Before
    public void setUp()
    {
        store = new RouteStore();
        store.bootstrap(new RouteLibrary());
        playerTile = new WorldPoint(3200, 3200, 0);
        recorder = new RouteRecorder(store, () -> playerTile);
    }

    @Test
    public void startCreatesDraftAndIsRecording()
    {
        recorder.start("Herb run");
        assertTrue(recorder.isRecording());
    }

    @Test
    public void markCurrentLocationAppendsWaypointStep()
    {
        recorder.start("R");
        recorder.markCurrentLocation();
        Route draft = store.getRouteById(recorder.getDraftRouteIdForTest());
        assertEquals(1, draft.getSteps().size());
        assertEquals(StepType.WAYPOINT, draft.getSteps().get(0).getType());
        assertEquals(WorldPointPacker.pack(3200, 3200, 0),
            draft.getSteps().get(0).getPackedWorldPoint());
    }

    @Test
    public void addManualStepAppendsManualStep()
    {
        recorder.start("R");
        recorder.addManualStep("Withdraw seeds");
        Route draft = store.getRouteById(recorder.getDraftRouteIdForTest());
        assertEquals(StepType.MANUAL, draft.getSteps().get(0).getType());
        assertEquals("Withdraw seeds", draft.getSteps().get(0).getLabel());
    }

    @Test
    public void stopEndsRecordingAndKeepsRouteInStore()
    {
        recorder.start("R");
        recorder.markCurrentLocation();
        java.util.UUID id = recorder.getDraftRouteIdForTest();
        recorder.stopAndSave();
        assertFalse(recorder.isRecording());
        assertEquals(1, store.getRouteById(id).getSteps().size());
    }

    @Test
    public void markIgnoredWhenNotRecording()
    {
        recorder.markCurrentLocation();
        assertEquals(0, store.getRoutesOrdered().size());
    }
}
