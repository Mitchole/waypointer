package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.function.Function;
import java.util.function.Supplier;

// Encode/decode contract for a single override-snapshot type. The shared decodeWithDefaults
// captures the recipe both override codecs use: empty input -> empty snapshot, parse failure
// -> empty snapshot, and a caller-supplied field-defaulting pass over the decoded object.
public interface SnapshotCodec<S>
{
    String encode(S snapshot);

    S decode(String json);

    static <S> S decodeWithDefaults(Gson gson, String json, Class<S> type,
        Supplier<S> empty, Function<S, S> applyDefaults)
    {
        if (json == null || json.isEmpty()) return empty.get();
        try
        {
            S decoded = gson.fromJson(json, type);
            if (decoded == null) return empty.get();
            return applyDefaults.apply(decoded);
        }
        catch (JsonParseException e)
        {
            return empty.get();
        }
    }
}
