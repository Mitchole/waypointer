/*
 * Copyright (c) 2026, Dazuzi
 * Lifted (read-only subset) from the Chart Plotter plugin under BSD 2-Clause.
 * THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 */
package com.waypointer.sailing;

import com.waypointer.sailing.WaterCollisionData.Chunk;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Parses the bundled text collision format: a `data <version>` header line, then per-chunk
 *  lines of `cx cy blockedHex` (all tiles known) or `cx cy knownHex blockedHex`. */
@Slf4j
public final class WaterCollisionCodec
{
    private static final int USHORT = 0xffff;

    private WaterCollisionCodec() {}

    public static Map<Long, Chunk> readText(InputStream src)
    {
        Map<Long, Chunk> data = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(src, StandardCharsets.UTF_8)))
        {
            String s;
            while ((s = in.readLine()) != null)
            {
                String[] p = s.trim().split("\\s+");
                if (p.length == 2 && "data".equals(p[0])) continue;
                if (p.length != 3 && p.length != 4) continue;
                int cx = Integer.parseInt(p[0]);
                int cy = Integer.parseInt(p[1]);
                if (cx < 0 || cx > USHORT || cy < 0 || cy > USHORT) continue;
                long known = p.length == 3 ? -1L : Long.parseUnsignedLong(p[2], 16);
                long blocked = Long.parseUnsignedLong(p[p.length - 1], 16);
                if (known == 0L) continue;
                data.put(WaterCollisionData.key(cx, cy), new Chunk(known, blocked & known));
            }
        }
        catch (Exception e)
        {
            log.warn("Failed to parse bundled water collision data", e);
            return null;
        }
        return data;
    }
}
