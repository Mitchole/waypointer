package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.waypointer.model.route.Route;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.inject.Inject;

/**
 * Encodes/decodes a single route share code:
 *   RT1:&lt;base64(gzip(json))&gt;
 *
 * Soft cap: gunzipped payload must be &lt; 1 MiB. Mirrors {@link WaypointShareCodec}.
 */
public class RouteShareCodec
{
    public static final String ROUTE_MAGIC = "RT1:";
    static final int MAX_INFLATED_BYTES = 1024 * 1024;

    private final Gson gson;

    @Inject
    public RouteShareCodec(Gson gson) { this.gson = gson; }

    public String encodeRoute(Route route)
    {
        return ROUTE_MAGIC + gzipBase64(gson.toJson(route));
    }

    public Route decodeRoute(String input)
    {
        String body = stripMagic(input);
        String json = ungzipBase64(body);
        final Route route;
        try
        {
            route = gson.fromJson(json, Route.class);
        }
        catch (JsonParseException e)
        {
            throw new MalformedCodeException("Bad JSON inside route code");
        }
        if (route == null || route.getName() == null)
        {
            throw new MalformedCodeException("Route decoded as null or nameless");
        }
        if (route.getSteps() == null) route.setSteps(new java.util.ArrayList<>());
        return route;
    }

    private static String stripMagic(String input)
    {
        String trimmed = input.trim();
        if (!trimmed.startsWith(ROUTE_MAGIC))
        {
            throw new MalformedCodeException("Expected magic " + ROUTE_MAGIC + " at start of code");
        }
        return trimmed.substring(ROUTE_MAGIC.length());
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

    public static class MalformedCodeException extends RuntimeException
    {
        public MalformedCodeException(String msg) { super(msg); }
    }
}
