package com.waypointer.codec;

import com.google.gson.Gson;
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
        Library lib = JsonDecodeSupport.decode(
            gson, json, Library.CURRENT_SCHEMA_VERSION, LibraryMigrator::migrate, Library.class,
            e -> new MalformedLibraryException("Failed to parse library JSON", e),
            v -> new UnsupportedSchemaException(
                "Library schemaVersion " + v + " is newer than supported "
                    + Library.CURRENT_SCHEMA_VERSION));
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
