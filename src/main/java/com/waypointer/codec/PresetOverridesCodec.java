package com.waypointer.codec;

import com.google.gson.Gson;
import com.waypointer.service.PresetOverridesSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Gson-backed encode/decode for {@link PresetOverridesSnapshot}. */
@Singleton
public class PresetOverridesCodec implements SnapshotCodec<PresetOverridesSnapshot>
{
    private final Gson gson;

    @Inject
    public PresetOverridesCodec(Gson gson)
    {
        this.gson = gson;
    }

    @Override
    public String encode(PresetOverridesSnapshot snapshot)
    {
        return gson.toJson(snapshot);
    }

    @Override
    public PresetOverridesSnapshot decode(String json)
    {
        return SnapshotCodec.decodeWithDefaults(gson, json, PresetOverridesSnapshot.class,
            PresetOverridesSnapshot::empty,
            decoded -> new PresetOverridesSnapshot(
                decoded.getVersion() == 0 ? PresetOverridesSnapshot.CURRENT_SCHEMA_VERSION : decoded.getVersion(),
                decoded.getByCategory() == null ? new LinkedHashMap<>() : decoded.getByCategory(),
                decoded.getAddedCategories() == null ? new ArrayList<>() : decoded.getAddedCategories(),
                decoded.getDeletedCategories() == null ? new ArrayList<>() : decoded.getDeletedCategories(),
                decoded.getDeletedWaypoints() == null ? new ArrayList<>() : decoded.getDeletedWaypoints()));
    }
}
