package com.waypointer.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import com.google.gson.Gson;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;

public class ReadableImportTest
{
    private final ReadableImport importer = new ReadableImport(new Gson());

    private static Waypoint only(Library lib, String name)
    {
        for (Waypoint w : lib.getWaypoints())
        {
            if (w.getName().equals(name)) return w;
        }
        fail("no waypoint " + name);
        return null;
    }

    private static String catName(Library lib, Waypoint w)
    {
        for (Category c : lib.getCategories())
        {
            if (c.getId().equals(w.getCategoryId())) return c.getName();
        }
        return null;
    }

    private static void assertRejected(ReadableImport imp, String text, String message)
    {
        try
        {
            imp.parse(text, new Library());
            fail("expected rejection: " + message);
        }
        catch (ReadableImport.MalformedListException e)
        {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void linesWithCategoriesAndDefaultCategory()
    {
        Library lib = importer.parse("Loose one, 100, 200\n# Charting\n"
            + "Shipwreck one, 2301, 3690\nCurrent duck 4, 2410 3725 0\n", new Library()).library;

        assertEquals(2, lib.getCategories().size());
        assertEquals("Imported", lib.getCategories().get(0).getName());
        assertEquals("Charting", lib.getCategories().get(1).getName());
        assertEquals(3, lib.getWaypoints().size());
        assertEquals("Loose one", lib.getWaypoints().get(0).getName());
        assertEquals(WorldPointPacker.pack(100, 200, 0), lib.getWaypoints().get(0).getPackedWorldPoint());
        assertEquals("Imported", catName(lib, lib.getWaypoints().get(0)));
        assertEquals("Shipwreck one", lib.getWaypoints().get(1).getName());
        assertEquals("Current duck 4", lib.getWaypoints().get(2).getName());
        assertEquals(WorldPointPacker.pack(2410, 3725, 0), lib.getWaypoints().get(2).getPackedWorldPoint());
        assertEquals("Charting", catName(lib, lib.getWaypoints().get(2)));
    }

    @Test
    public void namesWithDigitsAndCommas()
    {
        Library lib = importer.parse("Rock, north, 2301 3690\nDuck 4 2410 3725\nSpot 7 100 200 1\n",
            new Library()).library;
        assertEquals(WorldPointPacker.pack(2301, 3690, 0), only(lib, "Rock, north").getPackedWorldPoint());
        assertEquals(WorldPointPacker.pack(2410, 3725, 0), only(lib, "Duck 4").getPackedWorldPoint());
        assertEquals(WorldPointPacker.pack(100, 200, 1), only(lib, "Spot 7").getPackedWorldPoint());
    }

    @Test
    public void crlfTabsAndTrailingBlankLines()
    {
        Library lib = importer.parse("\r\n  Alpha,\t10,\t20 \r\n\r\n# Sea \r\nBeta 30 40\r\n\r\n\r\n",
            new Library()).library;
        assertEquals(2, lib.getWaypoints().size());
        assertEquals(WorldPointPacker.pack(10, 20, 0), only(lib, "Alpha").getPackedWorldPoint());
        assertEquals("Sea", catName(lib, only(lib, "Beta")));
    }

    @Test
    public void jsonArray()
    {
        Library lib = importer.parse(
            "[{\"name\":\"A\",\"x\":1,\"y\":2},{\"name\":\"B\",\"x\":3,\"y\":4,\"plane\":1,\"category\":\"Sea\"}]",
            new Library()).library;
        assertEquals(WorldPointPacker.pack(1, 2, 0), only(lib, "A").getPackedWorldPoint());
        assertEquals("Imported", catName(lib, only(lib, "A")));
        assertEquals(WorldPointPacker.pack(3, 4, 1), only(lib, "B").getPackedWorldPoint());
        assertEquals("Sea", catName(lib, only(lib, "B")));
    }

    private static Library existingWith(String category, String waypoint, int packed)
    {
        Library existing = new Library();
        Category c = new Category(UUID.randomUUID(), category, 3, false, 77, false);
        existing.getCategories().add(c);
        existing.getWaypoints().add(new Waypoint(UUID.randomUUID(), waypoint, packed, c.getId(), null,
            "", Instant.EPOCH, 0, false, null, false));
        return existing;
    }

    @Test
    public void reusesExistingCategoryIgnoringCase()
    {
        Library existing = existingWith("charting", "Old", WorldPointPacker.pack(5, 5, 0));
        UUID id = existing.getCategories().get(0).getId();
        Library lib = importer.parse("# Charting\nNew 10 20", existing).library;
        assertEquals(1, lib.getCategories().size());
        assertEquals(id, lib.getCategories().get(0).getId());
        assertEquals(Integer.valueOf(77), lib.getCategories().get(0).getIconId());
        assertEquals(id, lib.getWaypoints().get(0).getCategoryId());
    }

    @Test
    public void skipsEntriesAlreadyPresent()
    {
        Library existing = existingWith("Charting", "Wreck", WorldPointPacker.pack(10, 20, 0));
        ReadableImport.Result r = importer.parse("# Charting\nWRECK 10 20\nOther 1 2", existing);
        assertEquals(1, r.alreadyPresent);
        assertEquals(1, r.library.getWaypoints().size());
        assertEquals("Other", r.library.getWaypoints().get(0).getName());
    }

    @Test
    public void sameNameDifferentTileIsKept()
    {
        Library existing = existingWith("Charting", "Wreck", WorldPointPacker.pack(10, 20, 0));
        ReadableImport.Result r = importer.parse("# Charting\nWreck 11 20", existing);
        assertEquals(0, r.alreadyPresent);
        assertEquals(1, r.library.getWaypoints().size());
    }

    @Test
    public void freshUuidsEveryParse()
    {
        Library a = importer.parse("X 1 2", new Library()).library;
        Library b = importer.parse("X 1 2", new Library()).library;
        assertNotEquals(a.getWaypoints().get(0).getId(), b.getWaypoints().get(0).getId());
        assertNotEquals(a.getCategories().get(0).getId(), b.getCategories().get(0).getId());
    }

    @Test
    public void badLineReportsLineNumber()
    {
        assertRejected(importer, "A 1 2\n\nJust a name\n", "Line 3: expected a name followed by x, y.");
        assertRejected(importer, "# Cat\nA 1", "Line 2: expected a name followed by x, y.");
    }

    @Test
    public void badEntryReportsIndex()
    {
        assertRejected(importer, "[{\"name\":\"A\",\"x\":1,\"y\":2},{\"name\":\"B\",\"x\":3}]",
            "Entry 2: needs a name, x and y.");
        assertRejected(importer, "[{\"name\":\" \",\"x\":1,\"y\":2}]", "Entry 1: needs a name, x and y.");
        assertRejected(importer, "[5]", "Entry 1: needs a name, x and y.");
    }

    @Test
    public void outOfRangeCoordinateReportsLine()
    {
        assertRejected(importer, "A 1 2\nB 99999 5", "Line 2: x and y must be between 0 and 16383.");
        assertRejected(importer, "[{\"name\":\"B\",\"x\":99999,\"y\":5}]",
            "Entry 1: x and y must be between 0 and 16383.");
        assertRejected(importer, "[{\"name\":\"B\",\"x\":1,\"y\":5,\"plane\":4}]",
            "Entry 1: Plane must be between 0 and 3.");
    }

    @Test
    public void emptyAndOversizedInputRejected()
    {
        assertRejected(importer, "", "Nothing to import.");
        assertRejected(importer, " \n# only a heading\n\n", "Nothing to import.");
        assertRejected(importer, "[]", "Nothing to import.");
        StringBuilder big = new StringBuilder();
        while (big.length() <= 1_048_576) big.append("A 1 2\n");
        assertRejected(importer, big.toString(), "That list is too large.");
    }

    @Test
    public void brokenJsonRejected()
    {
        assertRejected(importer, "[{\"name\":", "That is not a valid list.");
        assertRejected(importer, "[1,2] trailing", "That is not a valid list.");
    }

    @Test
    public void shareCodePrefixDetected()
    {
        assertTrue(ReadableImport.isShareCode(" WPL1:abc"));
        assertTrue(ReadableImport.isShareCode("WP1:abc"));
        assertFalse(ReadableImport.isShareCode("# x"));
        assertFalse(ReadableImport.isShareCode(null));
    }

    @Test(timeout = 5000)
    public void longSeparatorRunDoesNotHang()
    {
        StringBuilder huge = new StringBuilder("a");
        for (int i = 0; i < 100_000; i++) huge.append(',');
        huge.append("b 1 2");
        assertRejected(importer, huge.toString(), "Line 1: too long.");

        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 25; i++) name.append("abcdefghi").append(',');
        for (int i = 0; i < 175; i++) name.append(',');
        long start = System.nanoTime();
        try
        {
            importer.parse(name + "z 1 2", new Library());
        }
        catch (ReadableImport.MalformedListException ignored)
        {
            // either outcome is fine; only the time matters
        }
        assertTrue(System.nanoTime() - start < 1_000_000_000L);
    }

    @Test(timeout = 5000)
    public void hugeExponentNumberRejectedQuickly()
    {
        assertRejected(importer, "[{\"name\":\"A\",\"x\":1e100000000,\"y\":2}]",
            "Entry 1: needs a name, x and y.");
    }

    @Test
    public void zeroZeroRejectedWithLineNumber()
    {
        assertRejected(importer, "A 1 2\nSpot 0 0", "Line 2: x and y cannot both be 0.");
        assertRejected(importer, "Spot 0, 0, 1", "Line 1: x and y cannot both be 0.");
    }

    @Test
    public void zeroZeroRejectedWithEntryNumber()
    {
        assertRejected(importer,
            "[{\"name\":\"A\",\"x\":1,\"y\":2},{\"name\":\"B\",\"x\":0,\"y\":0}]",
            "Entry 2: x and y cannot both be 0.");
    }

    @Test
    public void overlongLineRejected()
    {
        StringBuilder line = new StringBuilder();
        while (line.length() < 300) line.append('x');
        assertRejected(importer, "A 1 2\n" + line + " 1 2", "Line 2: too long.");
        assertRejected(importer, "# " + line, "Line 1: too long.");
    }

    @Test
    public void bracketFirstLineIsNotAValidList()
    {
        assertRejected(importer, "[Boat] Wreck 1 2", "That is not a valid list.");
    }

    private static final String NEEDS = "Entry 1: needs a name, x and y.";

    private static String withX(String x)
    {
        return "[{\"name\":\"A\",\"x\":" + x + ",\"y\":2}]";
    }

    @Test(timeout = 2000)
    public void hugeNegativeExponentRejectedQuickly()
    {
        assertRejected(importer, withX("1e-100000000"), NEEDS);
    }

    @Test(timeout = 2000)
    public void fractionalNumberRejected()
    {
        assertRejected(importer, withX("1.5"), NEEDS);
        assertRejected(importer, withX("1.0"), NEEDS);
    }

    @Test(timeout = 2000)
    public void exponentFormRejected()
    {
        assertRejected(importer, withX("1e3"), NEEDS);
    }

    @Test(timeout = 2000)
    public void veryLongDigitRunRejectedQuickly()
    {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < 100_000; i++) digits.append('1');
        assertRejected(importer, withX(digits.toString()), NEEDS);
    }

    @Test(timeout = 2000)
    public void plainIntegersStillAccepted()
    {
        Library lib = importer.parse(
            "[{\"name\":\"A\",\"x\":2301,\"y\":3690,\"plane\":1}]", new Library()).library;
        assertEquals(WorldPointPacker.pack(2301, 3690, 1), lib.getWaypoints().get(0).getPackedWorldPoint());
        assertRejected(importer, "[{\"name\":\"A\",\"x\":\"12\",\"y\":2}]", NEEDS);
    }
}
