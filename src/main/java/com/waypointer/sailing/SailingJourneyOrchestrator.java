package com.waypointer.sailing;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.Navigator;
import com.waypointer.service.PlayerPosition;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.util.Listeners;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;

/** Runs a one-way land -> dock -> board -> sail journey to a sea target, or hands the course
 *  straight to Chart Plotter when the player is already aboard. */
@Slf4j
@Singleton
public class SailingJourneyOrchestrator
{
    private enum State { IDLE, WALKING_TO_DOCK, SAILING }

    static final String HANDOVER_NOTICE =
        "Course sent to Chart Plotter. If nothing draws, turn on its charted line.";

    private final SailingBoats boats;
    private final ChartPlotterBridge bridge;
    private final SailingBoatPin pin;
    private final WaypointPathfinder pathfinder;
    private final Client client;
    private final PlayerPosition playerPosition;
    private final Listeners listeners = new Listeners();

    private volatile Feedback feedback = (m, w) -> { };
    private volatile State state = State.IDLE;
    private volatile int destination = WorldPointPacker.UNDEFINED;
    private volatile String destinationName;
    private volatile boolean routeStep;
    private volatile boolean handoverNoticeShown;

    @Inject
    public SailingJourneyOrchestrator(SailingBoats boats, ChartPlotterBridge bridge, SailingBoatPin pin,
        WaypointPathfinder pathfinder, Client client, PlayerPosition playerPosition)
    {
        this.boats = boats;
        this.bridge = bridge;
        this.pin = pin;
        this.pathfinder = pathfinder;
        this.client = client;
        this.playerPosition = playerPosition;
    }

    public void setFeedback(Feedback f) { this.feedback = f == null ? (m, w) -> { } : f; }

    /** Decoupled user-feedback sink so this service never depends on Swing/ui. */
    public interface Feedback { void show(String message, boolean warning); }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public int activeDestination() { return state == State.IDLE ? WorldPointPacker.UNDEFINED : destination; }

    @Nullable
    public String activeName() { return state == State.IDLE ? null : destinationName; }

    /** Forgets that the Chart Plotter notice was shown, so the next plugin start shows it again. */
    public void resetSession() { handoverNoticeShown = false; }

    public void start(int packed, String name, boolean routeStep)
    {
        if (!bridge.isAvailable())
        {
            feedback.show("Install Chart Plotter to sail to ocean waypoints.", true);
            return;
        }
        // Drop any prior journey, clearing its in-flight leg, without firing yet.
        if (state == State.WALKING_TO_DOCK) pathfinder.clearPath();
        else if (state == State.SAILING) bridge.clear();
        reset(false);
        destination = packed;
        destinationName = name;
        this.routeStep = routeStep;

        if (boats.isBoarded())
        {
            handoff();
            return;
        }

        SailingBoats.BoatInfo chosen;
        if (pin.isEnabled())
        {
            chosen = boats.boat(pin.slot());
            if (chosen == null)
            {
                feedback.show("Your pinned boat isn't docked at a port right now.", true);
                reset(true);
                return;
            }
            String pinned = pin.name();
            if (pinned != null && !pinned.isEmpty() && !pinned.equals(chosen.name))
            {
                feedback.show("Your pinned boat changed - re-pick it in the panel.", true);
                reset(true);
                return;
            }
        }
        else
        {
            List<SailingBoats.BoatInfo> moored = boats.mooredBoats();
            if (moored.isEmpty())
            {
                feedback.show("No boat is docked at a port - dock a boat first.", true);
                reset(true);
                return;
            }
            chosen = nearestTo(moored, destination);
        }

        WorldPoint dockTile = chosen.dock.getNavigationLocation();
        pathfinder.requestPath(WorldPointPacker.pack(dockTile), "Sail via " + chosen.dock.getDockName());
        state = State.WALKING_TO_DOCK;
        listeners.fire();
    }

    public void cancel()
    {
        if (state == State.WALKING_TO_DOCK) pathfinder.clearPath();
        else if (state == State.SAILING) bridge.clear();
        reset(true);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e)
    {
        if (e.getVarbitId() != VarbitID.SAILING_BOARDED_BOAT) return;
        boolean boarded = boats.isBoarded();
        if (state == State.WALKING_TO_DOCK && boarded)
        {
            pathfinder.clearPath();
            feedback.show("Setting sail to " + destinationName + ".", false);
            handoff();
        }
        else if (state == State.SAILING && !boarded)
        {
            boolean silent = routeStep;
            reset(true);
            if (!silent) feedback.show("Sailing cancelled - you left the boat.", true);
        }
    }

    @Subscribe
    public void onGameTick(GameTick e)
    {
        if (state != State.SAILING) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;
        WorldPoint loc = playerPosition.current();
        if (loc == null) return;
        if (WorldPointPacker.arrivedAboard(destination, loc, Navigator.ABOARD_ARRIVAL_RADIUS))
        {
            reset(true);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        if (state == State.IDLE) return;
        GameState s = e.getGameState();
        if (s == GameState.LOGIN_SCREEN || s == GameState.HOPPING || s == GameState.CONNECTION_LOST)
        {
            reset(true);
        }
    }

    private void handoff()
    {
        bridge.chart(WorldPointPacker.getX(destination), WorldPointPacker.getY(destination));
        state = State.SAILING;
        listeners.fire();
        if (!handoverNoticeShown)
        {
            handoverNoticeShown = true;
            feedback.show(HANDOVER_NOTICE, false);
        }
    }

    private void reset(boolean fire)
    {
        state = State.IDLE;
        destination = WorldPointPacker.UNDEFINED;
        destinationName = null;
        routeStep = false;
        if (fire) listeners.fire();
    }

    private static SailingBoats.BoatInfo nearestTo(List<SailingBoats.BoatInfo> moored, int packedTarget)
    {
        WorldPoint dst = WorldPointPacker.unpack(packedTarget);
        SailingBoats.BoatInfo best = null;
        long bestD = Long.MAX_VALUE;
        for (SailingBoats.BoatInfo b : moored)
        {
            WorldPoint n = b.dock.getNavigationLocation();
            long dx = n.getX() - dst.getX();
            long dy = n.getY() - dst.getY();
            long d = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = b; }
        }
        return best;
    }
}
