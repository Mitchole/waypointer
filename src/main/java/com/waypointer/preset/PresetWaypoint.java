package com.waypointer.preset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One curated location inside a {@link Preset}. Coordinates are plain world tile values. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PresetWaypoint
{
    private String name;
    private String description;
    private int x;
    private int y;
    private int plane;
}
