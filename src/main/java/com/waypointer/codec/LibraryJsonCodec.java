package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.waypointer.model.Library;
import javax.inject.Inject;

/** Encodes/decodes a Library to/from JSON. Handles schema version checks and missing fields. */
public class LibraryJsonCodec
{
    private final Gson gson;

    @Inject
    public LibraryJsonCodec(Gson injectedGson)
    {
        this.gson = injectedGson;
    }

    public String encode(Library lib)
    {
        return gson.toJson(lib);
    }

    public Library decode(String json)
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
            throw new MalformedLibraryException("Failed to parse library JSON", e);
        }

        // Treat a missing schemaVersion as 0 (pre-versioned) so the migrator runs. Trusting
        // the current version here would silently skip every future migration on legacy files.
        int version = obj.has("schemaVersion") ? obj.get("schemaVersion").getAsInt() : 0;
        if (version > Library.CURRENT_SCHEMA_VERSION)
        {
            throw new UnsupportedSchemaException(
                "Library schemaVersion " + version + " is newer than supported "
                    + Library.CURRENT_SCHEMA_VERSION);
        }
        JsonObject migrated = LibraryMigrator.migrate(obj, version);

        Library lib = gson.fromJson(migrated, Library.class);
        // Defensive: Gson may leave nulls if fields were absent.
        if (lib.getCategories() == null) lib.setCategories(new java.util.ArrayList<>());
        if (lib.getWaypoints() == null) lib.setWaypoints(new java.util.ArrayList<>());
        if (lib.getSchemaVersion() == 0) lib.setSchemaVersion(Library.CURRENT_SCHEMA_VERSION);
        return lib;
    }

    public static class MalformedLibraryException extends RuntimeException
    {
        public MalformedLibraryException(String msg, Throwable cause) { super(msg, cause); }
    }

    public static class UnsupportedSchemaException extends RuntimeException
    {
        public UnsupportedSchemaException(String msg) { super(msg); }
    }
}
