package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.sailing.ChartPlotterBridge;
import com.waypointer.sailing.SailingBoats;
import com.waypointer.sailing.SailingJourneyOrchestrator;
import com.waypointer.sailing.WaterMap;
import com.waypointer.util.Listeners;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

/**
 * Picks the router for a path request: Shortest Path on land, Chart Plotter while aboard, or a
 * walk-to-dock journey for a sea target reached from land. Callers start, stop and read paths
 * here so only one router is ever active.
 */
@Singleton
public class Navigator
{
    public static final int ABOARD_ARRIVAL_RADIUS = 6;
    public static final int FOOT_ARRIVAL_RADIUS = 3;

    public enum Router { SHORTEST_PATH, CHART_PLOTTER, DOCK_JOURNEY }

    private final WaypointPathfinder pathfinder;
    private final SailingJourneyOrchestrator orchestrator;
    private final ChartPlotterBridge bridge;
    private final SailingBoats boats;
    private final WaterMap waterMap;
    private final WaypointerConfig config;

    @Inject
    public Navigator(WaypointPathfinder pathfinder, SailingJourneyOrchestrator orchestrator,
        ChartPlotterBridge bridge, SailingBoats boats, WaterMap waterMap, WaypointerConfig config)
    {
        this.pathfinder = pathfinder;
        this.orchestrator = orchestrator;
        this.bridge = bridge;
        this.boats = boats;
        this.waterMap = waterMap;
        this.config = config;
    }

    static Router choose(boolean sailingEnabled, boolean chartPlotterAvailable, boolean boarded, boolean targetAtSea)
    {
        if (!sailingEnabled || !chartPlotterAvailable) return Router.SHORTEST_PATH;
        if (boarded) return Router.CHART_PLOTTER;
        if (targetAtSea) return Router.DOCK_JOURNEY;
        return Router.SHORTEST_PATH;
    }

    public Router routerFor(int packed)
    {
        return route(packed, true);
    }

    /** The router any land target would get right now, for callers that pick the tile later. */
    public Router landRouter()
    {
        return route(WorldPointPacker.UNDEFINED, false);
    }

    private Router route(int packed, boolean checkSea)
    {
        // Each read is skipped once an earlier one decides, so with sailing off nothing
        // past the config is touched.
        boolean sailing = config.sailingEnabled();
        boolean plotter = sailing && bridge.isAvailable();
        boolean boarded = plotter && boats.isBoarded();
        boolean atSea = checkSea && plotter && !boarded && waterMap.isAtSea(WorldPointPacker.unpack(packed));
        return choose(sailing, plotter, boarded, atSea);
    }

    public void go(int packed, String name)
    {
        go(packed, name, false);
    }

    public void goStep(int packed, String name)
    {
        go(packed, name, true);
    }

    private void go(int packed, String name, boolean routeStep)
    {
        boolean sailing = orchestrator.activeDestination() != WorldPointPacker.UNDEFINED;
        if (routerFor(packed) == Router.SHORTEST_PATH)
        {
            if (sailing) orchestrator.cancel();
            pathfinder.requestPath(packed, name);
            return;
        }
        // The orchestrator clears its own previous leg; only a standalone path needs clearing.
        if (!sailing && pathfinder.getActiveTarget() != WorldPointPacker.UNDEFINED) pathfinder.clearPath();
        orchestrator.start(packed, name, routeStep);
    }

    public void stop()
    {
        if (orchestrator.activeDestination() != WorldPointPacker.UNDEFINED) orchestrator.cancel();
        pathfinder.clearPath();
    }

    public int activeTarget()
    {
        int sailing = orchestrator.activeDestination();
        return sailing != WorldPointPacker.UNDEFINED ? sailing : pathfinder.getActiveTarget();
    }

    @Nullable
    public String activeName()
    {
        if (orchestrator.activeDestination() != WorldPointPacker.UNDEFINED) return orchestrator.activeName();
        return pathfinder.getActiveName();
    }

    public boolean arrived(int packed, @Nullable WorldPoint pos)
    {
        if (pos == null) return false;
        if (boats.isBoarded()) return WorldPointPacker.arrivedAboard(packed, pos, ABOARD_ARRIVAL_RADIUS);
        return WorldPointPacker.arrived(packed, pos, FOOT_ARRIVAL_RADIUS);
    }

    public Listeners.Subscription subscribe(Runnable r)
    {
        Listeners.Subscription path = pathfinder.subscribe(r);
        Listeners.Subscription sail = orchestrator.subscribe(r);
        return () ->
        {
            if (path != null) path.close();
            if (sail != null) sail.close();
        };
    }
}
