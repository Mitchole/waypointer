package com.waypointer.codec;

import com.waypointer.model.Library;

/**
 * Result of decoding a library: the cleaned {@link Library} plus how many entries the sanitizer
 * dropped (missing required fields). Lets the import UI tell the user a pasted code contained
 * entries that could not be read.
 */
public final class DecodeReport
{
    public final Library library;
    public final int droppedWaypoints;
    public final int droppedCategories;

    public DecodeReport(Library library, int droppedWaypoints, int droppedCategories)
    {
        this.library = library;
        this.droppedWaypoints = droppedWaypoints;
        this.droppedCategories = droppedCategories;
    }

    public int totalDropped() { return droppedWaypoints + droppedCategories; }
}
