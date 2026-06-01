package com.waypointer.model.route;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Persisted aggregate of all routes, account-global (not per-profile). */
@Getter
@Setter
public final class RouteLibrary
{
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private List<Route> routes = new ArrayList<>();
}
