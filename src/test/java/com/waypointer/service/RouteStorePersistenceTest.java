package com.waypointer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.codec.RouteJsonCodec;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.model.route.RouteStep;
import java.time.Instant;
import java.util.UUID;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouteStorePersistenceTest
{
    private ConfigManager configManager;
    private RouteJsonCodec codec;
    private RouteStorePersistence persistence;

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

    @Before
    public void setUp()
    {
        configManager = mock(ConfigManager.class);
        codec = new RouteJsonCodec(buildGson());
        persistence = new RouteStorePersistence(configManager, codec);
    }

    private RouteLibrary oneRoute()
    {
        RouteLibrary lib = new RouteLibrary();
        Route r = new Route(UUID.randomUUID(), "R1", new java.util.ArrayList<>(), false, Instant.now(), 0);
        r.getSteps().add(RouteStep.manual("step"));
        lib.getRoutes().add(r);
        return lib;
    }

    @Test
    public void loadReturnsEmptyWhenNoConfigValue()
    {
        when(configManager.getConfiguration("waypointer", "routes")).thenReturn(null);
        assertEquals(0, persistence.load().getRoutes().size());
    }

    @Test
    public void saveWritesEncodedRoutesToConfig()
    {
        RouteLibrary lib = oneRoute();
        persistence.save(lib);
        verify(configManager).setConfiguration("waypointer", "routes", codec.encode(lib));
    }

    @Test
    public void saveAndLoadRoundTripThroughConfig()
    {
        RouteLibrary lib = oneRoute();
        when(configManager.getConfiguration("waypointer", "routes")).thenReturn(codec.encode(lib));
        RouteLibrary loaded = persistence.load();
        assertEquals(1, loaded.getRoutes().size());
        assertEquals("R1", loaded.getRoutes().get(0).getName());
    }

    @Test
    public void corruptValueFreezesSavesAndReturnsEmpty()
    {
        when(configManager.getConfiguration("waypointer", "routes")).thenReturn("{ not json");
        assertEquals(0, persistence.load().getRoutes().size());
        assertTrue(persistence.isRefusingSaves());
        persistence.save(oneRoute());
        verify(configManager, never()).setConfiguration(anyString(), anyString(), any());
    }

    @Test
    public void clearUnsetsConfigAndLiftsFreeze()
    {
        when(configManager.getConfiguration("waypointer", "routes")).thenReturn("{ bad");
        persistence.load();
        assertTrue(persistence.isRefusingSaves());
        persistence.clear();
        verify(configManager).unsetConfiguration("waypointer", "routes");
        assertFalse(persistence.isRefusingSaves());
    }
}
