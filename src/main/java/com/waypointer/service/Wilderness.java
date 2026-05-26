package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;

/**
 * Static check: is a packed tile inside the Wilderness? Bboxes are best-effort against
 * the standard OSRS map. Surface main wilderness uses the well-known Y >= 3525 boundary
 * (the ditch sits at Y=3523; Y=3520-3524 is the safe ditch zone). Underground caves use
 * plane-0 packing per OSRS map convention.
 */
public final class Wilderness
{
    private Wilderness() {}

    // Inclusive bboxes: {xMin, yMin, xMax, yMax, plane}.
    private static final int[][] BBOXES = {
        {2944, 3525, 3392, 3967, 0},   // surface main wilderness
        {2944, 9925, 3392, 10367, 0},  // Wilderness Slayer Cave
        {3072, 10112, 3327, 10239, 0}, // Revenants Cave / Forinthry Dungeon
    };

    public static boolean isInWilderness(int packed)
    {
        int x = WorldPointPacker.getX(packed);
        int y = WorldPointPacker.getY(packed);
        int plane = WorldPointPacker.getPlane(packed);
        for (int[] b : BBOXES)
        {
            if (plane == b[4] && x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) return true;
        }
        return false;
    }
}
