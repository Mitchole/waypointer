package com.waypointer.util;

import com.waypointer.model.WorldPointPacker;
import net.runelite.api.coords.WorldPoint;

/** Distance and compass direction from a tile to a packed target, for display. */
public final class Bearing
{
    private static final String[] POINTS = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};

    private Bearing() {}

    /** Chebyshev distance and eight-point compass, e.g. "14 tiles NE". North is +y. */
    public static String describe(WorldPoint from, int packedTarget)
    {
        WorldPoint to = WorldPointPacker.unpack(packedTarget);
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dist = Math.max(Math.abs(dx), Math.abs(dy));
        if (dist == 0) return "here";
        double degrees = Math.toDegrees(Math.atan2(dy, dx));
        int sector = (int) Math.floorMod(Math.round(degrees / 45.0), 8L);
        return dist + (dist == 1 ? " tile " : " tiles ") + POINTS[sector];
    }
}
