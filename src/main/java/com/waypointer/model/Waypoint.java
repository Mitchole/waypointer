package com.waypointer.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A saved location. Coordinates use shortest-path's packed-int format so we can pass them
 * through PluginMessage without re-packing on the hot path.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class Waypoint
{
    private UUID id;
    private String name;
    private int packedWorldPoint;
    private UUID categoryId;
    private Integer iconId;     // null = no icon
    private String notes;       // never null; "" when empty
    private Instant createdAt;
    private int sortOrder;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Waypoint)) return false;
        return Objects.equals(id, ((Waypoint) o).id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
