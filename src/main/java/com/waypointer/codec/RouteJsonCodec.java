package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
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
        final JsonObject obj;
        try
        {
            // Stuck with deprecated instance API: RuneLite's bundled Gson 2.8.5 predates
            // JsonParser.parseString.
            @SuppressWarnings("deprecation")
            JsonObject parsed = new JsonParser().parse(json).getAsJsonObject();
            obj = parsed;
        }
        catch (JsonParseException | IllegalStateException e)
        {
            throw new MalformedRouteException("Failed to parse routes JSON", e);
        }

        int version = obj.has("schemaVersion") ? obj.get("schemaVersion").getAsInt() : 0;
        if (version > RouteLibrary.CURRENT_SCHEMA_VERSION)
        {
            throw new UnsupportedSchemaException(
                "Routes schemaVersion " + version + " is newer than supported "
                    + RouteLibrary.CURRENT_SCHEMA_VERSION);
        }
        JsonObject migrated = RouteMigrator.migrate(obj, version);

        RouteLibrary lib = gson.fromJson(migrated, RouteLibrary.class);
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
