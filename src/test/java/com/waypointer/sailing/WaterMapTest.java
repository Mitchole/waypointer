package com.waypointer.sailing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import org.junit.BeforeClass;
import org.junit.Test;

public class WaterMapTest
{
    private static final String[] LANDMARK_FILES = {
        "altars", "anvils", "bank-chests", "banks", "charter-ships", "fairy-rings", "furnaces",
        "landmarks", "looms", "slayer-masters", "spinning-wheels", "spirit-trees", "tanners",
    };

    /** The one bundled landmark that really is on the open sea. */
    private static final String KNOWN_AT_SEA = "Bank Boat Bank";

    private static WaterMap map;

    @BeforeClass
    public static void loadRealData()
    {
        map = new WaterMap(); // the real bundled resource
        map.warm();
    }

    @Test
    public void portSarimSeaIsWater()
    {
        assertTrue(map.isWater(WaterMap.SEED_X, WaterMap.SEED_Y));
        assertTrue(map.isAtSea(WaterMap.SEED_X, WaterMap.SEED_Y));
    }

    @Test
    public void adjacentDockTileIsLand()
    {
        assertFalse(map.isWater(3048, 3192));
        assertFalse(map.isAtSea(3048, 3192));
    }

    @Test
    public void tileMissingFromTheDataIsLand()
    {
        assertFalse(map.isWater(3222, 3218));
        assertFalse(map.isAtSea(3222, 3218));
    }

    @Test
    public void wrongPlaneIsNotWater()
    {
        assertFalse(map.isWater(new WorldPoint(3051, 3195, 1)));
        assertFalse(map.isAtSea(new WorldPoint(3051, 3195, 1)));
    }

    @Test
    public void walkableTownTilesAreLand()
    {
        // All three are open (walkable) in the collision map but not connected to the sea.
        int[][] land = { {3166, 3487}, {3254, 3421}, {3093, 3244} }; // GE, Varrock East Bank, Draynor Bank
        for (int[] t : land)
        {
            assertFalse(t[0] + "," + t[1], map.isWater(t[0], t[1]));
            assertFalse(t[0] + "," + t[1], map.isAtSea(t[0], t[1]));
        }
    }

    @Test
    public void seaHoldsMostOpenTiles() throws IOException
    {
        // Guards against a seed that lands in a lake after a data refresh.
        long open = 0;
        for (WaterCollisionData.Chunk c : readRealChunks().values())
        {
            open += Long.bitCount(c.known & ~c.blocked);
        }
        assertTrue("sea " + map.seaSize() + " of open " + open, map.seaSize() * 2L > open);
    }

    @Test
    public void bundledLandmarksAndPresetsOnLandAreNotAtSea() throws IOException
    {
        List<String> atSea = new ArrayList<>();
        int checked = 0;
        for (String file : LANDMARK_FILES)
        {
            try (BufferedReader in = reader("/com/waypointer/landmarks/" + file + "-bboxes.tsv"))
            {
                String line;
                while ((line = in.readLine()) != null)
                {
                    if (line.startsWith("#") || line.trim().isEmpty()) continue;
                    String[] cols = line.split("\t");
                    String[] c = cols[0].trim().split("\\s+");
                    int x = (Integer.parseInt(c[0]) + Integer.parseInt(c[2])) / 2;
                    int y = (Integer.parseInt(c[1]) + Integer.parseInt(c[3])) / 2;
                    checked++;
                    if (map.isAtSea(new WorldPoint(x, y, Integer.parseInt(c[4]))) && !KNOWN_AT_SEA.equals(cols[1]))
                    {
                        atSea.add(file + ": " + cols[1]);
                    }
                }
            }
        }
        try (BufferedReader in = reader("/com/waypointer/preset-waypoints.json"))
        {
            JsonObject root = new JsonParser().parse(in).getAsJsonObject();
            for (JsonElement cat : root.getAsJsonArray("presets"))
            {
                for (JsonElement w : cat.getAsJsonObject().getAsJsonArray("waypoints"))
                {
                    JsonObject o = w.getAsJsonObject();
                    int plane = o.has("plane") ? o.get("plane").getAsInt() : 0;
                    checked++;
                    if (map.isAtSea(new WorldPoint(o.get("x").getAsInt(), o.get("y").getAsInt(), plane)))
                    {
                        atSea.add("preset: " + o.get("name").getAsString());
                    }
                }
            }
        }
        assertTrue("nothing checked", checked > 500);
        assertEquals(new ArrayList<String>(), atSea);
    }

