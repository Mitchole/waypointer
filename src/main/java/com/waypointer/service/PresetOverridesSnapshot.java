package com.waypointer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Materialized snapshot of dev-mode preset overrides.
// A category in byCategory fully replaces the bundled list for that category.
// Deletions apply last for categories not in byCategory.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PresetOverridesSnapshot
{
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int version = CURRENT_SCHEMA_VERSION;
    private Map<String, CategoryOverride> byCategory = new LinkedHashMap<>();
    private List<CategoryOverride> addedCategories = new ArrayList<>();
    private List<String> deletedCategories = new ArrayList<>();
    private List<DeletedWaypoint> deletedWaypoints = new ArrayList<>();

    public static PresetOverridesSnapshot empty()
    {
        return new PresetOverridesSnapshot(
            CURRENT_SCHEMA_VERSION,
            new LinkedHashMap<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>());
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class CategoryOverride
    {
        private String category;
        private String description;
        private Integer icon;
        private List<Waypoint> waypoints = new ArrayList<>();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Waypoint
    {
        private String name;
        private String description;
        private int x, y, plane;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class DeletedWaypoint
    {
        private String category;
        private String name;
        private int x, y, plane;
    }
}
