package com.waypointer.ui;

/**
 * Surface used by DragAndDropHandler to push indicator state onto a target component
 * during a drag. WaypointRow implements this directly; CategorySection supplies a lambda
 * that targets its header row panel.
 */
@FunctionalInterface
interface DropIndicatable
{
    void setDropIndicator(DropIndicatorMode mode);

    /** No-op default for wiring sites that don't have a real target yet. */
    DropIndicatable NO_OP = mode -> { };
}
