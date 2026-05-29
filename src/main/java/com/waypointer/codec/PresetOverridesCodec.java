package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.waypointer.service.PresetOverridesSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Gson-backed encode/decode for {@link PresetOverridesSnapshot}. */
@Singleton
public class PresetOverridesCodec
{
    private final Gson gson;

    @Inject
    public PresetOverridesCodec(Gson gson)
    {
        this.gson = gson;
    }

    public String encode(PresetOverridesSnapshot snapshot)
    {
        return gson.toJson(snapshot);
    }

    public PresetOverridesSnapshot decode(String json)
    {
        if (json == null || json.isEmpty()) return PresetOverridesSnapshot.empty();
        try
        {
            PresetOverridesSnapshot decoded = gson.fromJson(json, PresetOverridesSnapshot.class);
            if (decoded == null) return PresetOverridesSnapshot.empty();
            return new PresetOverridesSnapshot(
                decoded.getVersion() == 0 ? PresetOverridesSnapshot.CURRENT_SCHEMA_VERSION : decoded.getVersion(),
                decoded.getByCategory() == null ? new LinkedHashMap<>() : decoded.getByCategory(),
                decoded.getAddedCategories() == null ? new ArrayList<>() : decoded.getAddedCategories(),
                decoded.getDeletedCategories() == null ? new ArrayList<>() : decoded.getDeletedCategories(),
                decoded.getDeletedWaypoints() == null ? new ArrayList<>() : decoded.getDeletedWaypoints());
        }
        catch (JsonParseException e)
        {
            return PresetOverridesSnapshot.empty();
        }
    }
}
