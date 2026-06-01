package com.waypointer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.codec.RouteJsonCodec;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.model.route.RouteStep;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RouteStorePersistenceTest
{
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

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

    private RouteStorePersistence newPersistence() throws Exception
    {
        Path dir = tmp.newFolder("waypointer").toPath();
        return new RouteStorePersistence(dir, new RouteJsonCodec(buildGson()));
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
    public void savesAndReloads() throws Exception
    {
        RouteStorePersistence p = newPersistence();
        assertTrue(p.saveBlocking(oneRoute()));
        RouteLibrary loaded = p.loadOrEmpty();
        assertEquals(1, loaded.getRoutes().size());
        assertEquals("R1", loaded.getRoutes().get(0).getName());
    }

    @Test
    public void missingFileLoadsEmpty() throws Exception
    {
        RouteStorePersistence p = newPersistence();
        assertEquals(0, p.loadOrEmpty().getRoutes().size());
    }

    @Test
    public void writeRefreshesBackup() throws Exception
    {
        RouteStorePersistence p = newPersistence();
        p.saveBlocking(oneRoute());
        assertTrue(Files.exists(p.backupFile()));
    }

    @Test
    public void corruptPrimaryFallsBackToBackup() throws Exception
    {
        RouteStorePersistence p = newPersistence();
        p.saveBlocking(oneRoute());          // writes primary + backup
        Files.write(p.routesFile(), "{ not json".getBytes());
        RouteLibrary loaded = p.loadOrEmpty();
        assertEquals(1, loaded.getRoutes().size());   // recovered from backup
    }
}
