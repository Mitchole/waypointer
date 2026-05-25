package com.waypointer.ui;

/**
 * Visual state of a drag-and-drop target while a drag is hovering over it. NONE is the
 * resting state; TINT alone signals "this category will receive the dragged waypoint";
 * BORDER_AND_TINT signals "drop will insert before this row".
 */
enum DropIndicatorMode
{
    NONE, TINT, BORDER_AND_TINT
}
