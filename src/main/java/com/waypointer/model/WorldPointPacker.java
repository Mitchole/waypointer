package com.waypointer.model;

import net.runelite.api.coords.WorldPoint;

/**
 * Packs a {@link WorldPoint} into a single int. Layout matches shortest-path's WorldPointUtil
 * so the int can be passed straight through PluginMessage without conversion:
 *   bits  0..14  : x  (15 bits, range 0..32767)
 *   bits 15..29  : y  (15 bits)
 *   bits 30..31  : plane (2 bits, range 0..3)
 */
public final class WorldPointPacker
{
    public static final int UNDEFINED = -1;

    private static final int X_BITS = 15;
    private static final int Y_BITS = 15;
    private static final int X_MASK = (1 << X_BITS) - 1;
    private static final int Y_MASK = (1 << Y_BITS) - 1;

    private WorldPointPacker() {}

    public static int pack(int x, int y, int plane)
    {
        return (plane << (X_BITS + Y_BITS)) | ((y & Y_MASK) << X_BITS) | (x & X_MASK);
    }

    public static int pack(WorldPoint wp)
    {
        return pack(wp.getX(), wp.getY(), wp.getPlane());
    }

    public static WorldPoint unpack(int packed)
    {
        return new WorldPoint(getX(packed), getY(packed), getPlane(packed));
    }

    public static int getX(int packed) { return packed & X_MASK; }
    public static int getY(int packed) { return (packed >>> X_BITS) & Y_MASK; }
    public static int getPlane(int packed) { return (packed >>> (X_BITS + Y_BITS)) & 0x3; }
}
