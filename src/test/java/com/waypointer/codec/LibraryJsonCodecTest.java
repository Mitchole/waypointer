package com.waypointer.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LibraryJsonCodecTest
{
    private LibraryJsonCodec codec;

    @Before
    public void setUp()
    {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new com.google.gson.JsonSerializer<Instant>() {
                @Override public com.google.gson.JsonElement serialize(Instant src,
                    java.lang.reflect.Type t, com.google.gson.JsonSerializationContext c) {
                    return new com.google.gson.JsonPrimitive(src.toString());
                }
            })
            .registerTypeAdapter(Instant.class, new com.google.gson.JsonDeserializer<Instant>() {
                @Override public Instant deserialize(com.google.gson.JsonElement e,
                    java.lang.reflect.Type t, com.google.gson.JsonDeserializationContext c) {
                    return Instant.parse(e.getAsString());
                }
            })
            .create();
        codec = new LibraryJsonCodec(gson);
    }

    @Test
    public void emptyLibraryRoundTrips()
    {
        Library lib = new Library();
        String json = codec.encode(lib);
        Library back = codec.decode(json);
        assertEquals(Library.CURRENT_SCHEMA_VERSION, back.getSchemaVersion());
        assertTrue(back.getCategories().isEmpty());
        assertTrue(back.getWaypoints().isEmpty());
    }

    @Test
    public void populatedLibraryRoundTripsAllFields()
    {
        Library lib = new Library();
        UUID catId = UUID.randomUUID();
        lib.getCategories().add(new Category(catId, "Bossing", 0, false, null, false));
        lib.getWaypoints().add(new Waypoint(
            UUID.randomUUID(), "Vorkath", 42, catId, 8059, "best icon",
            Instant.parse("2026-05-02T12:00:00Z"), 0, false, null, false));

        Library back = codec.decode(codec.encode(lib));
        assertEquals(1, back.getCategories().size());
        assertEquals("Bossing", back.getCategories().get(0).getName());
        assertEquals(1, back.getWaypoints().size());
        Waypoint w = back.getWaypoints().get(0);
        assertEquals("Vorkath", w.getName());
        assertEquals(42, w.getPackedWorldPoint());
        assertEquals(Integer.valueOf(8059), w.getIconId());
        assertEquals("best icon", w.getNotes());
        assertEquals(Instant.parse("2026-05-02T12:00:00Z"), w.getCreatedAt());
    }

    @Test
    public void categoryIconIdRoundTrips()
    {
        Library lib = new Library();
        UUID catId = UUID.randomUUID();
        lib.getCategories().add(new Category(catId, "Mining", 0, false, 199, false));

        Library back = codec.decode(codec.encode(lib));
        assertEquals(1, back.getCategories().size());
        assertEquals(Integer.valueOf(199), back.getCategories().get(0).getIconId());

        // null iconId also round-trips
        Library lib2 = new Library();
        lib2.getCategories().add(new Category(UUID.randomUUID(), "Plain", 0, false, null, false));
        Library back2 = codec.decode(codec.encode(lib2));
        assertNull(back2.getCategories().get(0).getIconId());
    }

    @Test(expected = LibraryJsonCodec.UnsupportedSchemaException.class)
    public void rejectsSchemaVersionTooNew()
    {
        Library lib = new Library();
        lib.setSchemaVersion(Library.CURRENT_SCHEMA_VERSION + 5);
        String json = codec.encode(lib);
        codec.decode(json);
    }

    @Test
    public void missingFieldsDecodeWithDefaults()
    {
        String minimal = "{\"schemaVersion\":1}";
        Library back = codec.decode(minimal);
        assertNotNull(back.getCategories());
        assertNotNull(back.getWaypoints());
        assertTrue(back.getCategories().isEmpty());
        assertTrue(back.getWaypoints().isEmpty());
    }

    @Test(expected = LibraryJsonCodec.MalformedLibraryException.class)
    public void malformedJsonThrows()
    {
        codec.decode("not json {{");
    }

    @Test
    public void categoryWithSortMode_roundTrips()
    {
        Library lib = new Library();
        UUID catId = UUID.randomUUID();
        Category c = new Category(catId, "POIs", 0, false, null, false);
        c.setSortMode(com.waypointer.model.CategorySortMode.NAME);
        lib.getCategories().add(c);

        Library back = codec.decode(codec.encode(lib));

        assertEquals(1, back.getCategories().size());
        assertEquals(com.waypointer.model.CategorySortMode.NAME,
            back.getCategories().get(0).getSortMode());
    }

    @Test
    public void legacyJsonWithoutSortMode_decodesAsNull()
    {
        String legacyJson =
            "{\"schemaVersion\":2,"
            + "\"categories\":["
            + "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":\"Old\","
            + "\"sortOrder\":0,\"uncategorized\":false,\"bundled\":false}"
            + "],"
            + "\"waypoints\":[]}";

        Library back = codec.decode(legacyJson);

        assertEquals(1, back.getCategories().size());
        assertNull("legacy category must decode with null sortMode",
            back.getCategories().get(0).getSortMode());
    }
}
