package com.waypointer.model;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A category groups waypoints in the panel. The single Uncategorized sentinel is flagged. */
@Getter
@Setter
@NoArgsConstructor
public final class Category
{
    private UUID id;
    private String name;
    private int sortOrder;
    private boolean uncategorized;
    /** Optional sprite id for the category icon. {@code null} = no icon. */
    private Integer iconId;
    /** True if this category was shipped as a preset. Sorts after user-created categories. */
    private boolean bundled;
    /**
     * Sort mode for waypoints within this category. {@code null} is equivalent to
     * {@link CategorySortMode#MANUAL} and is how legacy JSON deserializes. Not part of
     * the 6-arg constructor below so that the 23 existing positional call sites compile
     * unchanged; set via {@link #setSortMode(CategorySortMode)}.
     */
    private CategorySortMode sortMode;
    /**
     * Optional accent colour as a packed RGB int. {@code null} = no accent. Like
     * {@link #sortMode}, kept out of the 6-arg constructor so existing positional call
     * sites compile unchanged; set via {@link #setColor(Integer)}.
     */
    private Integer color;

    public Category(UUID id, String name, int sortOrder, boolean uncategorized,
        Integer iconId, boolean bundled)
    {
        this.id = id;
        this.name = name;
        this.sortOrder = sortOrder;
        this.uncategorized = uncategorized;
        this.iconId = iconId;
        this.bundled = bundled;
    }

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
