package com.waypointer.ui;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

public class CollapseStateCodecTest
{
    private final CollapseStateCodec codec = new CollapseStateCodec(new Gson());

    @Test
    public void decodeEmptyStringYieldsEmptyMap()
    {
        assertTrue(codec.decode("").isEmpty());
        assertTrue(codec.decode("{}").isEmpty());
    }

    @Test
    public void encodeAndDecodeRoundTrip()
    {
        Map<UUID, Boolean> map = new HashMap<>();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        map.put(a, true);
        map.put(b, false);

        Map<UUID, Boolean> back = codec.decode(codec.encode(map));
        assertEquals(2, back.size());
        assertTrue(back.get(a));
        assertFalse(back.get(b));
    }

    @Test
    public void decodeMalformedReturnsEmpty()
    {
        assertTrue(codec.decode("not json").isEmpty());
    }
}
