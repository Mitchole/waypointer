package com.waypointer.preset;

import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import java.time.Instant;
import java.util.UUID;

/**
 * Builds a one-entry {@link Library} from a single preset waypoint, ready to feed through
 * {@code WaypointStore.importMerge}. importMerge matches the category by name, so a fresh
 * category id here is harmless: an existing same-named category absorbs the waypoint.
 */
public final class PresetImport
{
    private PresetImport()
    {
    }

    public static Library singleEntryLibrary(Preset preset, PresetWaypoint wp)
    {
        // sortOrder 0 is a placeholder; importMerge assigns the real sort order on import.
        Category category = new Category(
            UUID.randomUUID(), preset.getCategory(), 0, false, preset.getIcon(), true);

        int packed = WorldPointPacker.pack(wp.getX(), wp.getY(), wp.getPlane());
        Waypoint waypoint = new Waypoint(
            UUID.randomUUID(),
            wp.getName(),
            packed,
            category.getId(),
            null,
            wp.getDescription() == null ? "" : wp.getDescription(),
            Instant.now(),
            0);

        Library lib = new Library();
        lib.getCategories().add(category);
        lib.getWaypoints().add(waypoint);
        return lib;
    }
}
