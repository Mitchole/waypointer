package com.waypointer.sailing;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/** Global water/land oracle for ocean pathing. Loads the bundled, plane-0 collision map once.
 *  isWater is true only for tiles the dataset marks as open (sailable) water. */
@Slf4j
@Singleton
public class WaterMap
{
    static final int SEA_BOX_RADIUS = 3;
    static final int SEA_MIN_OPEN = 37;

    private static final String RESOURCE = "/com/waypointer/sailing/water-collision.txt";

    private final WaterCollisionData data;

    @Inject
    public WaterMap()
    {
        this(loadBundled());
    }

    WaterMap(WaterCollisionData data)
    {
        this.data = data;
        log.debug("WaterMap loaded {} chunks", data == null ? 0 : data.size());
    }

    public boolean isWater(int x, int y)
    {
        return data != null && data.flagAt(x, y) == WaterCollisionData.OPEN;
    }

    public boolean isWater(WorldPoint wp)
    {
        return wp != null && wp.getPlane() == 0 && isWater(wp.getX(), wp.getY());
    }

    /** True for open water, or for a blocked tile (such as a wreck) mostly ringed by open water:
     *  at least SEA_MIN_OPEN of the 49 tiles in the 7x7 box around it are open. */
    public boolean isAtSea(int x, int y)
    {
        if (isWater(x, y))
        {
            return true;
        }
        int open = 0;
        for (int dx = -SEA_BOX_RADIUS; dx <= SEA_BOX_RADIUS; dx++)
        {
            for (int dy = -SEA_BOX_RADIUS; dy <= SEA_BOX_RADIUS; dy++)
            {
                if (isWater(x + dx, y + dy))
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

    private static WaterCollisionData loadBundled()
    {
        try (InputStream in = WaterMap.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                log.warn("Water collision resource not found: {}", RESOURCE);
                return new WaterCollisionData(new HashMap<>());
            }
            Map<Long, WaterCollisionData.Chunk> chunks = WaterCollisionCodec.readText(in);
            return new WaterCollisionData(chunks == null ? new HashMap<>() : chunks);
        }
        catch (Exception e)
        {
            log.warn("Failed to load water collision data", e);
            return new WaterCollisionData(new HashMap<>());
        }
    }
}
