package com.waypointer.service;

import com.waypointer.codec.LandmarkOverridesCodec;
import com.waypointer.service.LandmarkOverridesSnapshot.DeletedEntry;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

// In-memory working set of dev-mode landmark overrides with pub/sub on mutation.
// BboxIndex and the dev tab editors subscribe to react to edits. Scaffolding (persistence,
// debounce, undo, listeners) lives in OverridesStore.
@Singleton
public class LandmarkOverrides extends OverridesStore<LandmarkOverridesSnapshot>
{
    @Inject
    public LandmarkOverrides(LandmarkOverridesCodec codec)
    {
        this(RuneLite.RUNELITE_DIR.toPath().resolve("waypointer"), codec,
            Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-landmark-overrides"); t.setDaemon(true); return t; }));
        loadFromDisk();
    }

    LandmarkOverrides(Path dir, LandmarkOverridesCodec codec, ScheduledExecutorService scheduler)
    {
        super(dir, "landmark-overrides.json", codec, scheduler, LandmarkOverridesSnapshot.empty());
    }

    public static LandmarkOverrides forTesting(Gson gson)
    {
        try
        {
            return forTesting(Files.createTempDirectory("lo-mem"), new LandmarkOverridesCodec(gson));
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public static LandmarkOverrides forTesting(Path dir, LandmarkOverridesCodec codec)
    {
        return new LandmarkOverrides(dir, codec,
            Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "waypointer-landmark-overrides-test"); t.setDaemon(true); return t; }));
    }

    public void addEntry(String type, Entry e)
    {
        beginMutation();
        TypeOverride t = snapshot.getByType().computeIfAbsent(type,
            k -> new TypeOverride(new ArrayList<>()));
        t.getEntries().add(e);
        fire();
        scheduleSave();
    }

    public void replaceEntry(String type, Entry original, Entry updated)
    {
        beginMutation();
        TypeOverride t = snapshot.getByType().get(type);
        if (t == null)
        {
            TypeOverride created = new TypeOverride(new ArrayList<>());
            created.getEntries().add(updated);
            snapshot.getByType().put(type, created);
            fire();
            scheduleSave();
            return;
        }
        for (int i = 0; i < t.getEntries().size(); i++)
        {
            if (sameTuple(t.getEntries().get(i), original))
            {
                t.getEntries().set(i, updated);
                fire();
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
        fire();
        scheduleSave();
    }

    public void deleteOverrideEntry(String type, Entry e)
    {
        beginMutation();
        TypeOverride t = snapshot.getByType().get(type);
        if (t == null) return;
        t.getEntries().removeIf(x -> sameTuple(x, e));
        fire();
        scheduleSave();
    }

    public void deleteBundledEntry(String type, String name, int x1, int y1, int x2, int y2, int plane)
    {
        beginMutation();
        if (!deletionExists(type, name, x1, y1, x2, y2, plane))
        {
            snapshot.getDeletions().add(new DeletedEntry(type, name, x1, y1, x2, y2, plane));
        }
        fire();
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
        beginMutation();
        TypeOverride t = snapshot.getByType().get(type);
        if (t != null && t.getEntries().removeIf(x ->
            x.getX1() == x1 && x.getY1() == y1 && x.getX2() == x2 && x.getY2() == y2
                && x.getPlane() == plane && Objects.equals(x.getName(), name)))
        {
            fire();
            scheduleSave();
            return;
        }
        if (!deletionExists(type, name, x1, y1, x2, y2, plane))
        {
            snapshot.getDeletions().add(new DeletedEntry(type, name, x1, y1, x2, y2, plane));
        }
        fire();
        scheduleSave();
    }

    private boolean deletionExists(String type, String name, int x1, int y1, int x2, int y2, int plane)
    {
        for (DeletedEntry d : snapshot.getDeletions())
        {
            if (d.getX1() == x1 && d.getY1() == y1 && d.getX2() == x2 && d.getY2() == y2
                && d.getPlane() == plane && d.getType().equals(type)
                && Objects.equals(d.getName(), name)) return true;
        }
        return false;
    }

    @Override
    protected void afterDecode(LandmarkOverridesSnapshot decoded)
    {
        dedupeDeletions(decoded);
    }

    // Drops duplicate deletion tuples (same type+name+bbox+plane), keeping first occurrence.
    // Cleans files written before the delete paths guarded against duplicates.
    private static void dedupeDeletions(LandmarkOverridesSnapshot s)
    {
        Set<String> seen = new HashSet<>();
        s.getDeletions().removeIf(d -> !seen.add(
            d.getType() + "|" + d.getName() + "|" + d.getX1() + "," + d.getY1()
                + "," + d.getX2() + "," + d.getY2() + "," + d.getPlane()));
    }

    @Override
    protected LandmarkOverridesSnapshot deepCopy(LandmarkOverridesSnapshot src)
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
