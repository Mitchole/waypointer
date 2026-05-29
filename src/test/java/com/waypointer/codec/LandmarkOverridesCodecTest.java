package com.waypointer.codec;

import com.google.gson.Gson;
import com.waypointer.service.LandmarkOverridesSnapshot;
import com.waypointer.service.LandmarkOverridesSnapshot.Entry;
import com.waypointer.service.LandmarkOverridesSnapshot.TypeOverride;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LandmarkOverridesCodecTest
{
    private final LandmarkOverridesCodec codec = new LandmarkOverridesCodec(new Gson());

    @Test
    public void roundTripsEmptySnapshot()
    {
        LandmarkOverridesSnapshot empty = LandmarkOverridesSnapshot.empty();
        LandmarkOverridesSnapshot back = codec.decode(codec.encode(empty));
        assertEquals(1, back.getVersion());
        assertTrue(back.getByType().isEmpty());
        assertTrue(back.getDeletions().isEmpty());
    }

    @Test
    public void roundTripsTypeOverrideWithOneEntry()
    {
        Map<String, TypeOverride> byType = new LinkedHashMap<>();
        byType.put("BANK", new TypeOverride(Arrays.asList(
            new Entry("Tel Teklan bank", 1234, 4567, 1234, 4567, 0))));
        LandmarkOverridesSnapshot s = new LandmarkOverridesSnapshot(1, byType, new java.util.ArrayList<>());

        LandmarkOverridesSnapshot back = codec.decode(codec.encode(s));
        assertEquals(1, back.getByType().size());
        TypeOverride bank = back.getByType().get("BANK");
        assertEquals(1, bank.getEntries().size());
        assertEquals("Tel Teklan bank", bank.getEntries().get(0).getName());
        assertEquals(1234, bank.getEntries().get(0).getX1());
    }
}
