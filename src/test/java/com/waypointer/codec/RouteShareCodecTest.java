package com.waypointer.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;

public class RouteShareCodecTest
{
    private static Gson buildGson()
    {
        return new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonSerializer<Instant>) (src, t, c) ->
                    new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonDeserializer<Instant>) (e, t, c) ->
                    Instant.parse(e.getAsString()))
            .create();
    }

    private final RouteShareCodec codec = new RouteShareCodec(buildGson());

    private Route sample()
    {
        return new Route(UUID.randomUUID(), "Herb run",
            Arrays.asList(
                RouteStep.waypoint(WorldPointPacker.pack(3200, 3200, 0), "Bank"),
                RouteStep.manual("Withdraw seeds")),
            true, Instant.parse("2026-06-01T00:00:00Z"), 0);
    }

    @Test
    public void encodeStartsWithMagic()
    {
        assertTrue(codec.encodeRoute(sample()).startsWith(RouteShareCodec.ROUTE_MAGIC));
    }

    @Test
    public void roundTripsRoute()
    {
        Route decoded = codec.decodeRoute(codec.encodeRoute(sample()));
        assertEquals("Herb run", decoded.getName());
        assertEquals(2, decoded.getSteps().size());
        assertEquals("Withdraw seeds", decoded.getSteps().get(1).getLabel());
        assertTrue(decoded.isRepeating());
    }

    @Test
    public void roundTripsStepBoxText()
    {
        RouteStep step = RouteStep.manual("Bank");
        step.setBoxText("Withdraw 5 ranarr seeds");
        Route route = new Route(UUID.randomUUID(), "Herb run",
            Arrays.asList(step), false, Instant.parse("2026-06-01T00:00:00Z"), 0);

        Route decoded = codec.decodeRoute(codec.encodeRoute(route));
        assertEquals("Withdraw 5 ranarr seeds", decoded.getSteps().get(0).getBoxText());
    }

    @Test(expected = RouteShareCodec.MalformedCodeException.class)
    public void wrongMagicThrows()
    {
        codec.decodeRoute("WP1:whatever");
    }

    @Test(expected = RouteShareCodec.MalformedCodeException.class)
    public void garbageBase64Throws()
    {
        codec.decodeRoute("RT1:!!!notbase64!!!");
    }

    private static String rt1(String innerJson) throws java.io.IOException
    {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gz = new java.util.zip.GZIPOutputStream(baos))
        {
            gz.write(innerJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return "RT1:" + java.util.Base64.getEncoder().withoutPadding()
            .encodeToString(baos.toByteArray());
    }

    @Test(expected = RouteShareCodec.MalformedCodeException.class)
    public void rejectsRouteWithNullId() throws Exception
    {
        codec.decodeRoute(rt1("{\"name\":\"Herb run\",\"steps\":[]}"));
    }

    @Test(expected = RouteShareCodec.MalformedCodeException.class)
    public void rejectsWrongTopLevelType() throws Exception
    {
        codec.decodeRoute(rt1("[]"));
    }

    @Test
    public void dropsInvalidStepsButKeepsRoute() throws Exception
    {
        String json = "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Herb run\",\"steps\":["
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"MANUAL\",\"label\":\"Good\"},"
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"MANUAL\"}"
            + "]}";

        Route decoded = codec.decodeRoute(rt1(json));

        assertEquals(1, decoded.getSteps().size());
        assertEquals("Good", decoded.getSteps().get(0).getLabel());
        assertNotNull("survivor must have non-null overlay text",
            decoded.getSteps().get(0).boxTextOrLabel());
    }
}
