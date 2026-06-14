package com.waypointer.model.route;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An ordered list of steps the plugin guides through one at a time. */
@Getter
@Setter
@NoArgsConstructor
public final class Route
{
    private UUID id;
    private String name;
    private List<RouteStep> steps = new ArrayList<>();
    private boolean repeating;
    private Instant createdAt;
    private int sortOrder;

    public Route(UUID id, String name, List<RouteStep> steps, boolean repeating,
        Instant createdAt, int sortOrder)
    {
        this.id = id;
        this.name = name;
        this.steps = steps == null ? new ArrayList<>() : new ArrayList<>(steps);
        this.repeating = repeating;
        this.createdAt = createdAt;
        this.sortOrder = sortOrder;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Route)) return false;
        return Objects.equals(id, ((Route) o).id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
