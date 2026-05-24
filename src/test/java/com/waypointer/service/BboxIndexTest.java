package com.waypointer.service;

import com.waypointer.model.WorldPointPacker;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BboxIndexTest
{
    @Test
    public void loadsBundledTsvs()
    {
        BboxIndex idx = new BboxIndex();
        // (0, 0, 0) is outside every known bbox so the bundled-resource ctor must return null
        // rather than throwing — proves loadResource walked every TSV without error.
        assertNull(idx.lookup(WorldPointPacker.pack(0, 0, 0)));
    }

    @Test
    public void forTestingBuildsFromLiteralEntries()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));
        assertEquals("Edgeville Bank", idx.lookup(WorldPointPacker.pack(3094, 3493, 0)));
    }

    @Test
    public void entryComputesAreaInclusive()
    {
        BboxIndex.Entry e = new BboxIndex.Entry(0, 0, 4, 4, 0, "x");
        assertTrue(e.area == 25); // (4-0+1) * (4-0+1)
    }

    @Test
    public void tileInsideBboxReturnsName()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));

        assertEquals("Edgeville Bank", idx.lookup(WorldPointPacker.pack(3094, 3493, 0)));
    }

    @Test
    public void tileOutsideBboxReturnsNull()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));

        assertNull(idx.lookup(WorldPointPacker.pack(3200, 3200, 0)));
    }

    @Test
    public void smallestAreaWinsOnOverlap()
    {
        // Outer 100x100 polygon, inner 5x5 polygon, point inside both.
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3000, 3000, 3099, 3099, 0, "Big Area"),
            new BboxIndex.Entry(3050, 3050, 3054, 3054, 0, "Small POI")));

        assertEquals("Small POI", idx.lookup(WorldPointPacker.pack(3052, 3052, 0)));
    }

    @Test
    public void ignoresOtherPlanes()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank")));

        assertNull(idx.lookup(WorldPointPacker.pack(3094, 3493, 1)));
    }

    @Test
    public void degenerateOneByOneBboxMatchesExactTile()
    {
        BboxIndex idx = BboxIndex.forTesting(Arrays.asList(
            new BboxIndex.Entry(2412, 4434, 2412, 4434, 0, "Zanaris Fairy Ring")));

        assertEquals("Zanaris Fairy Ring", idx.lookup(WorldPointPacker.pack(2412, 4434, 0)));
        assertNull(idx.lookup(WorldPointPacker.pack(2413, 4434, 0)));
    }

    @Test
    public void bundledTsvsCoverEdgevilleBank()
    {
        BboxIndex idx = new BboxIndex();
        String name = idx.lookup(WorldPointPacker.pack(3094, 3493, 0));
        assertEquals("Edgeville Bank", name);
    }

    @Test
    public void bundledTsvsCoverGrandExchangePlaza()
    {
        BboxIndex idx = new BboxIndex();
        String name = idx.lookup(WorldPointPacker.pack(3163, 3490, 0));
        assertNotNull(name);
        assertTrue("expected GE-area bbox, got " + name, name.toLowerCase().contains("grand exchange"));
    }

    // --- nearest(type, from) -----------------------------------------------------

    @Test
    public void nearest_picksClosestOfRequestedType()
    {
        // Two banks at different distances; player at (3094, 3493). The Edgeville one
        // (centre ~3094, 3494) is right next to the player; Falador (~3014, 3357) is far.
        BboxIndex idx = BboxIndex.forTesting(java.util.Arrays.asList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank", LandmarkType.BANK),
            new BboxIndex.Entry(3010, 3353, 3018, 3361, 0, "Falador West Bank", LandmarkType.BANK)));

        BboxIndex.Hit hit = idx.nearest(LandmarkType.BANK, WorldPointPacker.pack(3094, 3493, 0));

        assertNotNull(hit);
        assertEquals("Edgeville Bank", hit.name);
    }

    @Test
    public void nearest_clampsToBboxEdgeNotCentre()
    {
        // 5x5 bank at (3050..3054, 3050..3054). Player at (3056, 3052) is 2 tiles east of
        // the bbox; nearest must be 2, not the distance to centre (~4).
        BboxIndex idx = BboxIndex.forTesting(java.util.Collections.singletonList(
            new BboxIndex.Entry(3050, 3050, 3054, 3054, 0, "Test Bank", LandmarkType.BANK)));

        BboxIndex.Hit hit = idx.nearest(LandmarkType.BANK, WorldPointPacker.pack(3056, 3052, 0));

        assertNotNull(hit);
        assertEquals(2, hit.distance);
        // Hit.packed is the bbox tile closest to the player: x clamped to 3054, y = 3052.
        assertEquals(3054, WorldPointPacker.getX(hit.packed));
        assertEquals(3052, WorldPointPacker.getY(hit.packed));
        assertEquals(0, WorldPointPacker.getPlane(hit.packed));
    }

    @Test
    public void nearest_isPlaneAgnostic()
    {
        // Player on plane 1 (dungeon), only bank is on plane 0 (overworld).
        // Plane-agnostic search returns it, and the Hit carries the bbox's plane (0).
        BboxIndex idx = BboxIndex.forTesting(java.util.Collections.singletonList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank", LandmarkType.BANK)));

        BboxIndex.Hit hit = idx.nearest(LandmarkType.BANK, WorldPointPacker.pack(3094, 3493, 1));

        assertNotNull(hit);
        assertEquals(0, WorldPointPacker.getPlane(hit.packed));
    }

    @Test
    public void nearest_returnsNullWhenNoEntriesOfType()
    {
        BboxIndex idx = BboxIndex.forTesting(java.util.Collections.singletonList(
            new BboxIndex.Entry(3087, 3486, 3103, 3502, 0, "Edgeville Bank", LandmarkType.BANK)));

        // No altars in the index at all.
        assertNull(idx.nearest(LandmarkType.ALTAR, WorldPointPacker.pack(3094, 3493, 0)));
    }

    @Test
    public void nearest_chebyshevDiagonalIsOne()
    {
        // 1x1 bank at (3050, 3050). Player at (3051, 3051) is one tile diagonally away.
        // Chebyshev gives 1 (matches OSRS movement), not sqrt(2).
        BboxIndex idx = BboxIndex.forTesting(java.util.Collections.singletonList(
            new BboxIndex.Entry(3050, 3050, 3050, 3050, 0, "Tile Bank", LandmarkType.BANK)));

        BboxIndex.Hit hit = idx.nearest(LandmarkType.BANK, WorldPointPacker.pack(3051, 3051, 0));

        assertNotNull(hit);
        assertEquals(1, hit.distance);
    }

    @Test
    public void nearest_includesBankChests()
    {
        // Use the production constructor so the bank-chests TSV's BANK mapping is actually
        // exercised. The Lunar Isle bank chest lives around (2099, 3919). Asking for the
        // nearest BANK from that tile must return a hit (could be the chest or a Piscarilius
        // bank further away -- both are LandmarkType.BANK).
        BboxIndex idx = new BboxIndex();
        BboxIndex.Hit hit = idx.nearest(LandmarkType.BANK, WorldPointPacker.pack(2099, 3919, 0));

        assertNotNull(hit);
        // distance to the nearest BANK from inside the dataset must be small (this point
        // sits on or near the Lunar Isle bank chest itself).
        assertTrue("expected nearby bank, got distance " + hit.distance, hit.distance < 10);
    }
}
