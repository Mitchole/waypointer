package com.waypointer.service;

import com.waypointer.codec.PresetOverridesCodec;
import com.waypointer.service.PresetOverridesSnapshot.CategoryOverride;
import com.waypointer.service.PresetOverridesSnapshot.DeletedWaypoint;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

// In-memory working set of dev-mode preset overrides with pub/sub on mutation.
// PresetCatalog and the dev tab editors subscribe to react to edits. Scaffolding
// (persistence, debounce, undo, listeners) lives in OverridesStore.
@Singleton
public class PresetOverrides extends OverridesStore<PresetOverridesSnapshot>
{
    @Inject
    public PresetOverrides(PresetOverridesCodec codec)
    {
        this(RuneLite.RUNELITE_DIR.toPath().resolve("waypointer"), codec,
            Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-preset-overrides"); t.setDaemon(true); return t; }));
        loadFromDisk();
    }

    PresetOverrides(Path dir, PresetOverridesCodec codec, ScheduledExecutorService scheduler)
    {
        super(dir, "preset-overrides.json", codec, scheduler, PresetOverridesSnapshot.empty());
    }

    public static PresetOverrides forTesting(Gson gson)
    {
        try
        {
            return forTesting(Files.createTempDirectory("po-mem"), new PresetOverridesCodec(gson));
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public static PresetOverrides forTesting(Path dir, PresetOverridesCodec codec)
    {
        return new PresetOverrides(dir, codec,
            Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-preset-overrides-test"); t.setDaemon(true); return t; }));
    }

    // If original is null, append. Otherwise replace the entry matching the original tuple.
    public void upsertWaypoint(String category, Waypoint original, Waypoint updated)
    {
        beginMutation();
        CategoryOverride co = snapshot.getByCategory().computeIfAbsent(category,
            k -> new CategoryOverride(k, null, null, new ArrayList<>()));
        if (original != null)
        {
            for (int i = 0; i < co.getWaypoints().size(); i++)
            {
                if (sameTuple(co.getWaypoints().get(i), original))
                {
                    co.getWaypoints().set(i, updated);
                    fire();
                    scheduleSave();
                    return;
                }
            }
        }
        co.getWaypoints().add(updated);
        fire();
        scheduleSave();
    }

    public void deleteOverrideWaypoint(String category, Waypoint w)
    {
        beginMutation();
        CategoryOverride co = snapshot.getByCategory().get(category);
        if (co == null) return;
        co.getWaypoints().removeIf(x -> sameTuple(x, w));
        fire();
        scheduleSave();
    }

    public void deleteBundledWaypoint(String category, String name, int x, int y, int plane)
    {
        beginMutation();
        snapshot.getDeletedWaypoints().add(new DeletedWaypoint(category, name, x, y, plane));
        fire();
        scheduleSave();
    }

    public boolean addCategory(CategoryOverride co)
    {
        beginMutation();
        if (snapshot.getByCategory().containsKey(co.getCategory())) return false;
        for (CategoryOverride existing : snapshot.getAddedCategories())
        {
            if (Objects.equals(existing.getCategory(), co.getCategory())) return false;
        }
        snapshot.getAddedCategories().add(co);
        fire();
        scheduleSave();
        return true;
    }

    public void deleteCategory(String name)
    {
        beginMutation();
        snapshot.getDeletedCategories().add(name);
        snapshot.getByCategory().remove(name);
        snapshot.getAddedCategories().removeIf(c -> Objects.equals(c.getCategory(), name));
        fire();
        scheduleSave();
    }

    @Override
    protected PresetOverridesSnapshot deepCopy(PresetOverridesSnapshot src)
    {
        Map<String, CategoryOverride> by = new LinkedHashMap<>();
        for (Map.Entry<String, CategoryOverride> e : src.getByCategory().entrySet())
        {
            by.put(e.getKey(), copyCat(e.getValue()));
        }
        List<CategoryOverride> added = new ArrayList<>();
        for (CategoryOverride c : src.getAddedCategories()) added.add(copyCat(c));
        List<DeletedWaypoint> dw = new ArrayList<>();
        for (DeletedWaypoint d : src.getDeletedWaypoints())
        {
            dw.add(new DeletedWaypoint(d.getCategory(), d.getName(), d.getX(), d.getY(), d.getPlane()));
        }
        return new PresetOverridesSnapshot(src.getVersion(), by, added,
            new ArrayList<>(src.getDeletedCategories()), dw);
    }

    private static CategoryOverride copyCat(CategoryOverride c)
    {
        List<Waypoint> wps = new ArrayList<>();
        for (Waypoint w : c.getWaypoints())
        {
            wps.add(new Waypoint(w.getName(), w.getDescription(), w.getX(), w.getY(), w.getPlane()));
        }
        return new CategoryOverride(c.getCategory(), c.getDescription(), c.getIcon(), wps);
    }

    private static boolean sameTuple(Waypoint a, Waypoint b)
    {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getPlane() == b.getPlane()
            && Objects.equals(a.getName(), b.getName());
    }
}
