package com.waypointer.preset;

import com.google.gson.Gson;
import com.waypointer.service.PresetOverrides;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PresetCatalogTest
{
    private final PresetCatalog catalog = new PresetCatalog(new Gson(), PresetOverrides.forTesting());

    @Test
    public void parsesValidJson()
    {
        String json = "{\"presets\":[{\"category\":\"Banks\",\"description\":\"d\","
            + "\"icon\":12,\"waypoints\":[{\"name\":\"GE\",\"description\":\"hub\","
            + "\"x\":3164,\"y\":3486,\"plane\":0}]}]}";
        List<Preset> presets = catalog.parse(json);
        assertEquals(1, presets.size());
        Preset p = presets.get(0);
        assertEquals("Banks", p.getCategory());
        assertEquals(Integer.valueOf(12), p.getIcon());
        assertEquals(1, p.getWaypoints().size());
        assertEquals("GE", p.getWaypoints().get(0).getName());
        assertEquals(3164, p.getWaypoints().get(0).getX());
    }

    @Test
    public void malformedJsonYieldsEmptyList()
    {
        assertTrue(catalog.parse("{not valid json").isEmpty());
    }

    @Test
    public void nullAndEmptyObjectYieldEmptyList()
    {
        assertTrue(catalog.parse("null").isEmpty());
        assertTrue(catalog.parse("{}").isEmpty());
    }

    @Test
    public void loadsBundledResource()
    {
        assertFalse("bundled preset-waypoints.json should contain presets",
            catalog.getPresets().isEmpty());
    }
}
