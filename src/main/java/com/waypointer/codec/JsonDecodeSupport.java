package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Shared JSON-decode skeleton for the versioned library codecs. Holds the one tolerated
 * {@code new JsonParser()} deprecation so the per-entity codecs stay clean. Each codec keeps
 * its own exception types and supplies them as factories, preserving its public catch contract.
 */
final class JsonDecodeSupport
{
    private JsonDecodeSupport() {}

    /** Migration seam matching {@code LibraryMigrator}/{@code RouteMigrator}'s static signature. */
    @FunctionalInterface
    interface Migrator
    {
        JsonObject migrate(JsonObject obj, int fromVersion);
    }

    /**
     * Parse JSON to an object, wrapping any parse failure via {@code onMalformed}. The single
     * tolerated deprecated {@code JsonParser} call lives here (RuneLite's Gson 2.8.5 predates
     * {@code JsonParser.parseString}).
     */
    static JsonObject parseObject(String json, Function<Throwable, RuntimeException> onMalformed)
    {
        try
        {
            @SuppressWarnings("deprecation")
            JsonObject parsed = new JsonParser().parse(json).getAsJsonObject();
            return parsed;
        }
        catch (JsonParseException | IllegalStateException e)
        {
            throw onMalformed.apply(e);
        }
    }

    /**
     * The shared parse -> version-guard -> migrate -> bind skeleton. Per-entity field defaults
     * stay in the caller (they differ per codec) and run on the returned object.
     */
    static <T> T decode(
        Gson gson,
        String json,
        int currentSchemaVersion,
        Migrator migrator,
        Class<T> type,
        Function<Throwable, RuntimeException> onMalformed,
        IntFunction<RuntimeException> onUnsupported)
    {
        JsonObject obj = parseObject(json, onMalformed);

        // Treat a missing schemaVersion as 0 (pre-versioned) so the migrator runs. A present-but-
        // non-numeric schemaVersion is a corrupt blob: route it through onMalformed rather than let
        // getAsInt() throw an uncaught UnsupportedOperationException / NumberFormatException.
        int version = 0;
        if (obj.has("schemaVersion"))
        {
            JsonElement sv = obj.get("schemaVersion");
            if (!sv.isJsonPrimitive() || !sv.getAsJsonPrimitive().isNumber())
            {
                throw onMalformed.apply(
                    new JsonParseException("schemaVersion is not a number: " + sv));
            }
            version = sv.getAsInt();
        }
        if (version > currentSchemaVersion)
        {
            throw onUnsupported.apply(version);
        }
        return gson.fromJson(migrator.migrate(obj, version), type);
    }
}
