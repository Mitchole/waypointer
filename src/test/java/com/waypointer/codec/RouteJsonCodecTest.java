package com.waypointer.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;

public class RouteJsonCodecTest
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

    private final RouteJsonCodec codec = new RouteJsonCodec(buildGson());

    private RouteLibrary sample()
    {
        Route r = new Route(UUID.randomUUID(), "Herb run",
            Arrays.asList(
                RouteStep.waypoint(WorldPointPacker.pack(3200, 3200, 0), "Bank"),
                RouteStep.manual("Withdraw seeds")),
            true, Instant.parse("2026-06-01T00:00:00Z"), 0);
        RouteLibrary lib = new RouteLibrary();
        lib.getRoutes().add(r);
        return lib;
    }

    @Test
    public void roundTripsRoutes()
    {
        RouteLibrary decoded = codec.decode(codec.encode(sample()));
        assertEquals(1, decoded.getRoutes().size());
        Route r = decoded.getRoutes().get(0);
        assertEquals("Herb run", r.getName());
        assertTrue(r.isRepeating());
        assertEquals(2, r.getSteps().size());
        assertEquals(StepType.WAYPOINT, r.getSteps().get(0).getType());
        assertEquals(StepType.MANUAL, r.getSteps().get(1).getType());
        assertEquals("Withdraw seeds", r.getSteps().get(1).getLabel());
    }

    @Test
    public void stepBoxTextRoundTrips()
    {
        RouteStep step = RouteStep.manual("Bank");
        step.setBoxText("Withdraw 5 ranarr seeds");
        Route route = new Route(UUID.randomUUID(), "Herb run",
            Arrays.asList(step), false, Instant.parse("2026-06-01T00:00:00Z"), 0);
        RouteLibrary lib = new RouteLibrary();
        lib.getRoutes().add(route);

        RouteLibrary decoded = codec.decode(codec.encode(lib));
        assertEquals("Withdraw 5 ranarr seeds",
            decoded.getRoutes().get(0).getSteps().get(0).getBoxText());
    }

    @Test
    public void missingFieldsDecodeToEmptyDefaults()
    {
        RouteLibrary lib = codec.decode("{}");
        assertEquals(0, lib.getRoutes().size());
        assertEquals(RouteLibrary.CURRENT_SCHEMA_VERSION, lib.getSchemaVersion());
    }

    @Test(expected = RouteJsonCodec.UnsupportedSchemaException.class)
    public void newerSchemaIsRejected()
    {
        String json = "{\"schemaVersion\":999,\"routes\":[]}";
        codec.decode(json);
    }

    @Test(expected = RouteJsonCodec.MalformedRouteException.class)
    public void garbageJsonThrowsMalformed()
    {
        codec.decode("not json");
    }

    @Test
    public void dropsRouteWithNullName()
    {
        String json = "{\"schemaVersion\":2,\"routes\":["
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Keep\",\"steps\":[]},"
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"steps\":[]}"
            + "]}";

        RouteLibrary lib = codec.decode(json);

        assertEquals(1, lib.getRoutes().size());
        assertEquals("Keep", lib.getRoutes().get(0).getName());
    }

    @Test
    public void dropsStepsMissingBothLabelAndBoxText()
    {
        String json = "{\"schemaVersion\":2,\"routes\":[{"
            + "\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"R\",\"steps\":["
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"MANUAL\",\"label\":\"Good\"},"
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"type\":\"MANUAL\"}"
            + "]}]}";

        RouteLibrary lib = codec.decode(json);

        assertEquals(1, lib.getRoutes().size());
        assertEquals(1, lib.getRoutes().get(0).getSteps().size());
        assertEquals("Good", lib.getRoutes().get(0).getSteps().get(0).getLabel());
    }
}
