package com.waypointer.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
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
    private boolean pinned;
    private Instant pinnedAt;   // null when not pinned
    private boolean bypassWildernessConfirm;

    /**
     * Name of the NPC this waypoint was captured from. {@code null} = not an NPC waypoint.
     * Drives the active-path NPC highlight. Kept out of the canonical constructor below so
     * existing positional call sites compile unchanged; set via {@link #setTargetNpcName}.
     */
    private String targetNpcName;

    public Waypoint(UUID id, String name, int packedWorldPoint, UUID categoryId, Integer iconId,
        String notes, Instant createdAt, int sortOrder, boolean pinned, Instant pinnedAt,
        boolean bypassWildernessConfirm)
    {
        this.id = id;
        this.name = name;
        this.packedWorldPoint = packedWorldPoint;
        this.categoryId = categoryId;
        this.iconId = iconId;
        this.notes = notes;
        this.createdAt = createdAt;
        this.sortOrder = sortOrder;
        this.pinned = pinned;
        this.pinnedAt = pinnedAt;
        this.bypassWildernessConfirm = bypassWildernessConfirm;
    }

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
