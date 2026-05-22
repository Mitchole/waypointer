package com.waypointer.preset;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A curated set of waypoints, as read from the bundled preset catalog. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class Preset
{
    private String category;
    private String description;
    private Integer icon;            // RuneLite sprite id; null = no icon
    private List<PresetWaypoint> waypoints;
}
