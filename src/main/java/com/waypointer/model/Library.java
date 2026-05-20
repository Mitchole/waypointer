package com.waypointer.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** In-memory aggregate of categories and waypoints. */
@Getter
@Setter
public final class Library
{
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private List<Category> categories = new ArrayList<>();
    private List<Waypoint> waypoints = new ArrayList<>();
}
