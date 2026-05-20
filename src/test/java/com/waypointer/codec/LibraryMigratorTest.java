package com.waypointer.codec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.assertSame;

public class LibraryMigratorTest
{
    @Test
    public void migratorIsPassthroughForCurrentVersion()
    {
        @SuppressWarnings("deprecation")
        JsonObject v2 = new JsonParser().parse("{ \"schemaVersion\": 2, \"categories\": [], \"waypoints\": [] }").getAsJsonObject();
        assertSame(v2, LibraryMigrator.migrate(v2, 2));
    }

    @Test
    public void migratorIsPassthroughForOldVersions()
    {
        @SuppressWarnings("deprecation")
        JsonObject v1 = new JsonParser().parse("{ \"schemaVersion\": 1, \"categories\": [], \"waypoints\": [] }").getAsJsonObject();
        assertSame(v1, LibraryMigrator.migrate(v1, 1));
    }
}
