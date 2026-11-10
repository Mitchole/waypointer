package com.waypointer.service;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.sailing.ChartPlotterBridge;
import com.waypointer.sailing.SailingBoats;
import com.waypointer.sailing.SailingJourneyOrchestrator;
import com.waypointer.sailing.WaterMap;
import com.waypointer.util.Listeners;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

/**
 * Picks the router for a path request: Shortest Path on land, Chart Plotter while aboard, or a
 * walk-to-dock journey for a sea target reached from land. Callers start, stop and read paths
 * here so only one router is ever active.
 *
 * <p>Whether the player is aboard is a varbit, readable only on the client thread. With sailing
 * off or Chart Plotter absent every target goes to Shortest Path, and that answer is given on
 * the calling thread without touching the client.
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
    private final ClientThread clientThread;

    @Inject
    public Navigator(WaypointPathfinder pathfinder, SailingJourneyOrchestrator orchestrator,
        ChartPlotterBridge bridge, SailingBoats boats, WaterMap waterMap, WaypointerConfig config,
        ClientThread clientThread)
    {
        this.pathfinder = pathfinder;
        this.orchestrator = orchestrator;
        this.bridge = bridge;
        this.boats = boats;
        this.waterMap = waterMap;
        this.config = config;
        this.clientThread = clientThread;
    }

    static Router choose(boolean sailingEnabled, boolean chartPlotterAvailable, boolean boarded, boolean targetAtSea)
    {
        if (!sailingEnabled || !chartPlotterAvailable) return Router.SHORTEST_PATH;
        if (boarded) return Router.CHART_PLOTTER;
        if (targetAtSea) return Router.DOCK_JOURNEY;
        return Router.SHORTEST_PATH;
    }

    /** True when every target goes to Shortest Path: sailing is off or Chart Plotter is absent.
     *  Safe on any thread. */
    public boolean shortestPathOnly()
    {
        return !config.sailingEnabled() || !bridge.isAvailable();
    }

    /** The router for a target. Client thread only. */
    public Router routerFor(int packed)
    {
        if (shortestPathOnly()) return Router.SHORTEST_PATH;
        boolean boarded = boats.isBoarded();
        boolean atSea = !boarded && waterMap.isAtSea(WorldPointPacker.unpack(packed));
        return choose(true, true, boarded, atSea);
    }

    /**
     * Works out the router for a target from the EDT. Without sailing routes the answer is
     * given at once on the calling thread; otherwise it is computed on the client thread and
     * delivered on the EDT.
     */
    public void resolveRouter(int packed, Consumer<Router> onResult)
    {
        if (shortestPathOnly())
        {
            onResult.accept(Router.SHORTEST_PATH);
            return;
        }
        clientThread.invoke(() ->
        {
            Router router = routerFor(packed);
            SwingUtilities.invokeLater(() -> onResult.accept(router));
        });
    }

    /** Starts a path with a router the caller already resolved. Safe on any thread. */
    public void go(int packed, String name, Router router)
    {
        dispatch(packed, name, router, false);
    }

    /** Starts a path to a standalone target. Safe on any thread. */
    public void go(int packed, String name)
    {
        resolveAndGo(packed, name, false);
    }

    /** Starts a path to a route step. Safe on any thread. */
    public void goStep(int packed, String name)
    {
        resolveAndGo(packed, name, true);
    }

    private void resolveAndGo(int packed, String name, boolean routeStep)
    {
        if (shortestPathOnly())
        {
            dispatch(packed, name, Router.SHORTEST_PATH, routeStep);
            return;
        }
        clientThread.invoke(() -> dispatch(packed, name, routerFor(packed), routeStep));
    }

    private void dispatch(int packed, String name, Router router, boolean routeStep)
    {
        if (router == Router.SHORTEST_PATH)
        {
            if (orchestrator.activeDestination() != WorldPointPacker.UNDEFINED) orchestrator.cancel();
            pathfinder.requestPath(packed, name);
            return;
        }
        clientThread.invoke(() ->
        {
            // The orchestrator clears its own previous leg; only a standalone path needs clearing.
            if (orchestrator.activeDestination() == WorldPointPacker.UNDEFINED
                && pathfinder.getActiveTarget() != WorldPointPacker.UNDEFINED)
            {
                pathfinder.clearPath();
            }
            orchestrator.start(packed, name, routeStep);
        });
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

    /** True when {@code pos} has reached the target: 6 tiles aboard, 3 on foot. Client thread only. */
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
