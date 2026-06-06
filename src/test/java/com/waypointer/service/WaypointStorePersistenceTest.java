package com.waypointer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import java.time.Instant;
import java.util.UUID;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WaypointStorePersistenceTest
{
    private ConfigManager configManager;
    private LibraryJsonCodec codec;
    private WaypointStorePersistence persistence;

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
        codec = new LibraryJsonCodec(buildGson());
        persistence = new WaypointStorePersistence(configManager, codec);
    }

    private Library oneCategory()
    {
        Library lib = new Library();
        lib.getCategories().add(new Category(UUID.randomUUID(), "Mining", 1, false, null, false));
        return lib;
    }

    @Test
    public void loadReturnsEmptyWhenNoConfigValue()
    {
        when(configManager.getRSProfileConfiguration("waypointer", "library")).thenReturn(null);
        Library lib = persistence.load();
        assertNotNull(lib);
        assertTrue(lib.getWaypoints().isEmpty());
    }

    @Test
    public void saveWritesEncodedLibraryToRsProfileConfig()
    {
        Library lib = oneCategory();
        persistence.save(lib);
        verify(configManager).setRSProfileConfiguration("waypointer", "library", codec.encode(lib));
    }

    @Test
    public void saveAndLoadRoundTripThroughConfig()
    {
        Library lib = oneCategory();
        when(configManager.getRSProfileConfiguration("waypointer", "library"))
            .thenReturn(codec.encode(lib));
        Library loaded = persistence.load();
        assertTrue(loaded.getCategories().stream().anyMatch(c -> "Mining".equals(c.getName())));
    }

    @Test
    public void corruptValueFreezesSavesAndReturnsEmpty()
    {
        when(configManager.getRSProfileConfiguration("waypointer", "library"))
            .thenReturn("{ not valid json");
        Library lib = persistence.load();
        assertTrue(lib.getWaypoints().isEmpty());
        assertTrue(persistence.isRefusingSaves());
        persistence.save(new Library());
        verify(configManager, never()).setRSProfileConfiguration(anyString(), anyString(), any());
    }

    @Test
    public void clearUnsetsConfigAndLiftsFreeze()
    {
        when(configManager.getRSProfileConfiguration("waypointer", "library"))
            .thenReturn("{ bad");
        persistence.load();
        assertTrue(persistence.isRefusingSaves());
        persistence.clear();
        verify(configManager).unsetRSProfileConfiguration("waypointer", "library");
        assertFalse(persistence.isRefusingSaves());
    }
}
