package com.waypointer.codec;

import com.google.gson.Gson;
import com.waypointer.service.PresetOverridesSnapshot;
import com.waypointer.service.PresetOverridesSnapshot.CategoryOverride;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PresetOverridesCodecTest
{
    private final PresetOverridesCodec codec = new PresetOverridesCodec(new Gson());

    @Test
    public void roundTripsEmptySnapshot()
    {
        PresetOverridesSnapshot back = codec.decode(codec.encode(PresetOverridesSnapshot.empty()));
        assertEquals(1, back.getVersion());
        assertTrue(back.getByCategory().isEmpty());
        assertTrue(back.getAddedCategories().isEmpty());
        assertTrue(back.getDeletedCategories().isEmpty());
        assertTrue(back.getDeletedWaypoints().isEmpty());
    }

    @Test
    public void roundTripsCategoryOverride()
    {
        Map<String, CategoryOverride> byCategory = new LinkedHashMap<>();
        byCategory.put("Bosses", new CategoryOverride("Bosses", "Boss locations", 1234,
            Arrays.asList(new Waypoint("Vorkath", "Big dragon", 2272, 4052, 0))));
        PresetOverridesSnapshot s = new PresetOverridesSnapshot(1, byCategory,
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        PresetOverridesSnapshot back = codec.decode(codec.encode(s));
        CategoryOverride bosses = back.getByCategory().get("Bosses");
        assertEquals(1, bosses.getWaypoints().size());
        assertEquals("Vorkath", bosses.getWaypoints().get(0).getName());
        assertEquals(2272, bosses.getWaypoints().get(0).getX());
    }

    @Test
    public void emptyStringReturnsEmptySnapshot()
    {
        PresetOverridesSnapshot s = codec.decode("");
        assertEquals(1, s.getVersion());
        assertTrue(s.getByCategory().isEmpty());
    }

    @Test
    public void malformedJsonReturnsEmptySnapshot()
    {
        PresetOverridesSnapshot s = codec.decode("{garbage");
        assertEquals(1, s.getVersion());
        assertTrue(s.getByCategory().isEmpty());
    }
}
