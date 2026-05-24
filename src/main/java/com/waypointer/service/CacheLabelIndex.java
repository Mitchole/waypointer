package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CacheLabelIndex
{
    private static final String RESOURCE = "/com/waypointer/landmarks/map-labels-raw.tsv";
    private static final int CHUNK_SHIFT = 4;   // 16-tile chunks

    static final class Entry
    {
        final int packed;
        final String name;
        final int category;
        final int spriteId;
        final int textScale;

        Entry(int packed, String name, int category, int spriteId, int textScale)
        {
            this.packed = packed;
            this.name = name;
            this.category = category;
            this.spriteId = spriteId;
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

    public int size() { return all.size(); }

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
                        int category = Integer.parseInt(tabs[2].trim());
                        int spriteId = Integer.parseInt(tabs[3].trim());
                        int textScale = Integer.parseInt(tabs[4].trim());
                        if (name.isEmpty()) continue;

                        int packed = WorldPointPacker.pack(x, y, plane);
                        Entry e = new Entry(packed, name, category, spriteId, textScale);
                        all.add(e);
                        byChunk.computeIfAbsent(chunkKey(x, y, plane), k -> new ArrayList<>()).add(e);
                        nameCounts.merge(name, 1, Integer::sum);
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

    static long chunkKey(int x, int y, int plane)
    {
        return ((long) plane << 40) | ((long)(y >> CHUNK_SHIFT) << 20) | (long)(x >> CHUNK_SHIFT);
    }
}
