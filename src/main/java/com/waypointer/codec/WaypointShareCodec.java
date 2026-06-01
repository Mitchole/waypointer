package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import javax.inject.Inject;

/**
 * Encodes/decodes share codes:
 *   WP1:<base64(gzip(payload))>      - single waypoint + its category
 *   WPL1:<base64(gzip(payload))>     - full library
 *
 * Soft cap: gunzipped payload must be < 1 MiB. Defends against gzip bombs.
 */
public class WaypointShareCodec
{
    public static final String SINGLE_MAGIC = "WP1:";
    public static final String LIBRARY_MAGIC = "WPL1:";

    private final Gson gson;
    private final LibraryJsonCodec libraryJsonCodec;

    @Inject
    public WaypointShareCodec(Gson gson, LibraryJsonCodec libraryJsonCodec)
    {
        this.gson = gson;
        this.libraryJsonCodec = libraryJsonCodec;
    }

    public SingleResult decodeSingle(String input)
    {
        String body = ShareCodecSupport.stripMagic(input, SINGLE_MAGIC, MalformedCodeException::new);
        String json = ShareCodecSupport.ungzipBase64(body, MalformedCodeException::new);
        JsonObject obj = parseObject(json);
        Waypoint w = gson.fromJson(obj.get("waypoint"), Waypoint.class);
        Category c = gson.fromJson(obj.get("category"), Category.class);
        if (w == null || c == null) throw new MalformedCodeException("Missing waypoint or category");
        return new SingleResult(w, c);
    }

    public String encodeLibrary(Library lib)
    {
        JsonObject obj = gson.toJsonTree(lib).getAsJsonObject();
        return LIBRARY_MAGIC + ShareCodecSupport.gzipBase64(obj.toString());
    }

    public Library decodeLibrary(String input)
    {
        String trimmed = input.trim();
        if (trimmed.startsWith(SINGLE_MAGIC))
        {
            // Caller used the wrong method - wrap a single result as a library.
            SingleResult sr = decodeSingle(trimmed);
            Library lib = new Library();
            lib.getCategories().add(sr.category);
            lib.getWaypoints().add(sr.waypoint);
            return lib;
        }
        String body = ShareCodecSupport.stripMagic(input, LIBRARY_MAGIC, MalformedCodeException::new);
        String json = ShareCodecSupport.ungzipBase64(body, MalformedCodeException::new);
        // Share the file-load decode contract: schema-version guard, migrator, and field defaults.
        try
        {
            return libraryJsonCodec.decode(json);
        }
        catch (LibraryJsonCodec.UnsupportedSchemaException e)
        {
            throw new MalformedCodeException("Share code is from a newer version of the plugin");
        }
        catch (LibraryJsonCodec.MalformedLibraryException e)
        {
            throw new MalformedCodeException("Bad library JSON inside share code");
        }
    }

    // ---- helpers ----

    private static JsonObject parseObject(String json)
    {
        return JsonDecodeSupport.parseObject(json, e -> new MalformedCodeException("Bad JSON inside share code"));
    }

    public static class MalformedCodeException extends RuntimeException
    {
        public MalformedCodeException(String msg) { super(msg); }
    }

    public static final class SingleResult
    {
        public final Waypoint waypoint;
        public final Category category;
        public SingleResult(Waypoint w, Category c) { this.waypoint = w; this.category = c; }
    }
}
