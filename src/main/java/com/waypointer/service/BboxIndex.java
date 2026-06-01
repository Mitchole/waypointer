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
public class BboxIndex
{
    private static final class ResourceEntry
    {
        final String path;
        @javax.annotation.Nullable
        final LandmarkType type;

        ResourceEntry(String path, @javax.annotation.Nullable LandmarkType type)
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
        @javax.annotation.Nullable
        public final LandmarkType type;

        Entry(int x1, int y1, int x2, int y2, int plane, String name)
        {
            this(x1, y1, x2, y2, plane, name, null);
        }

        Entry(int x1, int y1, int x2, int y2, int plane, String name,
            @javax.annotation.Nullable LandmarkType type)
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
    private final com.waypointer.util.Listeners listeners = new com.waypointer.util.Listeners();
    private final List<Entry> bundled = new ArrayList<>();
    private int total;

    @Inject
    public BboxIndex(LandmarkOverrides overrides)
    {
        for (ResourceEntry res : RESOURCES) loadResource(res);
        log.info("BboxIndex loaded {} bbox entries across {} planes", total, byPlane.size());
        applyOverrides(overrides.getSnapshot());
        overrides.subscribe(() -> reload(overrides.getSnapshot()));
    }

    private BboxIndex(boolean skipResourceLoad)
    {
        // package-private; only used by forTesting()
    }

    public com.waypointer.util.Listeners.Subscription subscribe(Runnable r)
    {
        return listeners.subscribe(r);
    }

    /**
     * Per-entry editor view of a type: every bundled entry of the type that has not been deleted,
     * followed by any override-added entries of the type. Unlike the live index (see
     * {@link #applyOverrides}) this does NOT apply the wholesale "a type override replaces the
     * whole bundled type" rule, so the dev editor can list and curate individual bundled entries
     * even once overrides exist for that type. Reflects deletions, additions, and edits (an edit
     * is recorded as a deletion of the original plus an added entry).
     */
    public java.util.List<Entry> editableOfType(LandmarkType t, LandmarkOverridesSnapshot s)
    {
        java.util.List<Entry> out = new java.util.ArrayList<>();
        for (Entry b : bundled)
        {
            if (b.type == t && !isDeleted(b, s)) out.add(b);
        }
        LandmarkOverridesSnapshot.TypeOverride ov = s.getByType().get(t.name());
        if (ov != null)
        {
            for (LandmarkOverridesSnapshot.Entry e : ov.getEntries())
            {
                out.add(new Entry(e.getX1(), e.getY1(), e.getX2(), e.getY2(), e.getPlane(), e.getName(), t));
            }
        }
        return out;
    }

    public void reload(LandmarkOverridesSnapshot s)
    {
        applyOverrides(s);
        listeners.fire();
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
     * in Chebyshev tile distance, plane-agnostic. The returned {@link Hit#packed} is the
     * bbox tile nearest the player (clamped), carrying the bbox's plane. Returns null if
     * the index holds no entries of that type.
     */
    @Nullable
    public Hit nearest(LandmarkType type, int fromPacked)
    {
        int fx = WorldPointPacker.getX(fromPacked);
        int fy = WorldPointPacker.getY(fromPacked);

        Entry best = null;
        int bestDist = Integer.MAX_VALUE;
        int bestCx = 0;
        int bestCy = 0;

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
                    bestCx = cx;
                    bestCy = cy;
                }
            }
        }
        if (best == null) return null;
        return new Hit(WorldPointPacker.pack(bestCx, bestCy, best.plane), best.name, bestDist);
    }

    private void addEntry(Entry e)
    {
        addEntryInternal(e);
        bundled.add(e);
    }

    private void addEntryInternal(Entry e)
    {
        byPlane.computeIfAbsent(e.plane, k -> new ArrayList<>()).add(e);
        total++;
    }

    public void applyOverrides(LandmarkOverridesSnapshot s)
    {
        byPlane.clear();
        total = 0;

        java.util.Set<String> replacedTypes = new java.util.HashSet<>(s.getByType().keySet());

        for (Entry b : bundled)
        {
            if (b.type != null && replacedTypes.contains(b.type.name())) continue;
            if (isDeleted(b, s)) continue;
            addEntryInternal(b);
        }
        for (Map.Entry<String, LandmarkOverridesSnapshot.TypeOverride> ent : s.getByType().entrySet())
        {
            LandmarkType t;
            try { t = LandmarkType.valueOf(ent.getKey()); }
            catch (IllegalArgumentException e) { log.warn("Unknown landmark type in override: {}", ent.getKey()); continue; }
            for (LandmarkOverridesSnapshot.Entry e : ent.getValue().getEntries())
            {
                addEntryInternal(new Entry(e.getX1(), e.getY1(), e.getX2(), e.getY2(), e.getPlane(), e.getName(), t));
            }
        }
    }

    private boolean isDeleted(Entry b, LandmarkOverridesSnapshot s)
    {
        if (b.type == null) return false;
        for (LandmarkOverridesSnapshot.DeletedEntry d : s.getDeletions())
        {
            if (!d.getType().equals(b.type.name())) continue;
            if (d.getX1() == b.x1 && d.getY1() == b.y1
                && d.getX2() == b.x2 && d.getY2() == b.y2
                && d.getPlane() == b.plane
                && java.util.Objects.equals(d.getName(), b.name)) return true;
        }
        return false;
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
