package com.waypointer.codec;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import java.util.ArrayList;
import java.util.List;

/**
 * Drops decoded entries missing required fields so partial or hostile JSON -- pasted share
 * codes or a corrupted config blob -- cannot push null-field model objects into the panel or
 * the route overlay. Pure filtering: surviving entries are returned unchanged, invalid ones are
 * removed. Never throws; callers decide whether a drop becomes a whole-code rejection.
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

    /** Shared coordinate check: not the UNDEFINED sentinel and not the (0,0) null-island. */
    static boolean isCoordinateUsable(int packed)
    {
        if (packed == WorldPointPacker.UNDEFINED) return false;
        return !(WorldPointPacker.getX(packed) == 0 && WorldPointPacker.getY(packed) == 0);
    }
}
