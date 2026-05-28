package com.waypointer.model;

/**
 * Per-category ordering of waypoints in the panel. A {@code null} mode on a {@link Category}
 * is equivalent to {@link #MANUAL} and is how legacy (pre-feature) JSON deserializes.
 */
public enum CategorySortMode
{
    MANUAL,
    NAME,
    DATE_ADDED
}
