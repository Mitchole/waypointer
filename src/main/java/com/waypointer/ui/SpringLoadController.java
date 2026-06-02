package com.waypointer.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.Timer;

/**
 * Spring-load auto-expand during a drag: hovering a collapsed category header schedules a
 * transient expand after a short delay, and drags that drift away or end revert it. Extracted
 * from {@link DragAndDropHandler} so the transfer-handler code stays focused on drop routing.
 * Per-cycle: a fresh controller is created with each handler on panel rebuild.
 */
final class SpringLoadController
{
    private static final int AUTO_EXPAND_DELAY_MS = 600;

    private final AutoExpandController autoExpand = new AutoExpandController();
    private final Map<UUID, CategorySection> sectionsByCategoryId = new HashMap<>();
    private Timer pendingExpandTimer;
    private CategorySection pendingExpandTarget;

    void register(UUID categoryId, CategorySection section)
    {
        sectionsByCategoryId.put(categoryId, section);
    }

    /** Hover bookkeeping for a target in {@code categoryId}; reverts sections drifted out of. */
    void onHover(UUID categoryId)
    {
        revert(autoExpand.onHover(categoryId));
    }

    void scheduleExpand(CategorySection section)
    {
        if (pendingExpandTarget == section) return;
        cancelExpand();
        pendingExpandTarget = section;
        pendingExpandTimer = new Timer(AUTO_EXPAND_DELAY_MS, e ->
        {
            section.setExpandedTransient(true);
            autoExpand.recordTransientExpand(section.getCategoryId());
            pendingExpandTimer = null;
            pendingExpandTarget = null;
        });
        pendingExpandTimer.setRepeats(false);
        pendingExpandTimer.start();
    }

    void cancelExpand()
    {
        if (pendingExpandTimer != null)
        {
            pendingExpandTimer.stop();
            pendingExpandTimer = null;
        }
        pendingExpandTarget = null;
    }

    void onDragEnd()
    {
        cancelExpand();
        revert(autoExpand.onDragEnd());
    }

    void resolveOnDrop(UUID destCategoryId)
    {
        cancelExpand();
        AutoExpandController.DropResolution r = autoExpand.onDropAt(destCategoryId);
        revert(r.getToRevert());
        if (r.getToConfirm() != null)
        {
            CategorySection s = sectionsByCategoryId.get(r.getToConfirm());
            if (s != null) s.confirmTransientExpand();
        }
    }

    private void revert(Set<UUID> ids)
    {
        for (UUID id : ids)
        {
            CategorySection s = sectionsByCategoryId.get(id);
            if (s != null) s.setExpandedTransient(false);
        }
    }
}
