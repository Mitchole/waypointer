package com.waypointer.codec;

import com.google.gson.JsonObject;
import com.waypointer.model.Library;

/**
 * Schema-version migration hook. Today every supported version maps straight through, so this
 * is an identity passthrough -- the {@code fromVersion} argument is unused on purpose. It exists
 * as the stable seam to slot real migration steps into when the on-disk schema next changes;
 * it is not dead code and is not live migration logic.
 */
public final class LibraryMigrator
{
    private LibraryMigrator() {}

    public static JsonObject migrate(JsonObject obj, int fromVersion)
    {
        return obj;
    }
}
