package com.waypointer.codec;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import java.util.ArrayList;
import java.util.List;

/**
 * Drops decoded entries missing required fields so partial or hostile JSON -- pasted share
 * codes or a corrupted config blob -- cannot push null-field model objects into the panel or
 * the route overlay. Filtering: invalid entries are removed; surviving entries are returned as-is,
 * except a surviving route has its own step list replaced with the sanitized one (see
 * {@link #sanitizeRoutes}). Never throws; callers decide whether a drop becomes a whole-code
 * rejection.
 */
final class ModelSanitizer
{
    private ModelSanitizer() {}

    static List<Waypoint> sanitizeWaypoints(List<Waypoint> in)
    {
        List<Waypoint> out = new ArrayList<>();
        if (in == null) return out;
        for (Waypoint w : in)
        {
            if (isValidWaypoint(w)) out.add(w);
        }
        return out;
    }

    static List<Category> sanitizeCategories(List<Category> in)
    {
        List<Category> out = new ArrayList<>();
        if (in == null) return out;
        for (Category c : in)
        {
            if (isValidCategory(c)) out.add(c);
        }
        return out;
    }

    static boolean isValidWaypoint(Waypoint w)
    {
        return w != null
            && w.getId() != null
            && w.getName() != null
            && isCoordinateUsable(w.getPackedWorldPoint());
    }

    static boolean isValidCategory(Category c)
    {
        if (c == null) return false;
        if (c.isUncategorized()) return true; // the sentinel is never dropped, whatever its fields
        return c.getId() != null && c.getName() != null;
    }

    static List<RouteStep> sanitizeSteps(List<RouteStep> in)
    {
        List<RouteStep> out = new ArrayList<>();
        if (in == null) return out;
        for (RouteStep s : in)
        {
            if (isValidStep(s)) out.add(s);
        }
        return out;
    }

    /** Drops invalid routes and replaces each survivor's step list with its sanitized form. */
    static List<Route> sanitizeRoutes(List<Route> in)
    {
        List<Route> out = new ArrayList<>();
        if (in == null) return out;
        for (Route r : in)
        {
            if (!isValidRoute(r)) continue;
            r.setSteps(sanitizeSteps(r.getSteps()));
            out.add(r);
        }
        return out;
    }

    static boolean isValidStep(RouteStep s)
    {
        if (s == null || s.getId() == null || s.getType() == null) return false;
        if (s.boxTextOrLabel() == null) return false;
        if (s.getType() == StepType.WAYPOINT && !isCoordinateUsable(s.getPackedWorldPoint())) return false;
        return true;
    }

    static boolean isValidRoute(Route r)
    {
        return r != null && r.getId() != null && r.getName() != null;
    }

    /** Shared coordinate check: not the UNDEFINED sentinel and not the (0,0) null-island. */
    static boolean isCoordinateUsable(int packed)
    {
        if (packed == WorldPointPacker.UNDEFINED) return false;
        return !(WorldPointPacker.getX(packed) == 0 && WorldPointPacker.getY(packed) == 0);
    }
}
