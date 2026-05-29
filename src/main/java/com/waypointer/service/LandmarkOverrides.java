package com.waypointer.service;

import com.waypointer.service.LandmarkOverridesSnapshot.DeletedEntry;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
import com.waypointer.util.Listeners;
import com.waypointer.util.Listeners.Subscription;
import java.util.ArrayList;
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

    @Inject
    public LandmarkOverrides() {}

    public static LandmarkOverrides forTesting() { return new LandmarkOverrides(); }

    public LandmarkOverridesSnapshot getSnapshot() { return snapshot; }

    public Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    public void addEntry(String type, Entry e)
    {
        TypeOverride t = snapshot.getByType().computeIfAbsent(type,
            k -> new TypeOverride(new ArrayList<>()));
        t.getEntries().add(e);
        listeners.fire();
    }

    public void replaceEntry(String type, Entry original, Entry updated)
    {
        TypeOverride t = snapshot.getByType().get(type);
        if (t == null) { addEntry(type, updated); return; }
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
        deleteBundledEntry(type, original.getName(),
            original.getX1(), original.getY1(), original.getX2(), original.getY2(), original.getPlane());
        addEntry(type, updated);
    }

    public void deleteOverrideEntry(String type, Entry e)
    {
        TypeOverride t = snapshot.getByType().get(type);
        if (t == null) return;
        t.getEntries().removeIf(x -> sameTuple(x, e));
        listeners.fire();
    }

    public void deleteBundledEntry(String type, String name, int x1, int y1, int x2, int y2, int plane)
    {
        snapshot.getDeletions().add(new DeletedEntry(type, name, x1, y1, x2, y2, plane));
        listeners.fire();
    }

    private static boolean sameTuple(Entry a, Entry b)
    {
        return a.getX1() == b.getX1() && a.getY1() == b.getY1()
            && a.getX2() == b.getX2() && a.getY2() == b.getY2()
            && a.getPlane() == b.getPlane()
            && Objects.equals(a.getName(), b.getName());
    }
}
