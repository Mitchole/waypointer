package com.waypointer.service;

import com.waypointer.service.PresetOverridesSnapshot.CategoryOverride;
import com.waypointer.service.PresetOverridesSnapshot.DeletedWaypoint;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import com.waypointer.util.Listeners;
import com.waypointer.util.Listeners.Subscription;
import java.util.ArrayList;
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
    private final Listeners listeners = new Listeners();

    @Inject
    public PresetOverrides() {}

    public static PresetOverrides forTesting() { return new PresetOverrides(); }

    public PresetOverridesSnapshot getSnapshot() { return snapshot; }
    public Subscription subscribe(Runnable r) { return listeners.subscribe(r); }

    // If original is null, append. Otherwise replace the entry matching the original tuple.
    public void upsertWaypoint(String category, Waypoint original, Waypoint updated)
    {
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
                    return;
                }
            }
        }
        co.getWaypoints().add(updated);
        listeners.fire();
    }

    public void deleteOverrideWaypoint(String category, Waypoint w)
    {
        CategoryOverride co = snapshot.getByCategory().get(category);
        if (co == null) return;
        co.getWaypoints().removeIf(x -> sameTuple(x, w));
        listeners.fire();
    }

    public void deleteBundledWaypoint(String category, String name, int x, int y, int plane)
    {
        snapshot.getDeletedWaypoints().add(new DeletedWaypoint(category, name, x, y, plane));
        listeners.fire();
    }

    public boolean addCategory(CategoryOverride co)
    {
        if (snapshot.getByCategory().containsKey(co.getCategory())) return false;
        for (CategoryOverride existing : snapshot.getAddedCategories())
        {
            if (Objects.equals(existing.getCategory(), co.getCategory())) return false;
        }
        snapshot.getAddedCategories().add(co);
        listeners.fire();
        return true;
    }

    public void deleteCategory(String name)
    {
        snapshot.getDeletedCategories().add(name);
        snapshot.getByCategory().remove(name);
        snapshot.getAddedCategories().removeIf(c -> Objects.equals(c.getCategory(), name));
        listeners.fire();
    }

    private static boolean sameTuple(Waypoint a, Waypoint b)
    {
        return a.getX() == b.getX() && a.getY() == b.getY() && a.getPlane() == b.getPlane()
            && Objects.equals(a.getName(), b.getName());
    }
}
