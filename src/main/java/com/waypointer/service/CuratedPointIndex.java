package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CuratedPointIndex
{
    private static final LinkedHashMap<String, String> RESOURCES = new LinkedHashMap<>();
    static
    {
        RESOURCES.put("/com/waypointer/landmarks/bank.tsv", " Bank");
        RESOURCES.put("/com/waypointer/landmarks/altar.tsv", " Altar");
        RESOURCES.put("/com/waypointer/landmarks/anvil.tsv", " Anvil");
        RESOURCES.put("/com/waypointer/landmarks/apothecary.tsv", " Apothecary");
    }

    private final Map<Integer, String> nameByPacked = new HashMap<>();

    @Inject
    public CuratedPointIndex()
    {
        for (Map.Entry<String, String> res : RESOURCES.entrySet())
        {
            loadResource(res.getKey(), res.getValue());
        }
        log.info("CuratedPointIndex loaded {} tile->name entries", nameByPacked.size());
    }

    @Nullable
    public String lookup(int packedPoint)
    {
        return nameByPacked.get(packedPoint);
    }

    private void loadResource(String resourcePath, String suffix)
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
                    String coordPart;
                    String name;
                    if (tabIdx < 0)
                    {
                        // No tab -- coords-only row (e.g. anvil.tsv). Name is blank; suffix provides the label.
                        coordPart = line.trim();
                        name = "";
                    }
                    else
                    {
                        coordPart = line.substring(0, tabIdx).trim();
                        String rest = line.substring(tabIdx + 1);
                        int nextTab = rest.indexOf('\t');
                        name = (nextTab < 0 ? rest : rest.substring(0, nextTab)).trim();
                    }
                    // Need a displayable label: either "Name Suffix" or just "Suffix" (trimmed) for blank names.
                    if (name.isEmpty() && suffix.isEmpty()) continue;
                    String[] parts = coordPart.split("\\s+");
                    if (parts.length < 3) continue;
                    try
                    {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int plane = Integer.parseInt(parts[2]);
                        int packed = WorldPointPacker.pack(x, y, plane);
                        String label = name.isEmpty() ? suffix.trim() : name + suffix;
                        nameByPacked.putIfAbsent(packed, label);
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
