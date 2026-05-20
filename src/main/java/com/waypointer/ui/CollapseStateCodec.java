package com.waypointer.ui;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;

/** Codec for the categoryCollapsedJson config string. Map<categoryId, collapsed?>. */
public class CollapseStateCodec
{
    private final Gson gson;

    @Inject
    public CollapseStateCodec(Gson gson) { this.gson = gson; }

    public Map<UUID, Boolean> decode(String s)
    {
        if (s == null || s.isEmpty()) return new HashMap<>();
        try
        {
            Map<String, Boolean> raw = gson.fromJson(s,
                new TypeToken<Map<String, Boolean>>(){}.getType());
            if (raw == null) return new HashMap<>();
            Map<UUID, Boolean> out = new HashMap<>();
            for (Map.Entry<String, Boolean> e : raw.entrySet())
            {
                try { out.put(UUID.fromString(e.getKey()), e.getValue()); }
                catch (IllegalArgumentException ignored) {}
            }
            return out;
        }
        catch (JsonSyntaxException e)
        {
            return new HashMap<>();
        }
    }

    public String encode(Map<UUID, Boolean> map)
    {
        Map<String, Boolean> raw = new HashMap<>();
        for (Map.Entry<UUID, Boolean> e : map.entrySet())
        {
            raw.put(e.getKey().toString(), e.getValue());
        }
        return gson.toJson(raw);
    }
}
