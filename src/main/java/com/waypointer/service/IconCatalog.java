package com.waypointer.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/** Loads icon-categories.json once and exposes it as an ordered list of named categories.
 *  Each category is a sorted, immutable list of sprite ids. Used by IconPickerDialog so the
 *  user sees a curated set rather than every SpriteID. Returns an empty list if the resource
 *  is missing or malformed; the picker then falls back to reflecting over SpriteID. */
@Singleton
@Slf4j
public final class IconCatalog
{
    private static final String RESOURCE = "/com/waypointer/icon-categories.json";

    private final List<Category> categories;

    @Inject
    public IconCatalog(Gson gson)
    {
        this.categories = Collections.unmodifiableList(load(gson));
    }

    public List<Category> getCategories() { return categories; }

    private static List<Category> load(Gson gson)
    {
        try (InputStream in = IconCatalog.class.getResourceAsStream(RESOURCE))
        {
            if (in == null)
            {
                log.warn("Icon catalog resource not found: {}", RESOURCE);
                return new ArrayList<>();
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8))
            {
                JsonObject root = gson.fromJson(r, JsonObject.class);
                if (root == null || !root.has("categories")) return new ArrayList<>();
                JsonArray cats = root.getAsJsonArray("categories");
                List<Category> out = new ArrayList<>(cats.size());
                for (JsonElement el : cats)
                {
                    if (!el.isJsonObject()) continue;
                    JsonObject co = el.getAsJsonObject();
                    String name = co.has("name") ? co.get("name").getAsString() : "";
                    if (name.isEmpty()) continue;
                    JsonArray icons = co.has("icons") ? co.getAsJsonArray("icons") : new JsonArray();
                    List<Integer> ids = new ArrayList<>(icons.size());
                    for (JsonElement ic : icons)
                    {
                        if (!ic.isJsonObject()) continue;
                        JsonObject io = ic.getAsJsonObject();
                        if (!io.has("id")) continue;
                        try { ids.add(io.get("id").getAsInt()); }
                        catch (Exception ignored) {}
                    }
                    Collections.sort(ids);
                    out.add(new Category(name, Collections.unmodifiableList(ids)));
                }
                return out;
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to load icon catalog; picker will fall back to all-sprites view.", e);
            return new ArrayList<>();
        }
    }

    public static final class Category
    {
        private final String name;
        private final List<Integer> spriteIds;

        Category(String name, List<Integer> spriteIds)
        {
            this.name = name;
            this.spriteIds = spriteIds;
        }

        public String getName() { return name; }
        public List<Integer> getSpriteIds() { return spriteIds; }
    }
}
