package com.waypointer.codec;

import com.google.gson.JsonObject;

/**
 * Schema-version migration seam for {@link com.waypointer.model.route.RouteLibrary}. Identity
 * passthrough today; exists so future on-disk schema changes have a stable place to live.
 */
public final class RouteMigrator
{
    private RouteMigrator() {}

    public static JsonObject migrate(JsonObject obj, int fromVersion)
    {
        return obj;
    }
}
