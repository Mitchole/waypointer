package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
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
    static final String SINGLE_MAGIC = "WP1:";
    static final String LIBRARY_MAGIC = "WPL1:";
    static final int MAX_INFLATED_BYTES = 1024 * 1024;

    private final Gson gson;

    @Inject
    public WaypointShareCodec(Gson gson) { this.gson = gson; }

    public String encodeSingle(Waypoint w, Category c)
    {
        JsonObject obj = new JsonObject();
        obj.add("waypoint", gson.toJsonTree(w));
        obj.add("category", gson.toJsonTree(c));
        return SINGLE_MAGIC + gzipBase64(obj.toString());
    }

    public SingleResult decodeSingle(String input)
    {
        String body = stripMagic(input, SINGLE_MAGIC);
        String json = ungzipBase64(body);
        JsonObject obj = parseObject(json);
        Waypoint w = gson.fromJson(obj.get("waypoint"), Waypoint.class);
        Category c = gson.fromJson(obj.get("category"), Category.class);
        if (w == null || c == null) throw new MalformedCodeException("Missing waypoint or category");
        return new SingleResult(w, c);
    }

    public String encodeLibrary(Library lib)
    {
        JsonObject obj = gson.toJsonTree(lib).getAsJsonObject();
        return LIBRARY_MAGIC + gzipBase64(obj.toString());
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
        String body = stripMagic(input, LIBRARY_MAGIC);
        String json = ungzipBase64(body);
        Library lib = gson.fromJson(json, Library.class);
        if (lib == null) throw new MalformedCodeException("Library decoded as null");
        if (lib.getCategories() == null) lib.setCategories(new java.util.ArrayList<>());
        if (lib.getWaypoints() == null) lib.setWaypoints(new java.util.ArrayList<>());
        return lib;
    }

    // ---- helpers ----

    private static String stripMagic(String input, String magic)
    {
        String trimmed = input.trim();
        if (!trimmed.startsWith(magic))
        {
            throw new MalformedCodeException("Expected magic " + magic + " at start of code");
        }
        return trimmed.substring(magic.length());
    }

    private static String gzipBase64(String input)
    {
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(baos))
            {
                gz.write(input.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getEncoder().withoutPadding().encodeToString(baos.toByteArray());
        }
        catch (IOException e)
        {
            throw new RuntimeException("gzip+base64 encode failed", e);
        }
    }

    private static String ungzipBase64(String input)
    {
        byte[] decoded;
        try { decoded = Base64.getDecoder().decode(input); }
        catch (IllegalArgumentException e) { throw new MalformedCodeException("Bad base64"); }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(decoded)))
        {
            byte[] buf = new byte[4096];
            int n;
            while ((n = gz.read(buf)) > 0)
            {
                out.write(buf, 0, n);
                if (out.size() > MAX_INFLATED_BYTES)
                {
                    throw new MalformedCodeException("Share payload exceeds 1 MiB cap");
                }
            }
        }
        catch (IOException e) { throw new MalformedCodeException("gzip decode failed: " + e.getMessage()); }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static JsonObject parseObject(String json)
    {
        try
        {
            @SuppressWarnings("deprecation")
            JsonObject o = new JsonParser().parse(json).getAsJsonObject();
            return o;
        }
        catch (JsonParseException | IllegalStateException e)
        {
            throw new MalformedCodeException("Bad JSON inside share code");
        }
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
