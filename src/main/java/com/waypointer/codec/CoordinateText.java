package com.waypointer.codec;

import com.waypointer.model.WorldPointPacker;
import java.math.BigInteger;

/**
 * Reads and writes typed tile coordinates such as {@code 2301, 3690} or {@code 2301, 3690, 1}.
 * Numbers may be separated by commas, whitespace, or both. The plane defaults to 0.
 */
public final class CoordinateText
{
    private static final int MAX_XY = 16383;
    private static final int MAX_PLANE = 3;
    private static final BigInteger BIG_MAX_XY = BigInteger.valueOf(MAX_XY);
    private static final BigInteger BIG_MAX_PLANE = BigInteger.valueOf(MAX_PLANE);

    private static final String FORMAT_MESSAGE = "Enter coordinates as x, y or x, y, plane.";
    private static final String RANGE_MESSAGE = "x and y must be between 0 and 16383.";
    private static final String PLANE_MESSAGE = "Plane must be between 0 and 3.";

    private CoordinateText() {}

    /**
     * Parses typed coordinates into a packed tile.
     *
     * @throws IllegalArgumentException with a message fit to show the user
     */
    public static int parse(String text)
    {
        String trimmed = text == null ? "" : text.trim();
        String[] parts = trimmed.split("[\\s,]+", -1);
        if (parts.length < 2 || parts.length > 3)
        {
            throw new IllegalArgumentException(FORMAT_MESSAGE);
        }
        BigInteger[] n = new BigInteger[parts.length];
        for (int i = 0; i < parts.length; i++)
        {
            if (!parts[i].matches("-?\\d+"))
            {
                throw new IllegalArgumentException(FORMAT_MESSAGE);
            }
            n[i] = new BigInteger(parts[i]);
        }
        return parse(n[0], n[1], n.length == 3 ? n[2] : BigInteger.ZERO);
    }

    /**
     * Range-checks already split numbers and packs them.
     *
     * @throws IllegalArgumentException with a message fit to show the user
     */
    public static int parse(BigInteger x, BigInteger y, BigInteger plane)
    {
        if (!inRange(x, BIG_MAX_XY) || !inRange(y, BIG_MAX_XY))
        {
            throw new IllegalArgumentException(RANGE_MESSAGE);
        }
        if (!inRange(plane, BIG_MAX_PLANE))
        {
            throw new IllegalArgumentException(PLANE_MESSAGE);
        }
        return WorldPointPacker.pack(x.intValue(), y.intValue(), plane.intValue());
    }

    /** Formats a packed tile as {@code x, y}, with {@code , plane} appended above plane 0. */
    public static String format(int packed)
    {
        String xy = WorldPointPacker.getX(packed) + ", " + WorldPointPacker.getY(packed);
        int plane = WorldPointPacker.getPlane(packed);
        return plane == 0 ? xy : xy + ", " + plane;
    }

    private static boolean inRange(BigInteger v, BigInteger max)
    {
        return v.signum() >= 0 && v.compareTo(max) <= 0;
    }
}
