package com.waypointer.sailing;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Global sea/land oracle for ocean pathing, plane 0 only.
 *
 * <p>The bundled collision map marks every tile that does not block movement as open, and that
 * includes ordinary walkable land. So the sea is taken to be the open tiles connected (4-way) to
 * one seed tile of open ocean off Port Sarim. Inland lakes, rivers and towns are open too but are
 * not connected to it, so they count as land.
 *
 * <p>The map is parsed and the sea filled on first use, once. {@link #warm()} does that early,
 * off the game threads, for users who can sail.
 */
@Slf4j
@Singleton
public class WaterMap
{
    static final int SEA_BOX_RADIUS = 3;
    static final int SEA_MIN_OPEN = 37;
    /** Open ocean off Port Sarim. */
    static final int SEED_X = 3051;
    static final int SEED_Y = 3195;

    private static final String RESOURCE = "/com/waypointer/sailing/water-collision.txt";

    private final Supplier<WaterCollisionData> source;
    private final int seedX;
    private final int seedY;
    private volatile SeaMask sea;

    @Inject
    public WaterMap()
    {
        this(WaterMap::loadBundled, SEED_X, SEED_Y);
    }

    /** A map over the given data, with the sea grown from (seedX, seedY). */
    WaterMap(WaterCollisionData data, int seedX, int seedY)
    {
        this(() -> data, seedX, seedY);
    }

    private WaterMap(Supplier<WaterCollisionData> source, int seedX, int seedY)
    {
        this.source = source;
        this.seedX = seedX;
        this.seedY = seedY;
    }

    /** Parses the map and fills the sea now if that has not happened yet. Blocks until done. */
    public void warm()
    {
        sea();
    }

    /** True when the tile is part of the sea. */
    public boolean isWater(int x, int y)
    {
        return sea().contains(x, y);
    }

    public boolean isWater(WorldPoint wp)
    {
        return wp != null && wp.getPlane() == 0 && isWater(wp.getX(), wp.getY());
    }

    /** True for a sea tile, or for a blocked tile (such as a wreck) mostly ringed by sea:
     *  at least SEA_MIN_OPEN of the 49 tiles in the 7x7 box around it are sea. */
    public boolean isAtSea(int x, int y)
    {
        SeaMask s = sea();
        if (s.contains(x, y))
        {
            return true;
        }
        int open = 0;
        for (int dx = -SEA_BOX_RADIUS; dx <= SEA_BOX_RADIUS; dx++)
        {
            for (int dy = -SEA_BOX_RADIUS; dy <= SEA_BOX_RADIUS; dy++)
            {
                if (s.contains(x + dx, y + dy))
                {
                    open++;
                }
            }
        }
        return open >= SEA_MIN_OPEN;
    }

    public boolean isAtSea(WorldPoint wp)
    {
        return wp != null && wp.getPlane() == 0 && isAtSea(wp.getX(), wp.getY());
    }

    /** Number of sea tiles. */
    int seaSize()
    {
        return sea().size;
    }

    private SeaMask sea()
    {
        SeaMask s = sea;
        if (s != null)
        {
            return s;
        }
        // A query that arrives while warm() is still running waits here for that result.
        synchronized (this)
        {
            if (sea == null)
            {
                sea = build();
            }
            return sea;
        }
    }

    private SeaMask build()
    {
        try
        {
            long start = System.nanoTime();
            WaterCollisionData data = source.get();
            SeaMask s = data == null ? SeaMask.EMPTY : SeaMask.flood(data, seedX, seedY);
            log.debug("Sea map ready: {} sea tiles in {} ms", s.size, (System.nanoTime() - start) / 1_000_000);
            return s;
        }
        catch (RuntimeException e)
        {
            log.warn("Failed to build the sea map; treating everything as land", e);
            return SeaMask.EMPTY;
        }
    }

    private static WaterCollisionData loadBundled()
    {
        try (InputStream in = WaterMap.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                log.warn("Water collision resource not found: {}", RESOURCE);
                return null;
            }
            Map<Long, WaterCollisionData.Chunk> chunks = WaterCollisionCodec.readText(in);
            return chunks == null ? null : new WaterCollisionData(chunks);
        }
        catch (Exception e)
        {
            log.warn("Failed to load water collision data", e);
            return null;
        }
    }

    /** Sea tiles as one 64-bit mask per 8x8 chunk, keyed and sorted by chunk. */
    private static final class SeaMask
    {
        static final SeaMask EMPTY = new SeaMask(new long[0], new long[0], 0);

        private final long[] keys;
        private final long[] masks;
        final int size;

        private SeaMask(long[] keys, long[] masks, int size)
        {
            this.keys = keys;
            this.masks = masks;
            this.size = size;
        }

        boolean contains(int x, int y)
        {
            int i = Arrays.binarySearch(keys, WaterCollisionData.key(x >> 3, y >> 3));
            return i >= 0 && (masks[i] & 1L << ((x & 7) + ((y & 7) << 3))) != 0;
        }

        static SeaMask flood(WaterCollisionData data, int seedX, int seedY)
        {
            if (data.flagAt(seedX, seedY) != WaterCollisionData.OPEN)
            {
                log.warn("Sea seed ({}, {}) is not open water; treating everything as land", seedX, seedY);
                return EMPTY;
            }
            // Copy the open tiles into a dense grid of chunks for the fill; it is dropped after.
            int minCx = Integer.MAX_VALUE, minCy = Integer.MAX_VALUE, maxCx = 0, maxCy = 0;
            for (long key : data.chunks().keySet())
            {
                int cx = (int) (key >> 32), cy = (int) key;
                minCx = Math.min(minCx, cx);
                minCy = Math.min(minCy, cy);
                maxCx = Math.max(maxCx, cx);
                maxCy = Math.max(maxCy, cy);
            }
            int h = maxCy - minCy + 1;
            long[] open = new long[(maxCx - minCx + 1) * h];
            for (Map.Entry<Long, WaterCollisionData.Chunk> e : data.chunks().entrySet())
            {
                long key = e.getKey();
                WaterCollisionData.Chunk c = e.getValue();
                open[((int) (key >> 32) - minCx) * h + (int) key - minCy] = c.known & ~c.blocked;
            }
            long[] filled = new long[open.length];
            int minX = minCx << 3, minY = minCy << 3, maxX = (maxCx << 3) + 7, maxY = (maxCy << 3) + 7;

            int[] queue = new int[1 << 16];
            int head = 0;
            int tail = 0;
            int size = 0;
            int[] dxs = {1, -1, 0, 0};
            int[] dys = {0, 0, 1, -1};
            fill(filled, open, h, minCx, minCy, seedX, seedY);
            queue[tail++] = pack(seedX, seedY);
            size++;
            while (head != tail)
            {
                int p = queue[head];
                head = (head + 1) & (queue.length - 1);
                int x = p >>> 16;
                int y = p & 0xffff;
                for (int d = 0; d < 4; d++)
                {
                    int nx = x + dxs[d];
                    int ny = y + dys[d];
                    if (nx < minX || ny < minY || nx > maxX || ny > maxY) continue;
                    if (!fill(filled, open, h, minCx, minCy, nx, ny)) continue;
                    size++;
                    if (((tail + 1) & (queue.length - 1)) == head)
                    {
                        int[] grown = new int[queue.length * 2];
                        int n = 0;
                        for (int i = head; i != tail; i = (i + 1) & (queue.length - 1))
                        {
                            grown[n++] = queue[i];
                        }
                        queue = grown;
                        head = 0;
                        tail = n;
                    }
                    queue[tail] = pack(nx, ny);
                    tail = (tail + 1) & (queue.length - 1);
                }
            }

            // Keep only the chunks that hold sea. Walking the grid x-major gives keys in order.
            int count = 0;
            for (long m : filled)
            {
                if (m != 0) count++;
            }
            long[] keys = new long[count];
            long[] masks = new long[count];
            int k = 0;
            for (int i = 0; i < filled.length; i++)
            {
                if (filled[i] == 0) continue;
                keys[k] = WaterCollisionData.key(minCx + i / h, minCy + i % h);
                masks[k++] = filled[i];
            }
            return new SeaMask(keys, masks, size);
        }

        /** Marks an open tile as sea; false when it is not open or already marked. */
        private static boolean fill(long[] filled, long[] open, int h, int minCx, int minCy, int x, int y)
        {
            int i = ((x >> 3) - minCx) * h + (y >> 3) - minCy;
            long bit = 1L << ((x & 7) + ((y & 7) << 3));
            if ((open[i] & bit) == 0 || (filled[i] & bit) != 0) return false;
            filled[i] |= bit;
            return true;
        }

        private static int pack(int x, int y)
        {
            return x << 16 | y;
        }
    }
}
