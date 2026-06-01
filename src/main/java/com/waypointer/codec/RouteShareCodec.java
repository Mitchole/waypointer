package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.waypointer.model.route.Route;
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

    private final Gson gson;

    @Inject
    public RouteShareCodec(Gson gson) { this.gson = gson; }

    public String encodeRoute(Route route)
    {
        return ROUTE_MAGIC + ShareCodecSupport.gzipBase64(gson.toJson(route));
    }

    public Route decodeRoute(String input)
    {
        String body = ShareCodecSupport.stripMagic(input, ROUTE_MAGIC, MalformedCodeException::new);
        String json = ShareCodecSupport.ungzipBase64(body, MalformedCodeException::new);
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

    public static class MalformedCodeException extends RuntimeException
    {
        public MalformedCodeException(String msg) { super(msg); }
    }
}
