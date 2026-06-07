package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.util.Listeners;
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
public class BboxIndex
{
    private static final class ResourceEntry
    {
        final String path;
        @Nullable
        final LandmarkType type;

        ResourceEntry(String path, @Nullable LandmarkType type)
        {
            this.path = path;
            this.type = type;
        }
    }

    private static final ResourceEntry[] RESOURCES = {
        new ResourceEntry("/com/waypointer/landmarks/banks-bboxes.tsv",          LandmarkType.BANK),
        new ResourceEntry("/com/waypointer/landmarks/bank-chests-bboxes.tsv",    LandmarkType.BANK),
        new ResourceEntry("/com/waypointer/landmarks/altars-bboxes.tsv",         LandmarkType.ALTAR),
        new ResourceEntry("/com/waypointer/landmarks/anvils-bboxes.tsv",         LandmarkType.ANVIL),
        new ResourceEntry("/com/waypointer/landmarks/furnaces-bboxes.tsv",       LandmarkType.FURNACE),
        new ResourceEntry("/com/waypointer/landmarks/looms-bboxes.tsv",          LandmarkType.LOOM),
        new ResourceEntry("/com/waypointer/landmarks/spinning-wheels-bboxes.tsv", LandmarkType.SPINNING_WHEEL),
        new ResourceEntry("/com/waypointer/landmarks/tanners-bboxes.tsv",        LandmarkType.TANNER),
        new ResourceEntry("/com/waypointer/landmarks/spirit-trees-bboxes.tsv",   LandmarkType.SPIRIT_TREE),
        new ResourceEntry("/com/waypointer/landmarks/charter-ships-bboxes.tsv",  LandmarkType.CHARTER_SHIP),
        new ResourceEntry("/com/waypointer/landmarks/fairy-rings-bboxes.tsv",    LandmarkType.FAIRY_RING),
        new ResourceEntry("/com/waypointer/landmarks/slayer-masters-bboxes.tsv", LandmarkType.SLAYER_MASTER),
        new ResourceEntry("/com/waypointer/landmarks/landmarks-bboxes.tsv",      null),
    };

    public static final class Entry
    {
        public final int x1, y1, x2, y2, plane, area;
        public final String name;
        @Nullable
        public final LandmarkType type;

        Entry(int x1, int y1, int x2, int y2, int plane, String name)
        {
            this(x1, y1, x2, y2, plane, name, null);
        }

        Entry(int x1, int y1, int x2, int y2, int plane, String name,
            @Nullable LandmarkType type)
        {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.plane = plane;
            this.name = name;
            this.type = type;
            this.area = (x2 - x1 + 1) * (y2 - y1 + 1);
        }
    }

    private final Map<Integer, List<Entry>> byPlane = new HashMap<>();
    private final Listeners listeners = new Listeners();
    private int total;

    @Inject
    public BboxIndex()
    {
        for (ResourceEntry res : RESOURCES) loadResource(res);
        log.info("BboxIndex loaded {} bbox entries across {} planes", total, byPlane.size());
    }

    private BboxIndex(boolean skipResourceLoad)
    {
        // Test seam: forTesting() builds an index from explicit entries.
    }

    public Listeners.Subscription subscribe(Runnable r)
    {
        return listeners.subscribe(r);
    }

    static BboxIndex forTesting(Collection<Entry> entries)
    {
        BboxIndex idx = new BboxIndex(true);
        for (Entry e : entries) idx.addEntry(e);
        return idx;
    }

    @Nullable
    public String lookup(int packedPoint)
    {
        int x = WorldPointPacker.getX(packedPoint);
        int y = WorldPointPacker.getY(packedPoint);
        int plane = WorldPointPacker.getPlane(packedPoint);
        List<Entry> candidates = byPlane.get(plane);
        if (candidates == null) return null;

        Entry best = null;
        for (Entry e : candidates)
        {
            if (x < e.x1 || x > e.x2 || y < e.y1 || y > e.y2) continue;
            if (best == null || e.area < best.area) best = e;
        }
        return best == null ? null : best.name;
    }

    public static final class Hit
    {
        public final int packed;
        public final String name;
        public final int distance;

        public Hit(int packed, String name, int distance)
        {
            this.packed = packed;
            this.name = name;
            this.distance = distance;
        }
    }

    /**
     * Returns the entry of the requested type whose bbox is closest to {@code fromPacked}
     * in Chebyshev tile distance, plane-agnostic. Distance ranks by the nearest part of the
     * bbox (clamped), but the returned {@link Hit#packed} is the bbox CENTRE tile, carrying the
     * bbox's plane -- quick-nav should walk you to the middle of the landmark, not to a corner
     * of its bounding box. Returns null if the index holds no entries of that type.
     */
    @Nullable
    public Hit nearest(LandmarkType type, int fromPacked)
    {
        int fx = WorldPointPacker.getX(fromPacked);
        int fy = WorldPointPacker.getY(fromPacked);

        Entry best = null;
        int bestDist = Integer.MAX_VALUE;

        for (List<Entry> bucket : byPlane.values())
        {
            for (Entry e : bucket)
            {
                if (e.type != type) continue;
                int cx = Math.max(e.x1, Math.min(fx, e.x2));
                int cy = Math.max(e.y1, Math.min(fy, e.y2));
                int dx = Math.abs(cx - fx);
                int dy = Math.abs(cy - fy);
                int d = Math.max(dx, dy);
                if (d < bestDist)
                {
                    bestDist = d;
                    best = e;
                }
            }
        }
        if (best == null) return null;
        int centreX = (best.x1 + best.x2) / 2;
        int centreY = (best.y1 + best.y2) / 2;
        return new Hit(WorldPointPacker.pack(centreX, centreY, best.plane), best.name, bestDist);
    }

    private void addEntry(Entry e)
    {
        addEntryInternal(e);
    }

    private void addEntryInternal(Entry e)
    {
        byPlane.computeIfAbsent(e.plane, k -> new ArrayList<>()).add(e);
        total++;
    }

    private void loadResource(ResourceEntry res)
    {
        try (InputStream in = BboxIndex.class.getResourceAsStream(res.path))
        {
            if (in == null)
            {
                log.warn("Bbox resource not found: {}", res.path);
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = r.readLine()) != null)
                {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int tab = line.indexOf('\t');
                    if (tab < 0) continue;
                    String coordPart = line.substring(0, tab).trim();
                    String name = line.substring(tab + 1).trim();
                    if (name.isEmpty()) continue;
                    String[] parts = coordPart.split("\\s+");
                    if (parts.length < 5) continue;
                    try
                    {
                        int x1 = Integer.parseInt(parts[0]);
                        int y1 = Integer.parseInt(parts[1]);
                        int x2 = Integer.parseInt(parts[2]);
                        int y2 = Integer.parseInt(parts[3]);
                        int plane = Integer.parseInt(parts[4]);
                        addEntry(new Entry(x1, y1, x2, y2, plane, name, res.type));
                    }
                    catch (NumberFormatException ex)
                    {
                        log.warn("Malformed bbox row in {}: {}", res.path, line);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to load bbox resource {}", res.path, e);
        }
    }
}
