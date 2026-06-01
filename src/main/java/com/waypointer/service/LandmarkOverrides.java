package com.waypointer.service;

import com.waypointer.service.LandmarkOverridesSnapshot.DeletedEntry;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
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

// In-memory working set of dev-mode landmark overrides with pub/sub on mutation.
// BboxIndex and the dev tab editors subscribe to react to edits.
@Slf4j
@Singleton
public class LandmarkOverrides
{
    private LandmarkOverridesSnapshot snapshot = LandmarkOverridesSnapshot.empty();
    private final Listeners listeners = new Listeners();
    private LandmarkOverridesSnapshot undoBuffer = null;
    private final OverridePersistence persistence;
    private final com.waypointer.codec.LandmarkOverridesCodec codec;
    private volatile boolean dirty = false;
    private final java.util.concurrent.ScheduledExecutorService scheduler;

    @Inject
    public LandmarkOverrides(com.waypointer.codec.LandmarkOverridesCodec codec)
    {
        this(net.runelite.client.RuneLite.RUNELITE_DIR.toPath().resolve("waypointer"), codec,
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-landmark-overrides"); t.setDaemon(true); return t; }));
        loadFromDisk();
    }

    LandmarkOverrides(java.nio.file.Path dir, com.waypointer.codec.LandmarkOverridesCodec codec,
        java.util.concurrent.ScheduledExecutorService scheduler)
    {
        this.persistence = new OverridePersistence(dir, "landmark-overrides.json");
        this.codec = codec;
        this.scheduler = scheduler;
    }

    public static LandmarkOverrides forTesting()
    {
        try
        {
            return forTesting(java.nio.file.Files.createTempDirectory("lo-mem"),
                new com.waypointer.codec.LandmarkOverridesCodec(new com.google.gson.Gson()));
        }
        catch (java.io.IOException e) { throw new RuntimeException(e); }
    }

    public static LandmarkOverrides forTesting(java.nio.file.Path dir,
        com.waypointer.codec.LandmarkOverridesCodec codec)
    {
        return new LandmarkOverrides(dir, codec,
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-landmark-overrides-test"); t.setDaemon(true); return t; }));
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

    public LandmarkOverridesSnapshot getSnapshot() { return snapshot; }

    public Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public void addEntry(String type, Entry e)
    {
        undoBuffer = deepCopy(snapshot);
        TypeOverride t = snapshot.getByType().computeIfAbsent(type,
            k -> new TypeOverride(new ArrayList<>()));
        t.getEntries().add(e);
        listeners.fire();
        scheduleSave();
    }

    public void replaceEntry(String type, Entry original, Entry updated)
    {
        undoBuffer = deepCopy(snapshot);
        TypeOverride t = snapshot.getByType().get(type);
        if (t == null)
        {
            TypeOverride created = new TypeOverride(new ArrayList<>());
            created.getEntries().add(updated);
            snapshot.getByType().put(type, created);
            listeners.fire();
            scheduleSave();
            return;
        }
        for (int i = 0; i < t.getEntries().size(); i++)
        {
            if (sameTuple(t.getEntries().get(i), original))
            {
                t.getEntries().set(i, updated);
                listeners.fire();
                scheduleSave();
                return;
            }
        }
        // Original is from bundled, not in override. Record a delete-then-add pair.
        snapshot.getDeletions().add(new DeletedEntry(type, original.getName(),
            original.getX1(), original.getY1(), original.getX2(), original.getY2(), original.getPlane()));
        TypeOverride tt = snapshot.getByType().computeIfAbsent(type,
            k -> new TypeOverride(new ArrayList<>()));
        tt.getEntries().add(updated);
        listeners.fire();
        scheduleSave();
    }

    public void deleteOverrideEntry(String type, Entry e)
    {
        undoBuffer = deepCopy(snapshot);
        TypeOverride t = snapshot.getByType().get(type);
        if (t == null) return;
        t.getEntries().removeIf(x -> sameTuple(x, e));
        listeners.fire();
        scheduleSave();
    }

    public void deleteBundledEntry(String type, String name, int x1, int y1, int x2, int y2, int plane)
    {
        undoBuffer = deepCopy(snapshot);
        snapshot.getDeletions().add(new DeletedEntry(type, name, x1, y1, x2, y2, plane));
        listeners.fire();
        scheduleSave();
    }

    /**
     * Deletes an entry by tuple regardless of provenance: if it matches an override-added entry
     * it is removed from that type override; otherwise it is recorded as a bundled deletion. The
     * dev editor uses this because a listed row may be either a bundled entry or one the user
     * added.
     */
    public void deleteEntry(String type, String name, int x1, int y1, int x2, int y2, int plane)
    {
        undoBuffer = deepCopy(snapshot);
        TypeOverride t = snapshot.getByType().get(type);
        if (t != null && t.getEntries().removeIf(x ->
            x.getX1() == x1 && x.getY1() == y1 && x.getX2() == x2 && x.getY2() == y2
                && x.getPlane() == plane && Objects.equals(x.getName(), name)))
        {
            listeners.fire();
            scheduleSave();
            return;
        }
        snapshot.getDeletions().add(new DeletedEntry(type, name, x1, y1, x2, y2, plane));
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

    private static LandmarkOverridesSnapshot deepCopy(LandmarkOverridesSnapshot src)
    {
        Map<String, TypeOverride> by = new LinkedHashMap<>();
        for (Map.Entry<String, TypeOverride> e : src.getByType().entrySet())
        {
            List<Entry> entries = new ArrayList<>();
            for (Entry x : e.getValue().getEntries())
            {
                entries.add(new Entry(x.getName(), x.getX1(), x.getY1(), x.getX2(), x.getY2(), x.getPlane()));
            }
            by.put(e.getKey(), new TypeOverride(entries));
        }
        List<DeletedEntry> dels = new ArrayList<>();
        for (DeletedEntry d : src.getDeletions())
        {
            dels.add(new DeletedEntry(d.getType(), d.getName(),
                d.getX1(), d.getY1(), d.getX2(), d.getY2(), d.getPlane()));
        }
        return new LandmarkOverridesSnapshot(src.getVersion(), by, dels);
    }

    private static boolean sameTuple(Entry a, Entry b)
    {
        return a.getX1() == b.getX1() && a.getY1() == b.getY1()
            && a.getX2() == b.getX2() && a.getY2() == b.getY2()
            && a.getPlane() == b.getPlane()
            && Objects.equals(a.getName(), b.getName());
    }
}
