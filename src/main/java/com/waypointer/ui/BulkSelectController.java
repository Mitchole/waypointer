package com.waypointer.ui;

import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.service.WaypointStore;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Owns the panel's bulk select mode: the {@link BulkSelection} model, the select-mode flag, the
 * shift-range anchor, and the {@link BulkActionBar}. Splits the selection orchestration out of
 * {@link WaypointerPanel}; every Swing side effect routes back through {@link Host}.
 */
final class BulkSelectController
{
    /** Hooks back into the panel for the side effects the controller cannot perform itself. */
    interface Host
    {
        /** Request a (coalesced) body rebuild so rows re-render with current checkbox state. */
        void rebuild();

        /** Revalidate + repaint the panel after the action bar's visibility or size changes. */
        void revalidateAndRepaint();

        /** Window ancestor used as the export dialog's owner; may be null in tests. */
        Window windowAncestor();
    }

    private final WaypointStore store;
    private final Toasts toasts;
    private final WaypointShareCodec shareCodec;
    private final Host host;
    private final BulkActionBar bar;

    private final BulkSelection selection = new BulkSelection();
    private boolean selectMode = false;
    private UUID lastClickedId;
    // Ordered ids of the waypoints currently rendered in real categories (Pinned excluded),
    // refreshed every render pass so shift-range select honours the active filter and sort.
    private final List<UUID> visibleOrderedIds = new ArrayList<>();

    BulkSelectController(WaypointStore store, Toasts toasts,
        WaypointShareCodec shareCodec, Host host)
    {
        this.store = store;
        this.toasts = toasts;
        this.shareCodec = shareCodec;
        this.host = host;
        this.bar = new BulkActionBar(
            this::exitSelectMode,
            store::getCategoriesOrdered,
            this::bulkMoveTo,
            this::bulkDelete,
            this::bulkExport);
        bar.setVisible(false);
    }

    /** The SOUTH-docked action bar; added to the panel by {@link WaypointerPanel}. */
    BulkActionBar bar() { return bar; }

    boolean isSelectMode() { return selectMode; }

    BulkSelection selection() { return selection; }

    /** Replace the visible-row order used for shift-range selection. Called at the end of each rebuild. */
    void setVisibleOrderedIds(List<UUID> ids)
    {
        visibleOrderedIds.clear();
        visibleOrderedIds.addAll(ids);
    }

    // A row was clicked in select mode: shift extends the range from the last click; a plain
    // click toggles. Keyed by id so it is filter/sort independent.
    void onRowSelectClicked(Waypoint w, boolean shift)
    {
        UUID id = w.getId();
        if (shift && lastClickedId != null)
        {
            selection.selectRange(visibleOrderedIds, lastClickedId, id);
        }
        else
        {
            selection.toggle(id);
        }
        lastClickedId = id;
        afterSelectionChanged();
    }

    void onHeaderSelectToggle(List<UUID> ids, boolean select)
    {
        selection.setCategory(ids, select);
        afterSelectionChanged();
    }

    /** Enter select mode. Triggered by the "Select multiple" right-click entry. */
    void enterSelectMode() { setSelectMode(true); }

    /** Exit select mode. Triggered by the action bar's Done button. */
    void exitSelectMode() { setSelectMode(false); }

    void toggleSelectMode() { setSelectMode(!selectMode); }

    private void setSelectMode(boolean on)
    {
        if (selectMode == on) return;
        selectMode = on;
        if (!selectMode)
        {
            selection.clear();
            lastClickedId = null;
        }
        host.rebuild();
        refreshBulkBar();
    }

    void refreshBulkBar()
    {
        bar.setVisible(selectMode);
        if (selectMode)
        {
            bar.setCount(selection.size());
            bar.setActionsEnabled(!selection.isEmpty());
        }
        host.revalidateAndRepaint();
    }

    private void bulkMoveTo(UUID targetId)
    {
        if (selection.isEmpty()) return;
        Category target = store.getCategoryById(targetId);
        int n = selection.size();
        store.moveWaypointsToCategory(selection.ids(), targetId);
        selection.clear();
        lastClickedId = null;
        afterSelectionChanged();
        toasts.show("Moved " + n + " to " + (target == null ? "category" : target.getName()),
            null, null);
    }

    private void bulkDelete()
    {
        if (selection.isEmpty()) return;
        int n = selection.size();
        store.deleteWaypoints(selection.ids());
        selection.clear();
        lastClickedId = null;
        afterSelectionChanged();
        toasts.show(n + " deleted", "Undo", store::undoLast);
    }

    private void bulkExport()
    {
        if (selection.isEmpty()) return;
        Window owner = host.windowAncestor();
        new ExportPickerDialog(owner, store, shareCodec, toasts,
            selection.ids()).setVisible(true);
        // Selection is non-destructive for export; leave it intact.
    }

    private void afterSelectionChanged()
    {
        host.rebuild();
        refreshBulkBar();
    }

    // Test seams: exercise the non-modal action paths without driving the popup menu / dialog.
    void bulkDeleteForTest() { bulkDelete(); }
    void bulkMoveToForTest(UUID targetId) { bulkMoveTo(targetId); }
}
