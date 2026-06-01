package com.waypointer.tools;

import com.waypointer.service.LandmarkOverridesSnapshot;
import com.waypointer.service.LandmarkOverridesSnapshot.DeletedEntry;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LandmarkOverrideBakerTest
{
    @Test
    public void rewriteDropsDeletedRowKeepsCommentsAndAppendsAdditions()
    {
        List<String> lines = Arrays.asList(
            "# header comment",
            "# x1 y1 x2 y2 plane\tname",
            "2983 2266 2987 2270 0\tThe Onyx Crest Chest Bank",
            "3087 3486 3103 3502 0\tEdgeville Bank");

        List<DeletedEntry> dels = Collections.singletonList(
            new DeletedEntry("BANK", "The Onyx Crest Chest Bank", 2983, 2266, 2987, 2270, 0));
        List<Entry> adds = Collections.singletonList(
            new Entry("Nardah Bank", 3427, 2891, 3427, 2891, 0));

        LandmarkOverrideBaker.Result r = LandmarkOverrideBaker.rewrite(lines, dels, adds);

        assertEquals(1, r.removed);
        assertEquals(1, r.added);
        // Comments kept, fake dropped, real kept, addition appended last.
        assertEquals(Arrays.asList(
            "# header comment",
            "# x1 y1 x2 y2 plane\tname",
            "3087 3486 3103 3502 0\tEdgeville Bank",
            "3427 2891 3427 2891 0\tNardah Bank"), r.lines);
    }

    @Test
    public void parseIgnoresCommentsAndBlanksButReadsDataRows()
    {
        assertEquals(null, LandmarkOverrideBaker.parse("# comment"));
        assertEquals(null, LandmarkOverrideBaker.parse("   "));
        assertEquals(null, LandmarkOverrideBaker.parse("no tab here"));
        // A real row parses (round-trips through a deletion match).
        List<DeletedEntry> d = Collections.singletonList(
            new DeletedEntry("BANK", "X Bank", 1, 2, 3, 4, 0));
        LandmarkOverrideBaker.Result r = LandmarkOverrideBaker.rewrite(
            Collections.singletonList("1 2 3 4 0\tX Bank"), d, Collections.emptyList());
        assertEquals(1, r.removed);
        assertTrue(r.lines.isEmpty());
    }

    @Test
    public void bakeEditFlowRemovesOriginalAcrossFilesAndAppendsPointToPrimary() throws Exception
    {
        Path dir = Files.createTempDirectory("bake");
        try
        {
            // Edited bank lived in the secondary file (bank-chests); its point replacement must
            // land in the primary file (banks). A fake in the primary is also deleted.
            Files.write(dir.resolve("banks-bboxes.tsv"), Arrays.asList(
                "# banks",
                "2983 2266 2987 2270 0\tThe Onyx Crest Chest Bank",
                "3087 3486 3103 3502 0\tEdgeville Bank").stream()
                .reduce((a, b) -> a + "\n" + b).orElse("").getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("bank-chests-bboxes.tsv"), (
                "# bank chests\n2807 2519 2811 2523 0\tRed Rock Bank Chest").getBytes(StandardCharsets.UTF_8));

            Map<String, TypeOverride> by = new LinkedHashMap<>();
            by.put("BANK", new TypeOverride(new ArrayList<>(Collections.singletonList(
                new Entry("Red Rock Chest Bank", 2809, 2521, 2809, 2521, 0)))));
            List<DeletedEntry> dels = new ArrayList<>(Arrays.asList(
                new DeletedEntry("BANK", "The Onyx Crest Chest Bank", 2983, 2266, 2987, 2270, 0),
                new DeletedEntry("BANK", "Red Rock Bank Chest", 2807, 2519, 2811, 2523, 0)));
            LandmarkOverridesSnapshot snap = new LandmarkOverridesSnapshot(1, by, dels);

            int[] counts = LandmarkOverrideBaker.bake(snap, dir);

            assertEquals(2, counts[0]); // two rows removed (one per file)
            assertEquals(1, counts[1]); // one addition

            List<String> banks = Files.readAllLines(dir.resolve("banks-bboxes.tsv"), StandardCharsets.UTF_8);
            List<String> chests = Files.readAllLines(dir.resolve("bank-chests-bboxes.tsv"), StandardCharsets.UTF_8);

            assertTrue("Edgeville kept", banks.contains("3087 3486 3103 3502 0\tEdgeville Bank"));
            assertFalse("fake removed", banks.contains("2983 2266 2987 2270 0\tThe Onyx Crest Chest Bank"));
            assertTrue("point appended to primary", banks.contains("2809 2521 2809 2521 0\tRed Rock Chest Bank"));
            assertFalse("original chest removed from secondary",
                chests.contains("2807 2519 2811 2523 0\tRed Rock Bank Chest"));
        }
        finally
        {
            Files.walk(dir).sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
        }
    }
}
