package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CacheLabelIndex
{
    private static final String RESOURCE = "/com/waypointer/landmarks/map-labels-raw.tsv";
    private static final int CHUNK_SHIFT = 4;

    private static final int POI_RADIUS = 3;
    private static final int SUB_AREA_RADIUS = 50;
    private static final int CITY_RADIUS = 200;

    static final class Entry
    {
        final int packed;
        final String name;
        final int textScale;

        Entry(int packed, String name, int textScale)
        {
            this.packed = packed;
            this.name = name;
            this.textScale = textScale;
        }
    }

    private final List<Entry> all = new ArrayList<>();
    private final Map<Long, List<Entry>> byChunk = new HashMap<>();
    private final Map<String, Integer> nameCounts = new HashMap<>();

    @Inject
    public CacheLabelIndex()
    {
        load();
        log.info("CacheLabelIndex loaded {} cache-label entries", all.size());
    }

    /** Visible for tests: build an index from a literal list of entries instead of the TSV. */
    static CacheLabelIndex forTesting(Collection<Entry> entries)
    {
        CacheLabelIndex idx = new CacheLabelIndex(true);
        for (Entry e : entries)
        {
            idx.addEntry(e);
        }
        return idx;
    }

    private CacheLabelIndex(boolean skipResourceLoad)
    {
        // package-private ctor used only by forTesting()
    }

    public int size() { return all.size(); }

    @Nullable
    public LookupHit lookup(int packedPoint)
    {
        int x = WorldPointPacker.getX(packedPoint);
        int y = WorldPointPacker.getY(packedPoint);
        int plane = WorldPointPacker.getPlane(packedPoint);

        Entry best = null;
        int bestRank = Integer.MAX_VALUE;
        int bestDist = Integer.MAX_VALUE;
        int bestCount = Integer.MAX_VALUE;

        int chunks = (CITY_RADIUS >> CHUNK_SHIFT) + 1;
        int cx = x >> CHUNK_SHIFT;
        int cy = y >> CHUNK_SHIFT;
        for (int dxc = -chunks; dxc <= chunks; dxc++)
        {
            for (int dyc = -chunks; dyc <= chunks; dyc++)
            {
                List<Entry> bucket = byChunk.get(rawChunkKey(cx + dxc, cy + dyc, plane));
                if (bucket == null) continue;
                for (Entry e : bucket)
                {
                    if (WorldPointPacker.getPlane(e.packed) != plane) continue;
                    int dist = Math.max(
                        Math.abs(WorldPointPacker.getX(e.packed) - x),
                        Math.abs(WorldPointPacker.getY(e.packed) - y));

                    // Unique-name POIs get sub-area radius so they're not eclipsed by a city label
                    // just because the player is more than 3 tiles from the exact label tile.
                    boolean promoted = e.textScale == 0 && nameCounts.getOrDefault(e.name, 1) == 1;
                    int radius = promoted ? SUB_AREA_RADIUS : radiusFor(e.textScale);
                    if (dist > radius) continue;

                    int rank = promoted ? 1 : rankFor(e.textScale);
                    int count = nameCounts.getOrDefault(e.name, 1);
                    // Lower rank = tighter tier; tie-break by lower name count; then closer distance.
                    if (rank < bestRank
                        || (rank == bestRank && count < bestCount)
                        || (rank == bestRank && count == bestCount && dist < bestDist))
                    {
                        best = e;
                        bestRank = rank;
                        bestDist = dist;
                        bestCount = count;
                    }
                }
            }
        }
        if (best == null) return null;
        return new LookupHit(best.name, tierFor(best.textScale));
    }

    private static int radiusFor(int textScale)
    {
        switch (textScale)
        {
            case 0: return POI_RADIUS;
            case 1: return SUB_AREA_RADIUS;
            default: return CITY_RADIUS;
        }
    }

    private static int rankFor(int textScale)
    {
        switch (textScale)
        {
            case 0: return 0; // POI wins ties against sub-area / city
            case 1: return 1;
            default: return 2;
        }
    }

    private static LookupHit.Tier tierFor(int textScale)
    {
        switch (textScale)
        {
            case 0: return LookupHit.Tier.POI;
            case 1: return LookupHit.Tier.SUB_AREA;
            default: return LookupHit.Tier.CITY;
        }
    }

    private void addEntry(Entry e)
    {
        all.add(e);
        int x = WorldPointPacker.getX(e.packed);
        int y = WorldPointPacker.getY(e.packed);
        int plane = WorldPointPacker.getPlane(e.packed);
        byChunk.computeIfAbsent(rawChunkKey(x >> CHUNK_SHIFT, y >> CHUNK_SHIFT, plane),
            k -> new ArrayList<>()).add(e);
        nameCounts.merge(e.name, 1, Integer::sum);
    }

    private static long rawChunkKey(int cx, int cy, int plane)
    {
        return ((long) plane << 40) | ((long) cy << 20) | (cx & 0xFFFFF);
    }

    private void load()
    {
        try (InputStream in = CacheLabelIndex.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                log.warn("Cache-label resource not found: {}", RESOURCE);
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = r.readLine()) != null)
                {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] tabs = line.split("\t");
                    if (tabs.length < 5) continue;
                    String[] coord = tabs[0].split("\\s+");
                    if (coord.length < 3) continue;
                    try
                    {
                        int x = Integer.parseInt(coord[0]);
                        int y = Integer.parseInt(coord[1]);
                        int plane = Integer.parseInt(coord[2]);
                        String name = tabs[1].trim();
                        int textScale = Integer.parseInt(tabs[4].trim());
                        if (name.isEmpty()) continue;
                        addEntry(new Entry(WorldPointPacker.pack(x, y, plane), name, textScale));
                    }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to load cache-label resource {}", RESOURCE, e);
        }
    }
}
