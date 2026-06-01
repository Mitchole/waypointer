package com.waypointer.ui;

import com.waypointer.model.Category;
import java.util.UUID;

// JComboBox item wrapping a Category. A sentinel item (null category) carries a plain label
// instead - used by CaptureForm for its "+ New category..." row. The combo box renders
// items via toString().
final class CategoryComboItem
{
    private final Category category;     // null for a sentinel item
    private final String sentinelLabel;  // non-null only for a sentinel item

    CategoryComboItem(Category category)
    {
        this.category = category;
        this.sentinelLabel = null;
    }

    private CategoryComboItem(String sentinelLabel)
    {
        this.category = null;
        this.sentinelLabel = sentinelLabel;
    }

    static CategoryComboItem sentinel(String label)
    {
        return new CategoryComboItem(label);
    }

    boolean isSentinel()
    {
        return category == null;
    }

    UUID id()
    {
        return category == null ? null : category.getId();
    }

    @Override
    public String toString()
    {
        return category == null ? sentinelLabel : category.getName();
    }
}
