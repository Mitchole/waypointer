package com.waypointer.codec;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Schema-version migration for {@link com.waypointer.model.route.RouteLibrary}.
 *
 * <p>v1 -> v2: route steps gained a {@code boxText} field (the in-game route-box text, distinct
 * from the sidebar {@code label}). Older files have only {@code label}; backfill {@code boxText}
 * from it so existing routes read identically in the box.
 */
public final class RouteMigrator
{
    private RouteMigrator() {}

    public static JsonObject migrate(JsonObject obj, int fromVersion)
    {
        if (fromVersion < 2)
        {
            backfillBoxText(obj);
            obj.addProperty("schemaVersion", 2);
        }
        return obj;
    }

    private static void backfillBoxText(JsonObject obj)
    {
        if (!obj.has("routes") || !obj.get("routes").isJsonArray()) return;
        for (JsonElement routeEl : obj.getAsJsonArray("routes"))
        {
            if (!routeEl.isJsonObject()) continue;
            JsonObject route = routeEl.getAsJsonObject();
            if (!route.has("steps") || !route.get("steps").isJsonArray()) continue;
            for (JsonElement stepEl : route.getAsJsonArray("steps"))
            {
                if (!stepEl.isJsonObject()) continue;
                JsonObject step = stepEl.getAsJsonObject();
                if (!step.has("boxText") && step.has("label"))
                {
                    step.add("boxText", step.get("label"));
                }
            }
        }
    }
}
