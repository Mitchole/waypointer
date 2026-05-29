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

    @Inject
    public LandmarkOverrides() {}

    public static LandmarkOverrides forTesting() { return new LandmarkOverrides(); }

    public LandmarkOverridesSnapshot getSnapshot() { return snapshot; }

    public Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public void addEntry(String type, Entry e)
    {
        undoBuffer = deepCopy(snapshot);
        TypeOverride t = snapshot.getByType().computeIfAbsent(type,
            k -> new TypeOverride(new ArrayList<>()));
        t.getEntries().add(e);
        listeners.fire();
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
            return;
        }
        for (int i = 0; i < t.getEntries().size(); i++)
        {
            if (sameTuple(t.getEntries().get(i), original))
            {
                t.getEntries().set(i, updated);
                listeners.fire();
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
    }

    public void deleteOverrideEntry(String type, Entry e)
    {
        undoBuffer = deepCopy(snapshot);
        TypeOverride t = snapshot.getByType().get(type);
        if (t == null) return;
        t.getEntries().removeIf(x -> sameTuple(x, e));
        listeners.fire();
    }

    public void deleteBundledEntry(String type, String name, int x1, int y1, int x2, int y2, int plane)
    {
        undoBuffer = deepCopy(snapshot);
        snapshot.getDeletions().add(new DeletedEntry(type, name, x1, y1, x2, y2, plane));
        listeners.fire();
    }

    public boolean undoLast()
    {
        if (undoBuffer == null) return false;
        snapshot = undoBuffer;
        undoBuffer = null;
        listeners.fire();
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
