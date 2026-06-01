package com.waypointer.model.route;

import com.waypointer.model.WorldPointPacker;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One step in a {@link Route}. A single flat class with an enum discriminator -- deliberately
 * not a polymorphic hierarchy, so Gson needs no custom adapter.
 */
@Getter
@Setter
@NoArgsConstructor
public final class RouteStep
{
    private UUID id;
    private StepType type;
    private String label;                 // instruction (MANUAL) or location name (WAYPOINT)
    private int packedWorldPoint;         // WAYPOINT only; WorldPointPacker.UNDEFINED otherwise
    private UUID sourceWaypointId;        // nullable provenance link when captured from the library
    private Integer iconId;               // nullable
    private String boxText;               // nullable; in-game box overrides label when set

    public RouteStep(UUID id, StepType type, String label, int packedWorldPoint,
        UUID sourceWaypointId, Integer iconId)
    {
        this.id = id;
        this.type = type;
        this.label = label;
        this.packedWorldPoint = packedWorldPoint;
        this.sourceWaypointId = sourceWaypointId;
        this.iconId = iconId;
    }

    public static RouteStep waypoint(int packedWorldPoint, String label)
    {
        return new RouteStep(UUID.randomUUID(), StepType.WAYPOINT, label, packedWorldPoint, null, null);
    }

    public static RouteStep manual(String label)
    {
        return new RouteStep(UUID.randomUUID(), StepType.MANUAL, label,
            WorldPointPacker.UNDEFINED, null, null);
    }

    /** Overlay text: the explicit in-game box text if set, else the sidebar label. */
    public String boxTextOrLabel()
    {
        return boxText == null || boxText.isEmpty() ? label : boxText;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RouteStep)) return false;
        return Objects.equals(id, ((RouteStep) o).id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
