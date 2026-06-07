package com.waypointer.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Materialized snapshot of dev-mode landmark overrides.
// A type listed in byType fully replaces the bundled list for that type.
// deletions applies last for types not in byType.
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class LandmarkOverridesSnapshot
{
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int version = CURRENT_SCHEMA_VERSION;
    private Map<String, TypeOverride> byType = new LinkedHashMap<>();
    private List<DeletedEntry> deletions = new ArrayList<>();

    public static LandmarkOverridesSnapshot empty()
    {
        return new LandmarkOverridesSnapshot(
            CURRENT_SCHEMA_VERSION, new LinkedHashMap<>(), new ArrayList<>());
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class TypeOverride
    {
        private List<Entry> entries = new ArrayList<>();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Entry
    {
        private String name;
        private int x1, y1, x2, y2, plane;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class DeletedEntry
    {
        private String type;
        private String name;
        private int x1, y1, x2, y2, plane;
    }
}
