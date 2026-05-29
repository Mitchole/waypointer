package com.waypointer.codec;

import com.google.gson.Gson;
import com.waypointer.service.LandmarkOverridesSnapshot;
import javax.inject.Inject;
import javax.inject.Singleton;

/** Gson-backed encode/decode for {@link LandmarkOverridesSnapshot}. */
@Singleton
public class LandmarkOverridesCodec
{
    private final Gson gson;

    @Inject
    public LandmarkOverridesCodec(Gson gson)
    {
        this.gson = gson;
    }

    public String encode(LandmarkOverridesSnapshot snapshot)
    {
        return gson.toJson(snapshot);
    }

    public LandmarkOverridesSnapshot decode(String json)
    {
        LandmarkOverridesSnapshot decoded = gson.fromJson(json, LandmarkOverridesSnapshot.class);
        if (decoded == null)
        {
            return LandmarkOverridesSnapshot.empty();
        }
        if (decoded.getByType() == null || decoded.getDeletions() == null)
        {
            return new LandmarkOverridesSnapshot(
                decoded.getVersion(),
                decoded.getByType() == null ? new java.util.LinkedHashMap<>() : decoded.getByType(),
                decoded.getDeletions() == null ? new java.util.ArrayList<>() : decoded.getDeletions());
        }
        return decoded;
    }
}
