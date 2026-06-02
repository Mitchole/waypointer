package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import com.waypointer.util.Listeners;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

/**
 * Runs the active route as a linear state machine. Waypoint steps auto-advance when the player
 * arrives (3-tile radius, same arithmetic as {@link WaypointPathfinder}); manual steps advance via
 * {@link #advance()} (the Next button / hotkey). Path drawing is delegated to the pathfinder.
 *
 * <p>{@code @Singleton}, so it outlives plugin lifecycle: it registers with the EventBus in the
 * plugin's startUp and unregisters in shutDown. {@code client} is left null in unit tests, which
 * drive {@link #handleTick(WorldPoint)} directly.
 */
@Slf4j
@Singleton
public class RoutePlaybackEngine
{
    private static final int ARRIVAL_RADIUS_TILES = 3;

    private final WaypointPathfinder pathfinder;
    private final RouteStore store;
    private final Listeners listeners = new Listeners();
    @Nullable private volatile Client client;
    private Listeners.Subscription storeSub;

    // Volatile for cross-thread visibility: these three fields are mutated from both the EDT
    // (playback-bar buttons -> start/advance/back/stop) and the client thread (tick auto-advance
    // via handleTick). They are not updated as an atomic group, so a reader (the playback bar)
    // can briefly observe a transitional combination during a button-vs-tick race; that is
    // visually benign and corrected on the next listeners.fire(). Fully closing the race would
    // mean confining all mutation to one thread (deferred).
    private volatile Route active;
    private volatile int currentIndex;
    private volatile int lap;

    @Inject
    public RoutePlaybackEngine(WaypointPathfinder pathfinder, RouteStore store)
    {
        this.pathfinder = pathfinder;
        this.store = store;
    }

    /** Production wiring sets the client and a store subscription; tests skip this. */
    public void attach(Client client)
    {
        this.client = client;
        if (storeSub == null)
        {
            storeSub = store.subscribe(this::onStoreChanged);
        }
    }

    public void detach()
    {
        if (storeSub != null) { storeSub.close(); storeSub = null; }
        this.client = null;
        stop();
    }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public boolean isActive() { return active != null; }
    public int getCurrentIndex() { return currentIndex; }
    public int getLap() { return lap; }
    @Nullable public Route getActiveRoute() { return active; }

    @Nullable
    public RouteStep getCurrentStep()
    {
        Route r = active;
        if (r == null || currentIndex < 0 || currentIndex >= r.getSteps().size()) return null;
        return r.getSteps().get(currentIndex);
    }

    public void start(Route route)
    {
        if (route == null || route.getSteps().isEmpty()) return;
        this.active = route;
        this.currentIndex = 0;
        this.lap = 1;
        enterCurrentStep();
        listeners.fire();
    }

    public void stop()
    {
        if (active == null) { return; }
        active = null;
        currentIndex = 0;
        lap = 1;
        pathfinder.clearPath();
        listeners.fire();
    }

    /** Advance / skip to the next step. Wraps + increments lap for repeating routes; else stops. */
    public void advance()
    {
        Route r = active;
        if (r == null) return;
        int next = currentIndex + 1;
        if (next >= r.getSteps().size())
        {
            if (r.isRepeating())
            {
                currentIndex = 0;
                lap++;
                enterCurrentStep();
                listeners.fire();
            }
            else
            {
                stop();
            }
            return;
        }
        currentIndex = next;
        enterCurrentStep();
        listeners.fire();
    }

    public void back()
    {
        Route r = active;
        if (r == null) return;
        if (currentIndex == 0) return;
        currentIndex--;
        enterCurrentStep();
        listeners.fire();
    }

    private void enterCurrentStep()
    {
        RouteStep s = getCurrentStep();
        if (s == null) return;
        if (s.getType() == StepType.WAYPOINT && s.getPackedWorldPoint() != WorldPointPacker.UNDEFINED)
        {
            pathfinder.requestPath(s.getPackedWorldPoint(), s.getLabel());
        }
        else
        {
            pathfinder.clearPath();
        }
    }

    /** Package-private test seam + production tick body. Advances a waypoint step on arrival. */
    void handleTick(WorldPoint playerLoc)
    {
        RouteStep s = getCurrentStep();
        if (s == null || s.getType() != StepType.WAYPOINT) return;
        int target = s.getPackedWorldPoint();
        if (target == WorldPointPacker.UNDEFINED || playerLoc == null) return;
        if (WorldPointPacker.arrived(target, playerLoc, ARRIVAL_RADIUS_TILES))
        {
            advance();
        }
    }

    // Package-private so tests can drive the store-deletion path without a live store subscription.
    void onStoreChanged()
    {
        Route r = active;
        if (r != null && store.getRouteById(r.getId()) == null)
        {
            stop();
        }
    }

    @Subscribe
    public void onGameTick(GameTick e)
    {
        if (active == null || client == null) return;
        if (client.getGameState() != GameState.LOGGED_IN) return;
        Player p = client.getLocalPlayer();
        if (p == null) return;
        handleTick(p.getWorldLocation());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        if (active == null) return;
        GameState s = e.getGameState();
        if (s == GameState.LOGIN_SCREEN || s == GameState.HOPPING || s == GameState.CONNECTION_LOST)
        {
            stop();
        }
    }
}
