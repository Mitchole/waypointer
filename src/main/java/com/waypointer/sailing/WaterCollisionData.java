/*
 * Copyright (c) 2026, Dazuzi
 * Lifted (read-only subset) from the Chart Plotter plugin under BSD 2-Clause.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the above copyright notice and this notice are retained.
 * THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 */
package com.waypointer.sailing;

import java.util.Map;
import net.runelite.api.CollisionDataFlag;

/** Immutable collision lookup baked from Chart Plotter's map. OPEN = not movement-blocked,
 *  which covers walkable land as well as water (see {@link WaterMap} for what counts as sea),
 *  BLOCKED = blocked, UNKNOWN = not in the dataset. Plane 0 only. */
public final class WaterCollisionData
{
    public static final int UNKNOWN = -1;
    public static final int OPEN = 0;
    public static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL;

    private final Map<Long, Chunk> base;

    public WaterCollisionData(Map<Long, Chunk> base)
    {
        this.base = base;
    }

    public int flagAt(int x, int y)
    {
        Chunk c = base.get(key(x >> 3, y >> 3));
        return c == null ? UNKNOWN : c.flag((x & 7) + ((y & 7) << 3));
    }

    public int size() { return base.size(); }

    /** The chunks by {@link #key}. Read only. */
    Map<Long, Chunk> chunks() { return base; }

    public static long key(int x, int y) { return (long) x << 32 ^ y & 0xffffffffL; }

    public static final class Chunk
    {
        public final long known;
        public final long blocked;

        public Chunk(long known, long blocked)
        {
            this.known = known;
            this.blocked = blocked;
        }

        public int flag(int i)
        {
            long b = 1L << i;
            return (known & b) == 0 ? UNKNOWN : (blocked & b) == 0 ? OPEN : BLOCKED;
        }
    }
}
