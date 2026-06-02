package com.waypointer.service;

import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import com.waypointer.util.Listeners;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * In-memory CRUD over a {@link RouteLibrary}. Mutations notify listeners synchronously.
 * Debounced persistence is layered on via {@link #enableDebouncedPersistence}.
 */
@Slf4j
@Singleton
public class RouteStore
{
    // volatile: read off-EDT by the debounced-save supplier (the shutdown-hook flush thread).
    private volatile RouteLibrary library = new RouteLibrary();
    private final Listeners listeners = new Listeners();

    @Inject
    public RouteStore() {}

    public void bootstrap(RouteLibrary lib)
    {
        this.library = lib;
        notifyChanged();
    }

    public RouteLibrary getLibrary() { return library; }

    public Listeners.Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public List<Route> getRoutesOrdered()
    {
        List<Route> sorted = new ArrayList<>(library.getRoutes());
        sorted.sort(Comparator.comparingInt(Route::getSortOrder));
        return Collections.unmodifiableList(sorted);
    }

    public Route getRouteById(UUID id)
    {
        for (Route r : library.getRoutes()) if (r.getId().equals(id)) return r;
        return null;
    }

    public Route createRoute(String name)
    {
        int nextOrder = library.getRoutes().stream().mapToInt(Route::getSortOrder).max().orElse(-1) + 1;
        Route r = new Route(UUID.randomUUID(), name, new ArrayList<>(), false, Instant.now(), nextOrder);
        library.getRoutes().add(r);
        notifyChanged();
        return r;
    }

    public void renameRoute(UUID id, String newName)
    {
        Route r = getRouteById(id);
        if (r == null) return;
        r.setName(newName);
        notifyChanged();
    }

    public void setRepeating(UUID id, boolean repeating)
    {
        Route r = getRouteById(id);
        if (r == null) return;
        r.setRepeating(repeating);
        notifyChanged();
    }

    public void deleteRoute(UUID id)
    {
        if (library.getRoutes().removeIf(r -> r.getId().equals(id))) notifyChanged();
    }

    public Route duplicateRoute(UUID id)
    {
        Route src = getRouteById(id);
        if (src == null) return null;
        List<RouteStep> copies = new ArrayList<>();
        for (RouteStep s : src.getSteps())
        {
            RouteStep copy = new RouteStep(UUID.randomUUID(), s.getType(), s.getLabel(),
                s.getPackedWorldPoint(), s.getSourceWaypointId(), s.getIconId());
            copy.setBoxText(s.getBoxText());
            copies.add(copy);
        }
        int nextOrder = library.getRoutes().stream().mapToInt(Route::getSortOrder).max().orElse(-1) + 1;
        Route copy = new Route(UUID.randomUUID(), src.getName() + " (copy)", copies,
            src.isRepeating(), Instant.now(), nextOrder);
        library.getRoutes().add(copy);
        notifyChanged();
        return copy;
    }

    public void reorderRoutes(List<UUID> idsInNewOrder)
    {
        Map<UUID, Integer> rank = new HashMap<>();
        for (int i = 0; i < idsInNewOrder.size(); i++) rank.put(idsInNewOrder.get(i), i);
        for (Route r : library.getRoutes())
        {
            Integer rk = rank.get(r.getId());
            if (rk != null) r.setSortOrder(rk);
        }
        notifyChanged();
    }

    public void addWaypointStep(UUID routeId, int packed, String label, UUID sourceWaypointId)
    {
        Route r = getRouteById(routeId);
        if (r == null) return;
        r.getSteps().add(new RouteStep(UUID.randomUUID(),
            StepType.WAYPOINT, label, packed, sourceWaypointId, null));
        notifyChanged();
    }

    public void addManualStep(UUID routeId, String text)
    {
        Route r = getRouteById(routeId);
        if (r == null) return;
        r.getSteps().add(RouteStep.manual(text));
        notifyChanged();
    }

    public void updateStepLabel(UUID routeId, UUID stepId, String label)
    {
        RouteStep s = findStep(routeId, stepId);
        if (s == null) return;
        s.setLabel(label);
        notifyChanged();
    }

    public void updateStepWaypoint(UUID routeId, UUID stepId, int packed, String label)
    {
        RouteStep s = findStep(routeId, stepId);
        if (s == null) return;
        s.setPackedWorldPoint(packed);
        if (label != null) s.setLabel(label);
        notifyChanged();
    }

    public void updateStepText(UUID routeId, UUID stepId, String name, String boxText)
    {
        RouteStep s = findStep(routeId, stepId);
        if (s == null) return;
        s.setLabel(name);
        s.setBoxText(boxText);
        notifyChanged();
    }

    public void deleteStep(UUID routeId, UUID stepId)
    {
        Route r = getRouteById(routeId);
        if (r == null) return;
        if (r.getSteps().removeIf(s -> s.getId().equals(stepId))) notifyChanged();
    }

    public void reorderSteps(UUID routeId, List<UUID> stepIdsInOrder)
    {
        Route r = getRouteById(routeId);
        if (r == null) return;
        Map<UUID, RouteStep> byId = new HashMap<>();
        for (RouteStep s : r.getSteps()) byId.put(s.getId(), s);
        List<RouteStep> reordered = new ArrayList<>();
        for (UUID id : stepIdsInOrder)
        {
            RouteStep s = byId.remove(id);
            if (s != null) reordered.add(s);
        }
        reordered.addAll(byId.values()); // any not listed keep tail order
        r.setSteps(reordered);
        notifyChanged();
    }

    private RouteStep findStep(UUID routeId, UUID stepId)
    {
        Route r = getRouteById(routeId);
        if (r == null) return null;
        for (RouteStep s : r.getSteps()) if (s.getId().equals(stepId)) return s;
        return null;
    }

    private void notifyChanged()
    {
        listeners.fire();
    }

    // ---- Debounced persistence wiring ----

    // Must be initialized after `listeners` above -- it captures that reference at construction.
    private final PersistenceBinding<RouteLibrary> persistenceBinding = new PersistenceBinding<>(listeners);

    public void enableDebouncedPersistence(
        RouteStorePersistence p, ScheduledExecutorService exec, Duration debounceWindow)
    {
        persistenceBinding.enable(p, exec, debounceWindow, () -> library);
    }

    public void disableDebouncedPersistence()
    {
        persistenceBinding.disable();
    }

    public void flushPendingSave()
    {
        persistenceBinding.flush();
    }
}
