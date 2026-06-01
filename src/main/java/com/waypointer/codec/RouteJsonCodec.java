package com.waypointer.codec;

import com.google.gson.Gson;
import com.waypointer.model.route.RouteLibrary;
import javax.inject.Inject;

/** Encodes/decodes a {@link RouteLibrary} to/from JSON with a schema-version check. */
public class RouteJsonCodec
{
    private final Gson gson;

    @Inject
    public RouteJsonCodec(Gson injectedGson)
    {
        this.gson = injectedGson;
    }

    public String encode(RouteLibrary lib)
    {
        return gson.toJson(lib);
    }

    public RouteLibrary decode(String json)
    {
        RouteLibrary lib = JsonDecodeSupport.decode(
            gson, json, RouteLibrary.CURRENT_SCHEMA_VERSION, RouteMigrator::migrate, RouteLibrary.class,
            e -> new MalformedRouteException("Failed to parse routes JSON", e),
            v -> new UnsupportedSchemaException(
                "Routes schemaVersion " + v + " is newer than supported "
                    + RouteLibrary.CURRENT_SCHEMA_VERSION));
        // Defensive: Gson may leave nulls if fields were absent.
        if (lib.getRoutes() == null) lib.setRoutes(new java.util.ArrayList<>());
        if (lib.getSchemaVersion() == 0) lib.setSchemaVersion(RouteLibrary.CURRENT_SCHEMA_VERSION);
        return lib;
    }

    public static class MalformedRouteException extends RuntimeException
    {
        public MalformedRouteException(String msg, Throwable cause) { super(msg, cause); }
    }

    public static class UnsupportedSchemaException extends RuntimeException
    {
        public UnsupportedSchemaException(String msg) { super(msg); }
    }
}
