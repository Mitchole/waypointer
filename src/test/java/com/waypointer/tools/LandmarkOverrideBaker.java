package com.waypointer.tools;

import com.google.gson.Gson;
import com.waypointer.codec.LandmarkOverridesCodec;
import com.waypointer.service.LandmarkOverridesSnapshot;
import com.waypointer.service.LandmarkOverridesSnapshot.DeletedEntry;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dev tool (lives in the test source set, never ships in the plugin jar): bakes an exported
 * landmark-override snapshot into the bundled .tsv resource files. Deletions drop the matching
 * row from that type's file(s); byType entries (the adds and edits made in the dev tools) are
 * appended to the type's primary file. Run via the Gradle task:
 *   ./gradlew bakeLandmarks                  (reads the override JSON from the clipboard)
 *   ./gradlew bakeLandmarks -Pexport=x.json  (reads it from a file instead)
 * After baking, clear your local landmark-overrides.json -- otherwise the runtime override
 * re-applies on top of the now-baked data (and its wholesale byType replacement would undo it).
 */
public final class LandmarkOverrideBaker
{
    // type name -> the TSV files that hold its rows. The first file is the "primary": new
    // entries (adds/edits) are appended there. Mirrors BboxIndex.RESOURCES.
    private static final Map<String, String[]> TYPE_FILES = buildTypeFiles();

    private LandmarkOverrideBaker() {}

    public static void main(String[] args) throws Exception
    {
        Path landmarksDir = Paths.get(System.getProperty("waypointer.landmarksDir",
            "src/main/resources/com/waypointer/landmarks"));
        String json = args.length > 0 ? readFile(args[0]) : readClipboard();
        if (json == null || json.trim().isEmpty())
        {
            System.out.println("No override JSON found (clipboard empty, or no -Pexport file). Nothing to do.");
            return;
        }
        LandmarkOverridesSnapshot snap = new LandmarkOverridesCodec(new Gson()).decode(json);
        int[] counts = bake(snap, landmarksDir);
        System.out.println("Baked landmark overrides into " + landmarksDir);
        System.out.println("Removed " + counts[0] + " row(s), added " + counts[1] + " row(s).");
        System.out.println("Next: review the .tsv diffs, then clear your local landmark-overrides.json "
            + "so the runtime override does not re-apply on top of the baked data.");
    }

    /** Applies the snapshot to the TSV files under {@code dir}. Returns {removed, added} counts. */
    public static int[] bake(LandmarkOverridesSnapshot snap, Path dir) throws IOException
    {
        Map<String, List<DeletedEntry>> delsByType = new LinkedHashMap<>();
        for (DeletedEntry d : snap.getDeletions())
        {
            delsByType.computeIfAbsent(d.getType(), k -> new ArrayList<>()).add(d);
        }

        int removed = 0;
        int added = 0;
        for (Map.Entry<String, String[]> tf : TYPE_FILES.entrySet())
        {
            String type = tf.getKey();
            String[] files = tf.getValue();
            List<DeletedEntry> dels = delsByType.getOrDefault(type, new ArrayList<>());
            List<Entry> additions = new ArrayList<>();
            TypeOverride ov = snap.getByType().get(type);
            if (ov != null && ov.getEntries() != null) additions.addAll(ov.getEntries());

            for (int i = 0; i < files.length; i++)
            {
                Path file = dir.resolve(files[i]);
                if (!Files.exists(file)) continue;
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                // Additions only go to the primary (first) file for the type.
                Result r = rewrite(lines, dels, i == 0 ? additions : new ArrayList<>());
                if (r.removed > 0 || r.added > 0)
                {
                    Files.write(file, r.lines, StandardCharsets.UTF_8);
                }
                removed += r.removed;
                added += r.added;
            }
        }
        return new int[]{removed, added};
    }

    /**
     * Pure transform: drops data rows matching any deletion (by name + bbox + plane) and appends
     * one row per addition. Comment (#) and blank lines are preserved verbatim and in place.
     */
    static Result rewrite(List<String> lines, List<DeletedEntry> deletions, List<Entry> additions)
    {
        List<String> out = new ArrayList<>();
        int removed = 0;
        for (String line : lines)
        {
            Row row = parse(line);
            if (row != null && matchesAny(row, deletions))
            {
                removed++;
                continue;
            }
            out.add(line);
        }
        for (Entry e : additions)
        {
            out.add(format(e.getX1(), e.getY1(), e.getX2(), e.getY2(), e.getPlane(), e.getName()));
        }
        return new Result(out, removed, additions.size());
    }

    static final class Result
    {
        final List<String> lines;
        final int removed;
        final int added;

        Result(List<String> lines, int removed, int added)
        {
            this.lines = lines;
            this.removed = removed;
            this.added = added;
        }
    }

    private static boolean matchesAny(Row row, List<DeletedEntry> deletions)
    {
        for (DeletedEntry d : deletions)
        {
            if (d.getX1() == row.x1 && d.getY1() == row.y1 && d.getX2() == row.x2
                && d.getY2() == row.y2 && d.getPlane() == row.plane
                && Objects.equals(d.getName(), row.name)) return true;
        }
        return false;
    }

    private static String format(int x1, int y1, int x2, int y2, int plane, String name)
    {
        return x1 + " " + y1 + " " + x2 + " " + y2 + " " + plane + "\t" + name;
    }

    private static final class Row
    {
        final int x1, y1, x2, y2, plane;
        final String name;

        Row(int x1, int y1, int x2, int y2, int plane, String name)
        {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.plane = plane;
            this.name = name;
        }
    }

    // Parses a data row "x1 y1 x2 y2 plane\tname"; returns null for comment/blank/malformed lines.
    static Row parse(String line)
    {
        if (line == null) return null;
        String s = line.trim();
        if (s.isEmpty() || s.startsWith("#")) return null;
        int tab = line.indexOf('\t');
        if (tab < 0) return null;
        String coords = line.substring(0, tab).trim();
        String name = line.substring(tab + 1).trim();
        String[] p = coords.split("\\s+");
        if (p.length < 5) return null;
        try
        {
            return new Row(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                Integer.parseInt(p[3]), Integer.parseInt(p[4]), name);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private static Map<String, String[]> buildTypeFiles()
    {
        Map<String, String[]> m = new LinkedHashMap<>();
        m.put("BANK", new String[]{"banks-bboxes.tsv", "bank-chests-bboxes.tsv"});
        m.put("ALTAR", new String[]{"altars-bboxes.tsv"});
        m.put("ANVIL", new String[]{"anvils-bboxes.tsv"});
        m.put("FURNACE", new String[]{"furnaces-bboxes.tsv"});
        m.put("LOOM", new String[]{"looms-bboxes.tsv"});
        m.put("SPINNING_WHEEL", new String[]{"spinning-wheels-bboxes.tsv"});
        m.put("TANNER", new String[]{"tanners-bboxes.tsv"});
        m.put("SPIRIT_TREE", new String[]{"spirit-trees-bboxes.tsv"});
        m.put("CHARTER_SHIP", new String[]{"charter-ships-bboxes.tsv"});
        m.put("FAIRY_RING", new String[]{"fairy-rings-bboxes.tsv"});
        m.put("SLAYER_MASTER", new String[]{"slayer-masters-bboxes.tsv"});
        return m;
    }

    private static String readFile(String path) throws IOException
    {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static String readClipboard()
    {
        try
        {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            return data == null ? null : data.toString();
        }
        catch (Exception e)
        {
            System.out.println("Could not read the clipboard (" + e.getMessage()
                + "). Pass the export with -Pexport=<file>.");
            return null;
        }
    }
}
