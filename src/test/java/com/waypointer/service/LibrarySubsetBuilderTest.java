package com.waypointer.service;

import com.waypointer.model.Category;
import com.waypointer.model.CategorySortMode;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LibrarySubsetBuilderTest
{
    private static Waypoint wp(UUID id, UUID catId, String name)
    {
        return new Waypoint(id, name, 1, catId, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);
    }

    @Test
    public void includesOnlySelectedWaypoints()
    {
        UUID cat = UUID.randomUUID();
        UUID keep = UUID.randomUUID();
        UUID drop = UUID.randomUUID();
        Library src = new Library();
        src.getCategories().add(new Category(cat, "Banks", 0, false, null, false));
        src.getWaypoints().add(wp(keep, cat, "GE"));
        src.getWaypoints().add(wp(drop, cat, "Falador"));

        Library out = LibrarySubsetBuilder.build(src,
            new HashSet<>(Collections.singletonList(keep)), Collections.emptySet());

        assertEquals(1, out.getWaypoints().size());
        assertEquals("GE", out.getWaypoints().get(0).getName());
    }

    @Test
    public void pullsInCategoryDefForReferencedCategory()
    {
        UUID cat = UUID.randomUUID();
        UUID wpId = UUID.randomUUID();
        Library src = new Library();
        src.getCategories().add(new Category(cat, "Banks", 3, false, 99, false));
        src.getWaypoints().add(wp(wpId, cat, "GE"));

        Library out = LibrarySubsetBuilder.build(src,
            new HashSet<>(Collections.singletonList(wpId)), Collections.emptySet());

        assertEquals(1, out.getCategories().size());
        assertEquals("Banks", out.getCategories().get(0).getName());
        assertEquals(Integer.valueOf(99), out.getCategories().get(0).getIconId());
    }

    @Test
    public void includesExplicitlyCheckedEmptyCategory()
    {
        UUID cat = UUID.randomUUID();
        Library src = new Library();
        src.getCategories().add(new Category(cat, "Empty", 0, false, null, false));

        Library out = LibrarySubsetBuilder.build(src,
            Collections.emptySet(), new HashSet<>(Collections.singletonList(cat)));

        assertEquals(1, out.getCategories().size());
        assertTrue(out.getWaypoints().isEmpty());
    }

    @Test
    public void includesUncategorizedSentinelWhenReferenced()
    {
        UUID unc = UUID.randomUUID();
        UUID wpId = UUID.randomUUID();
        Library src = new Library();
        src.getCategories().add(new Category(unc, "Uncategorized", 0, true, null, false));
        src.getWaypoints().add(wp(wpId, unc, "Loose"));

        Library out = LibrarySubsetBuilder.build(src,
            new HashSet<>(Collections.singletonList(wpId)), Collections.emptySet());

        assertEquals(1, out.getCategories().size());
        assertTrue(out.getCategories().get(0).isUncategorized());
    }

    @Test
    public void doesNotMutateSource()
    {
        UUID cat = UUID.randomUUID();
        UUID wpId = UUID.randomUUID();
        Library src = new Library();
        src.getCategories().add(new Category(cat, "Banks", 0, false, null, false));
        src.getWaypoints().add(wp(wpId, cat, "GE"));

        LibrarySubsetBuilder.build(src,
            new HashSet<>(Collections.singletonList(wpId)), Collections.emptySet());

        assertEquals(1, src.getWaypoints().size());
        assertEquals(1, src.getCategories().size());
    }

    @Test
    public void includesOnlyTheSelectedWaypointFromAMultiWaypointCategory()
    {
        UUID cat = UUID.randomUUID();
        UUID keep = UUID.randomUUID();
        UUID drop1 = UUID.randomUUID();
        UUID drop2 = UUID.randomUUID();
        Library src = new Library();
        src.getCategories().add(new Category(cat, "Banks", 0, false, null, false));
        src.getWaypoints().add(wp(keep, cat, "GE"));
        src.getWaypoints().add(wp(drop1, cat, "Falador"));
        src.getWaypoints().add(wp(drop2, cat, "Varrock"));

        Library out = LibrarySubsetBuilder.build(src,
            new HashSet<>(Collections.singletonList(keep)), Collections.emptySet());

        assertEquals(1, out.getWaypoints().size());
        assertEquals("GE", out.getWaypoints().get(0).getName());
        assertEquals(1, out.getCategories().size());
    }

    @Test
    public void preservesCategorySortMode()
    {
        UUID cat = UUID.randomUUID();
        UUID wpId = UUID.randomUUID();
        Library src = new Library();
        Category source = new Category(cat, "Banks", 0, false, null, false);
        source.setSortMode(CategorySortMode.NAME);
        src.getCategories().add(source);
        src.getWaypoints().add(wp(wpId, cat, "GE"));

        Library out = LibrarySubsetBuilder.build(src,
            new HashSet<>(Collections.singletonList(wpId)), Collections.emptySet());

        assertEquals(CategorySortMode.NAME, out.getCategories().get(0).getSortMode());
    }

    @Test
    public void subsetCarriesCurrentSchemaVersion()
    {
        Library out = LibrarySubsetBuilder.build(new Library(),
            Collections.emptySet(), Collections.emptySet());
        assertEquals(Library.CURRENT_SCHEMA_VERSION, out.getSchemaVersion());
    }
}
