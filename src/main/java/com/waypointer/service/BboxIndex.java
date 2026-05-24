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
    private static final String[] RESOURCES = {
        "/com/waypointer/landmarks/banks-bboxes.tsv",
        "/com/waypointer/landmarks/altars-bboxes.tsv",
        "/com/waypointer/landmarks/anvils-bboxes.tsv",
        "/com/waypointer/landmarks/furnaces-bboxes.tsv",
        "/com/waypointer/landmarks/looms-bboxes.tsv",
        "/com/waypointer/landmarks/spinning-wheels-bboxes.tsv",
        "/com/waypointer/landmarks/tanners-bboxes.tsv",
        "/com/waypointer/landmarks/spirit-trees-bboxes.tsv",
        "/com/waypointer/landmarks/bank-chests-bboxes.tsv",
        "/com/waypointer/landmarks/charter-ships-bboxes.tsv",
        "/com/waypointer/landmarks/fairy-rings-bboxes.tsv",
        "/com/waypointer/landmarks/slayer-masters-bboxes.tsv",
        "/com/waypointer/landmarks/landmarks-bboxes.tsv",
    };

    static final class Entry
    {
        final int x1, y1, x2, y2, plane, area;
        final String name;
        @javax.annotation.Nullable
        final LandmarkType type;

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
    private int total;

    @Inject
    public BboxIndex()
    {
        for (String res : RESOURCES) loadResource(res);
        log.info("BboxIndex loaded {} bbox entries across {} planes", total, byPlane.size());
    }

    private BboxIndex(boolean skipResourceLoad)
    {
        // package-private; only used by forTesting()
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

    private void addEntry(Entry e)
    {
        byPlane.computeIfAbsent(e.plane, k -> new ArrayList<>()).add(e);
        total++;
    }

    private void loadResource(String resourcePath)
    {
        try (InputStream in = BboxIndex.class.getResourceAsStream(resourcePath))
        {
            if (in == null)
            {
                log.warn("Bbox resource not found: {}", resourcePath);
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
                        addEntry(new Entry(x1, y1, x2, y2, plane, name));
                    }
                    catch (NumberFormatException ex)
                    {
                        log.warn("Malformed bbox row in {}: {}", resourcePath, line);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to load bbox resource {}", resourcePath, e);
        }
    }
}
