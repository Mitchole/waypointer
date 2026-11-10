package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.service.RouteStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class RouteLibraryPickerTest
{
    private static Waypoint wp(String name, UUID cat, int sortOrder)
    {
        return new Waypoint(UUID.randomUUID(), name, 100 + sortOrder, cat, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), sortOrder, false, null, false);
    }

    private static Library library(UUID catA, UUID catB)
    {
        Library lib = new Library();
        lib.getCategories().add(new Category(catB, "B", 1, false, null, false));
        lib.getCategories().add(new Category(catA, "A", 0, false, null, false));
        // Stored out of order on purpose.
        lib.getWaypoints().add(wp("b1", catB, 0));
        lib.getWaypoints().add(wp("a2", catA, 1));
        lib.getWaypoints().add(wp("a1", catA, 0));
        return lib;
    }

    private static List<String> names(List<Waypoint> list)
    {
        List<String> out = new ArrayList<>();
        for (Waypoint w : list) out.add(w.getName());
        return out;
    }

    @Test
    public void wholeCategoryAddsInStoredOrder()
    {
        UUID catA = UUID.randomUUID();
        UUID catB = UUID.randomUUID();
        WaypointPickerModel model = new WaypointPickerModel(library(catA, catB));
        model.selectNone();
        model.setCategoryChecked(catB, true);
        model.setCategoryChecked(catA, true);

        List<Waypoint> picked = model.checkedWaypointsInOrder();
        assertEquals(java.util.Arrays.asList("a1", "a2", "b1"), names(picked));

        RouteStore routes = new RouteStore();
        routes.bootstrap(new RouteLibrary());
        UUID routeId = routes.createRoute("R").getId();
        routes.addWaypointSteps(routeId, picked);
        assertEquals(3, routes.getRouteById(routeId).getSteps().size());
        assertEquals("a1", routes.getRouteById(routeId).getSteps().get(0).getLabel());
    }

    @Test
    public void nothingCheckedAddsNothing()
    {
        UUID catA = UUID.randomUUID();
        WaypointPickerModel model = new WaypointPickerModel(library(catA, UUID.randomUUID()));
        model.selectNone();
        assertTrue(model.checkedWaypointsInOrder().isEmpty());
        assertTrue(model.isEmptySelection() || model.getSelectedWaypointIds().isEmpty());

        RouteStore routes = new RouteStore();
        routes.bootstrap(new RouteLibrary());
        UUID routeId = routes.createRoute("R").getId();
        int[] fires = {0};
        routes.subscribe(() -> fires[0]++);
        routes.addWaypointSteps(routeId, model.checkedWaypointsInOrder());
        assertEquals(0, fires[0]);
        assertTrue(routes.getRouteById(routeId).getSteps().isEmpty());
    }
}
