package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CuratedPointIndex
{
    private static final String[] RESOURCES = {
        "/com/waypointer/landmarks/bank.tsv",
        "/com/waypointer/landmarks/altar.tsv",
        "/com/waypointer/landmarks/anvil.tsv",
        "/com/waypointer/landmarks/apothecary.tsv",
    };

    private final Map<Integer, String> nameByPacked = new HashMap<>();

    @Inject
    public CuratedPointIndex()
    {
        for (String res : RESOURCES)
        {
            loadResource(res);
        }
        log.info("CuratedPointIndex loaded {} tile->name entries", nameByPacked.size());
    }

    @Nullable
    public String lookup(int packedPoint)
    {
        return nameByPacked.get(packedPoint);
    }

    private void loadResource(String resourcePath)
    {
        try (InputStream in = CuratedPointIndex.class.getResourceAsStream(resourcePath))
        {
            if (in == null)
            {
                log.warn("Landmark resource not found: {}", resourcePath);
                return;
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = r.readLine()) != null)
                {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int tabIdx = line.indexOf('\t');
                    if (tabIdx < 0) continue;
                    String coordPart = line.substring(0, tabIdx).trim();
                    String rest = line.substring(tabIdx + 1);
                    int nextTab = rest.indexOf('\t');
                    String name = (nextTab < 0 ? rest : rest.substring(0, nextTab)).trim();
                    if (name.isEmpty()) continue;
                    String[] parts = coordPart.split("\\s+");
                    if (parts.length < 3) continue;
                    try
                    {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int plane = Integer.parseInt(parts[2]);
                        int packed = WorldPointPacker.pack(x, y, plane);
                        nameByPacked.putIfAbsent(packed, name);
                    }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to load landmark resource {}", resourcePath, e);
        }
    }
}