    @Test
    public void bankBoatIsAtSea()
    {
        assertTrue(map.isAtSea(2280, 2544));
    }

    @Test
    public void everyDockIsWithinThreeTilesOfTheSea()
    {
        for (SailingDock dock : SailingDock.values())
        {
            WorldPoint p = dock.getNavigationLocation();
            boolean near = false;
            for (int dx = -3; dx <= 3 && !near; dx++)
            {
                for (int dy = -3; dy <= 3 && !near; dy++)
                {
                    near = map.isWater(p.getX() + dx, p.getY() + dy);
                }
            }
            assertTrue(dock.name(), near);
        }
    }

    @Test
    public void blockedTileInOpenWaterIsAtSea()
    {
        assertTrue(fixture(48).isAtSea(1000, 1000));
    }

    @Test
    public void halfWaterCoastIsNotAtSea()
    {
        assertFalse(fixture(24).isAtSea(1000, 1000));
    }

    @Test
    public void thresholdIsThirtySevenOfFortyNine()
    {
        assertFalse(fixture(36).isAtSea(1000, 1000));
        assertTrue(fixture(37).isAtSea(1000, 1000));
    }

    @Test
    public void openTilesCutOffFromTheSeedAreLand()
    {
        // Two open tiles with a blocked one between them: only the seed's side is sea.
        Map<Long, WaterCollisionData.Chunk> chunks = new HashMap<>();
        long known = 0b111L;
        long blocked = 0b010L;
        chunks.put(WaterCollisionData.key(0, 0), new WaterCollisionData.Chunk(known, blocked));
        WaterMap m = new WaterMap(new WaterCollisionData(chunks), 0, 0);
        assertTrue(m.isWater(0, 0));
        assertFalse(m.isWater(1, 0));
        assertFalse(m.isWater(2, 0));
    }

    @Test
    public void seedThatIsNotOpenLeavesEverythingLand()
    {
        WaterMap m = new WaterMap(new WaterCollisionData(new HashMap<>()), 0, 0);
        assertFalse(m.isWater(0, 0));
        assertFalse(m.isAtSea(0, 0));
    }

    /** Known area 984..1015 squared, all blocked except {@code open} tiles taken in row order
     *  from the 7x7 box around (1000, 1000). The centre is never open. The open tiles touch, and
     *  the first of them, (997, 997), seeds the sea. */
    private static WaterMap fixture(int open)
    {
        Map<Long, WaterCollisionData.Chunk> chunks = new HashMap<>();
        int remaining = open;
        for (int x = 984; x < 1016; x++)
        {
            for (int y = 984; y < 1016; y++)
            {
                boolean inBox = Math.abs(x - 1000) <= 3 && Math.abs(y - 1000) <= 3;
                boolean isOpen = false;
                if (inBox && !(x == 1000 && y == 1000) && remaining > 0)
                {
                    isOpen = true;
                    remaining--;
                }
                long key = WaterCollisionData.key(x >> 3, y >> 3);
                WaterCollisionData.Chunk c = chunks.get(key);
                long known = c == null ? 0 : c.known;
                long blocked = c == null ? 0 : c.blocked;
                long bit = 1L << ((x & 7) + ((y & 7) << 3));
                known |= bit;
                if (!isOpen)
                {
                    blocked |= bit;
                }
                chunks.put(key, new WaterCollisionData.Chunk(known, blocked));
            }
        }
        return new WaterMap(new WaterCollisionData(chunks), 997, 997);
    }

    private static Map<Long, WaterCollisionData.Chunk> readRealChunks() throws IOException
    {
        try (InputStream in = WaterMapTest.class.getResourceAsStream("/com/waypointer/sailing/water-collision.txt"))
        {
            return WaterCollisionCodec.readText(in);
        }
    }

    private static BufferedReader reader(String resource)
    {
        InputStream in = WaterMapTest.class.getResourceAsStream(resource);
        if (in == null) throw new AssertionError("missing resource " + resource);
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
}
