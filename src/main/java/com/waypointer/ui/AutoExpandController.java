package com.waypointer.ui;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-drag state for sections auto-expanded by the spring-loaded hover logic.
 * Pure data: no Swing, no timers. The owning {@link DragAndDropHandler} drives
 * timing; this class decides which sections to revert vs confirm on each
 * drag event.
 */
final class AutoExpandController
{
    private final Set<UUID> autoExpanded = new LinkedHashSet<>();

    void recordTransientExpand(UUID id)
    {
        autoExpanded.add(id);
    }

    boolean isTransientlyExpanded(UUID id)
    {
        return autoExpanded.contains(id);
    }
}
