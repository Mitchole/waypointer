package com.waypointer.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

public class RouteMigratorTest
{
    @SuppressWarnings("deprecation")
    private static JsonObject parse(String json)
    {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    @Test
    public void backfillsBoxTextFromLabelForV1Steps()
    {
        JsonObject v1 = parse(
            "{\"schemaVersion\":1,\"routes\":[{\"steps\":["
            + "{\"label\":\"Bank\"},"
            + "{\"label\":\"Withdraw seeds\"}"
            + "]}]}");

        JsonObject migrated = RouteMigrator.migrate(v1, 1);

        JsonObject route = migrated.getAsJsonArray("routes").get(0).getAsJsonObject();
        JsonObject step0 = route.getAsJsonArray("steps").get(0).getAsJsonObject();
        JsonObject step1 = route.getAsJsonArray("steps").get(1).getAsJsonObject();
        assertEquals("Bank", step0.get("boxText").getAsString());
        assertEquals("Withdraw seeds", step1.get("boxText").getAsString());
        assertEquals(2, migrated.get("schemaVersion").getAsInt());
    }

    @Test
    public void doesNotOverwriteExistingBoxText()
    {
        JsonObject v1 = parse(
            "{\"schemaVersion\":1,\"routes\":[{\"steps\":["
            + "{\"label\":\"Bank\",\"boxText\":\"Withdraw 5 seeds\"}"
            + "]}]}");

        JsonObject migrated = RouteMigrator.migrate(v1, 1);

        JsonObject step0 = migrated.getAsJsonArray("routes").get(0).getAsJsonObject()
            .getAsJsonArray("steps").get(0).getAsJsonObject();
        assertEquals("Withdraw 5 seeds", step0.get("boxText").getAsString());
    }

    @Test
    public void v2IsIdentityAndAddsNothing()
    {
        JsonObject v2 = parse(
            "{\"schemaVersion\":2,\"routes\":[{\"steps\":[{\"label\":\"Bank\"}]}]}");

        JsonObject migrated = RouteMigrator.migrate(v2, 2);

        JsonObject step0 = migrated.getAsJsonArray("routes").get(0).getAsJsonObject()
            .getAsJsonArray("steps").get(0).getAsJsonObject();
        assertFalse("v2 step must not be backfilled", step0.has("boxText"));
        assertEquals(2, migrated.get("schemaVersion").getAsInt());
    }
}
