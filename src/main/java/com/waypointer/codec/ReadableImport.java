package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Turns pasted text into a {@link Library} the import picker can show. The text is either a JSON
 * array of {@code {name, x, y, plane?, category?}} objects or plain lines of
 * {@code name, x, y[, plane]}. A line starting with {@code #} sets the category for the lines
 * after it. Entries without a category go to {@code Imported}.
 */
public class ReadableImport
{
    static final String DEFAULT_CATEGORY = "Imported";
    private static final int MAX_CHARS = 1_048_576;
    private static final int MAX_PLANE = 3;

    private static final String NOTHING = "Nothing to import.";
    private static final String TOO_LARGE = "That list is too large.";
    private static final String NOT_VALID = "That is not a valid list.";

    private final Gson gson;

    @Inject
    public ReadableImport(Gson gson)
    {
        this.gson = gson;
    }

    /** True when the trimmed text starts with a share code prefix. */
    public static boolean isShareCode(String text)
    {
        if (text == null) return false;
        String t = text.trim();
        return t.startsWith("WP1:") || t.startsWith("WPL1:");
    }

    /**
     * Parses pasted text. Entries already in the matching category of {@code existing} (same name,
     * ignoring case, and same tile) are left out and counted in {@link Result#alreadyPresent}.
     *
     * @throws MalformedListException with a message fit to show the user
     */
    public Result parse(String text, Library existing)
    {
        if (text != null && text.length() > MAX_CHARS)
        {
            throw new MalformedListException(TOO_LARGE);
        }
        String trimmed = text == null ? "" : text.trim();
        List<Entry> entries = trimmed.startsWith("[") ? readJson(trimmed) : readLines(text == null ? "" : text);
        if (entries.isEmpty())
        {
            throw new MalformedListException(NOTHING);
        }
        return build(entries, existing);
    }

    // ---- lines ----

    private static List<Entry> readLines(String text)
    {
        List<Entry> out = new ArrayList<>();
        String category = DEFAULT_CATEGORY;
        String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++)
        {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (line.charAt(0) == '#')
            {
                String name = line.substring(1).trim();
                category = name.isEmpty() ? DEFAULT_CATEGORY : name;
                continue;
            }
            out.add(readLine(line, i + 1, category));
        }
        return out;
    }

    /**
     * Splits a line into name and trailing numbers. Up to three trailing integers are peeled off;
     * all three are the coordinates only when the last is a plane (0 to 3), otherwise the last two
     * are and the third belongs to the name ("Duck 4 2410 3725").
     */
    private static Entry readLine(String line, int lineNumber, String category)
    {
        int[] numStart = new int[3];
        int[] sepStart = new int[3];
        int count = 0;
        int end = line.length();
        while (count < 3)
        {
            int e = end;
            while (e > 0 && Character.isWhitespace(line.charAt(e - 1))) e--;
            int s = e;
            while (s > 0 && Character.isDigit(line.charAt(s - 1))) s--;
            if (s == e) break;
            if (s > 0 && line.charAt(s - 1) == '-') s--;
            int sep = s;
            while (sep > 0 && isSeparator(line.charAt(sep - 1))) sep--;
            if (sep == s) break;
            numStart[count] = s;
            sepStart[count] = sep;
            count++;
            end = sep;
        }
        if (count < 2)
        {
            throw new MalformedListException("Line " + lineNumber
                + ": expected a name followed by x, y.");
        }
        // Index 0 is the rightmost number. Pick how many belong to the coordinates.
        int k = 2;
        if (count == 3)
        {
            String plane = line.substring(numStart[0]).trim();
            if (plane.matches("[0-3]")) k = 3;
        }
        int first = k - 1;
        String name = line.substring(0, sepStart[first]).replaceAll("[,\\s]+$", "");
        if (name.isEmpty())
        {
            throw new MalformedListException("Line " + lineNumber
                + ": expected a name followed by x, y.");
        }
        int packed;
        try
        {
            packed = CoordinateText.parse(line.substring(numStart[first]));
        }
        catch (IllegalArgumentException ex)
        {
            throw new MalformedListException("Line " + lineNumber + ": " + ex.getMessage());
        }
        return new Entry(name, packed, category);
    }

    private static boolean isSeparator(char c)
    {
        return c == ',' || Character.isWhitespace(c);
    }

    // ---- json ----

    private List<Entry> readJson(String text)
    {
        JsonArray array = JsonDecodeSupport.parseArray(gson, text,
            t -> new MalformedListException(NOT_VALID));
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < array.size(); i++)
        {
            out.add(readEntry(array.get(i), i + 1));
        }
        return out;
    }

    private static Entry readEntry(JsonElement el, int index)
    {
        String missing = "Entry " + index + ": needs a name, x and y.";
        if (!el.isJsonObject()) throw new MalformedListException(missing);
        JsonObject o = el.getAsJsonObject();
        String name = stringOrNull(o.get("name"));
        BigInteger x = integerOrNull(o.get("x"));
        BigInteger y = integerOrNull(o.get("y"));
        if (name == null || name.trim().isEmpty() || x == null || y == null)
        {
            throw new MalformedListException(missing);
        }
        BigInteger plane = BigInteger.ZERO;
        if (o.has("plane") && !o.get("plane").isJsonNull())
        {
            plane = integerOrNull(o.get("plane"));
            if (plane == null) throw new MalformedListException(missing);
        }
        String category = DEFAULT_CATEGORY;
        if (o.has("category") && !o.get("category").isJsonNull())
        {
            String c = stringOrNull(o.get("category"));
            if (c == null) throw new MalformedListException(missing);
            if (!c.trim().isEmpty()) category = c.trim();
        }
        try
        {
            return new Entry(name.trim(), CoordinateText.parse(x, y, plane), category);
        }
        catch (IllegalArgumentException ex)
        {
            throw new MalformedListException("Entry " + index + ": " + ex.getMessage());
        }
    }

    private static String stringOrNull(JsonElement e)
    {
        if (e == null || !e.isJsonPrimitive()) return null;
        JsonPrimitive p = e.getAsJsonPrimitive();
        return p.isString() ? p.getAsString() : null;
    }

    private static BigInteger integerOrNull(JsonElement e)
    {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        try
        {
            return new BigDecimal(e.getAsString()).toBigIntegerExact();
        }
        catch (ArithmeticException | NumberFormatException ex)
        {
            return null;
        }
    }

    // ---- assembly ----

    private static Result build(List<Entry> entries, Library existing)
    {
        Library lib = new Library();
        Map<String, Category> byKey = new LinkedHashMap<>();
        Map<UUID, Integer> nextOrder = new LinkedHashMap<>();
        int nextCategoryOrder = 0;
        for (Category c : existing.getCategories())
        {
            nextCategoryOrder = Math.max(nextCategoryOrder, c.getSortOrder() + 1);
        }
        int alreadyPresent = 0;
        for (Entry e : entries)
        {
            String key = e.category.toLowerCase(Locale.ROOT);
            Category match = findExisting(existing, e.category);
            Category target = byKey.get(key);
            if (match != null && isDuplicate(existing, match, e))
            {
                alreadyPresent++;
                continue;
            }
            if (target == null)
            {
                target = match != null ? copyOf(match)
                    : new Category(UUID.randomUUID(), e.category, nextCategoryOrder++, false, null, false);
                byKey.put(key, target);
                lib.getCategories().add(target);
            }
            int order = nextOrder.merge(target.getId(), 1, Integer::sum) - 1;
            lib.getWaypoints().add(new Waypoint(UUID.randomUUID(), e.name, e.packed, target.getId(),
                null, "", Instant.now(), order, false, null, false));
        }
        lib.setWaypoints(ModelSanitizer.sanitizeWaypoints(lib.getWaypoints()));
        if (lib.getWaypoints().isEmpty() && alreadyPresent == 0)
        {
            throw new MalformedListException(NOTHING);
        }
        return new Result(lib, alreadyPresent);
    }

    private static Category findExisting(Library existing, String name)
    {
        for (Category c : existing.getCategories())
        {
            if (c.getName() != null && c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    private static boolean isDuplicate(Library existing, Category cat, Entry e)
    {
        for (Waypoint w : existing.getWaypoints())
        {
            if (cat.getId().equals(w.getCategoryId())
                && w.getPackedWorldPoint() == e.packed
                && w.getName() != null && w.getName().equalsIgnoreCase(e.name))
            {
                return true;
            }
        }
        return false;
    }

    private static Category copyOf(Category c)
    {
        Category copy = new Category(c.getId(), c.getName(), c.getSortOrder(), c.isUncategorized(),
            c.getIconId(), c.isBundled());
        copy.setSortMode(c.getSortMode());
        copy.setColor(c.getColor());
        return copy;
    }

    private static final class Entry
    {
        final String name;
        final int packed;
        final String category;

        Entry(String name, int packed, String category)
        {
            this.name = name;
            this.packed = packed;
            this.category = category;
        }
    }

    /** The library to offer in the picker, and how many entries were skipped as already saved. */
    public static final class Result
    {
        public final Library library;
        public final int alreadyPresent;

        Result(Library library, int alreadyPresent)
        {
            this.library = library;
            this.alreadyPresent = alreadyPresent;
        }
    }

    /** Thrown for text that cannot be read as a list; the message is shown to the user. */
    public static final class MalformedListException extends RuntimeException
    {
        public MalformedListException(String message)
        {
            super(message);
        }
    }
}
