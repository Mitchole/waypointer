package com.waypointer.model;

import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A category groups waypoints in the panel. The single Uncategorized sentinel is flagged. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class Category
{
    private UUID id;
    private String name;
    private int sortOrder;
    private boolean uncategorized;
    /** Optional sprite id for the category icon. {@code null} = no icon. */
    private Integer iconId;
    /** True if this category was shipped via {@code bundled-defaults.json}. Sorts after user-created. */
    private boolean bundled;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category c = (Category) o;
        return Objects.equals(id, c.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
