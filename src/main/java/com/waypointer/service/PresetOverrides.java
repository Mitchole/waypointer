package com.waypointer.service;

import com.waypointer.service.PresetOverridesSnapshot.CategoryOverride;
import com.waypointer.service.PresetOverridesSnapshot.DeletedWaypoint;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import com.waypointer.util.Listeners;
import com.waypointer.util.Listeners.Subscription;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

// In-memory working set of dev-mode preset overrides with pub/sub on mutation.
// PresetCatalog and the dev tab editors subscribe to react to edits.
@Slf4j
@Singleton
public class PresetOverrides
{
    private PresetOverridesSnapshot snapshot = PresetOverridesSnapshot.empty();
    private PresetOverridesSnapshot undoBuffer = null;
    private final Listeners listeners = new Listeners();
    private final OverridePersistence persistence;
    private final com.waypointer.codec.PresetOverridesCodec codec;
    private volatile boolean dirty = false;
    private final java.util.concurrent.ScheduledExecutorService scheduler;

    @Inject
    public PresetOverrides(com.waypointer.codec.PresetOverridesCodec codec)
    {
        this(net.runelite.client.RuneLite.RUNELITE_DIR.toPath().resolve("waypointer"), codec,
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-preset-overrides"); t.setDaemon(true); return t; }));
        loadFromDisk();
    }

    PresetOverrides(java.nio.file.Path dir, com.waypointer.codec.PresetOverridesCodec codec,
        java.util.concurrent.ScheduledExecutorService scheduler)
    {
        this.persistence = new OverridePersistence(dir, "preset-overrides.json");
        this.codec = codec;
        this.scheduler = scheduler;
    }

    public static PresetOverrides forTesting()
    {
        try
        {
            return forTesting(java.nio.file.Files.createTempDirectory("po-mem"),
                new com.waypointer.codec.PresetOverridesCodec(new com.google.gson.Gson()));
        }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    public static PresetOverrides forTesting(java.nio.file.Path dir,
        com.waypointer.codec.PresetOverridesCodec codec)
    {
        return new PresetOverrides(dir, codec,
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-preset-overrides-test"); t.setDaemon(true); return t; }));
    }

    public void loadFromDisk()
    {
        snapshot = codec.decode(persistence.loadOrEmpty());
        listeners.fire();
    }

    public boolean flushBlocking()
    {
        return persistence.writeBlocking(codec.encode(snapshot));
    }

    private void scheduleSave()
    {
        if (dirty) return;
        dirty = true;
        scheduler.schedule(() -> {
            dirty = false;
            flushBlocking();
        }, 500, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public PresetOverridesSnapshot getSnapshot() { return snapshot; }
    public Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    // If original is null, append. Otherwise replace the entry matching the original tuple.
    public void upsertWaypoint(String category, Waypoint original, Waypoint updated)
    {
        undoBuffer = deepCopy(snapshot);
        CategoryOverride co = snapshot.getByCategory().computeIfAbsent(category,
            k -> new CategoryOverride(k, null, null, new ArrayList<>()));
        if (original != null)
        {
            for (int i = 0; i < co.getWaypoints().size(); i++)
            {
                if (sameTuple(co.getWaypoints().get(i), original))
                {
                    co.getWaypoints().set(i, updated);
                    listeners.fire();
                    scheduleSave();
                    return;
                }
            }
        }
        co.getWaypoints().add(updated);
        listeners.fire();
        scheduleSave();
    }

    public void deleteOverrideWaypoint(String category, Waypoint w)
    {
        undoBuffer = deepCopy(snapshot);
        CategoryOverride co = snapshot.getByCategory().get(category);
        if (co == null) return;
        co.getWaypoints().removeIf(x -> sameTuple(x, w));
        listeners.fire();
        scheduleSave();
    }

    public void deleteBundledWaypoint(String category, String name, int x, int y, int plane)
    {
        undoBuffer = deepCopy(snapshot);
        snapshot.getDeletedWaypoints().add(new DeletedWaypoint(category, name, x, y, plane));
        listeners.fire();
        scheduleSave();
    }

    public boolean addCategory(CategoryOverride co)
    {
        undoBuffer = deepCopy(snapshot);
        if (snapshot.getByCategory().containsKey(co.getCategory())) return false;
        for (CategoryOverride existing : snapshot.getAddedCategories())
        {
            if (Objects.equals(existing.getCategory(), co.getCategory())) return false;
        }
        snapshot.getAddedCategories().add(co);
        listeners.fire();
        scheduleSave();
        return true;
    }

    public void deleteCategory(String name)
    {
        undoBuffer = deepCopy(snapshot);
        snapshot.getDeletedCategories().add(name);
        snapshot.getByCategory().remove(name);
        snapshot.getAddedCategories().removeIf(c -> Objects.equals(c.getCategory(), name));
        listeners.fire();
        scheduleSave();
    }

    public boolean undoLast()
    {
        if (undoBuffer == null) return false;
        snapshot = undoBuffer;
        undoBuffer = null;
        listeners.fire();
        scheduleSave();
        return true;
    }

    private static PresetOverridesSnapshot deepCopy(PresetOverridesSnapshot src)
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
