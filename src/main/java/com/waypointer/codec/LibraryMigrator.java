package com.waypointer.codec;

import com.google.gson.JsonObject;
import com.waypointer.model.Library;

// JSON-level upgrades from older library schema versions to Library.CURRENT_SCHEMA_VERSION.
// Currently a passthrough; no v0/v1 -> v2 transformations are needed. Kept so LibraryJsonCodec
// has a stable hook for future migrations.
public final class LibraryMigrator
{
    private LibraryMigrator() {}

    public static JsonObject migrate(JsonObject obj, int fromVersion)
    {
        return obj;
    }
}
