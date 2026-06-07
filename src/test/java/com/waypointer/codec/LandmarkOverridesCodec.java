package com.waypointer.codec;

import com.google.gson.Gson;
import com.waypointer.service.LandmarkOverridesSnapshot;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Gson-backed encode/decode for {@link LandmarkOverridesSnapshot}. */
@Singleton
public class LandmarkOverridesCodec implements SnapshotCodec<LandmarkOverridesSnapshot>
{
    private final Gson gson;

    @Inject
    public LandmarkOverridesCodec(Gson gson)
    {
        this.gson = gson;
    }

    @Override
    public String encode(LandmarkOverridesSnapshot snapshot)
    {
        return gson.toJson(snapshot);
    }

    @Override
    public LandmarkOverridesSnapshot decode(String json)
    {
        return SnapshotCodec.decodeWithDefaults(gson, json, LandmarkOverridesSnapshot.class,
            LandmarkOverridesSnapshot::empty,
            decoded -> new LandmarkOverridesSnapshot(
                decoded.getVersion() == 0 ? LandmarkOverridesSnapshot.CURRENT_SCHEMA_VERSION : decoded.getVersion(),
                decoded.getByType() == null ? new java.util.LinkedHashMap<>() : decoded.getByType(),
                decoded.getDeletions() == null ? new java.util.ArrayList<>() : decoded.getDeletions()));
    }
}
