package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import com.waypointer.service.WaypointStore;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import lombok.extern.slf4j.Slf4j;

// Swing drag-and-drop on waypoint rows and category headers. Transfer payloads are
// string-encoded ("waypoint:<uuid>" or "category:<uuid>"). Drag fires from mouseDragged
// after a 5px threshold so single clicks pass through to mouseClicked. Instantiated fresh
// per panel rebuild because onChange captures the panel's rebuild method.
@Slf4j
public class DragAndDropHandler
{
    static final DataFlavor STR = DataFlavor.stringFlavor;
    public static final String WAYPOINT_PREFIX = "waypoint:";
    public static final String CATEGORY_PREFIX = "category:";

    // 5 px squared: how far the mouse must move before a press becomes a drag.
    private static final int DRAG_THRESHOLD_SQ = 25;

    private final WaypointStore store;
    private final Runnable onChange;

    // Closures that clear indicator state on each attached target. Run on every
    // mouseReleased so an aborted drag (ESC, cursor leaves the window) doesn't
    // leave a row stuck in a highlighted state.
    private final List<Runnable> clearActions = new CopyOnWriteArrayList<>();

    // The indicator that's currently visually active. Tracked so canImport on a new
    // target can clear the old one before lighting itself up.
    private DropIndicatable activeIndicator;

    public DragAndDropHandler(WaypointStore store, Runnable onChange)
    {
        this.store = store;
        this.onChange = onChange;
    }

    // Row is the drop target; drag only fires from dragHandle so the row body's click-to-expand
    // still works.
    public void attachWaypointRow(JComponent row, JComponent dragHandle,
        DropIndicatable indicatable, UUID waypointId, UUID categoryId)
    {
        row.setTransferHandler(new TransferHandler()
        {
            @Override public int getSourceActions(JComponent c) { return MOVE; }

            @Override protected Transferable createTransferable(JComponent c)
            {
                return new java.awt.datatransfer.StringSelection(WAYPOINT_PREFIX + waypointId);
            }

            @Override public boolean canImport(TransferSupport s)
            {
                if (!s.isDataFlavorSupported(STR)) return false;
                String payload = readPayload(s.getTransferable());
                if (isSelfDrop(payload, TargetKind.WAYPOINT_ROW, waypointId))
                {
                    if (activeIndicator == indicatable)
                    {
                        indicatable.setDropIndicator(DropIndicatorMode.NONE);
                        activeIndicator = null;
                    }
                    return false;
                }
                onHoverEnter(indicatable, decideMode(payload, TargetKind.WAYPOINT_ROW));
                return true;
            }

            @Override public boolean importData(TransferSupport s)
            {
                String payload = read(s);
                if (payload == null) return false;
                if (payload.startsWith(WAYPOINT_PREFIX))
                {
                    UUID dragged = UUID.fromString(payload.substring(WAYPOINT_PREFIX.length()));
                    if (dragged.equals(waypointId)) return false;
                    Waypoint draggedW = store.getWaypointById(dragged);
                    if (draggedW == null) return false;
                    if (!draggedW.getCategoryId().equals(categoryId))
                    {
                        store.moveWaypointToCategory(dragged, categoryId);
                    }
                    swapWithinCategory(categoryId, dragged, waypointId);
                    onChange.run();
                    return true;
                }
                return false;
            }
        });
        clearActions.add(() -> indicatable.setDropIndicator(DropIndicatorMode.NONE));
        DragGestureListener gesture = new DragGestureListener(row);
        dragHandle.addMouseListener(gesture);
        dragHandle.addMouseMotionListener(gesture);
    }

    public void attachCategoryHeader(JComponent dragTarget, DropIndicatable indicatable, UUID categoryId)
    {
        dragTarget.setTransferHandler(new TransferHandler()
        {
            @Override public int getSourceActions(JComponent c) { return MOVE; }

            @Override protected Transferable createTransferable(JComponent c)
            {
                return new java.awt.datatransfer.StringSelection(CATEGORY_PREFIX + categoryId);
            }

            @Override public boolean canImport(TransferSupport s)
            {
                if (!s.isDataFlavorSupported(STR)) return false;
                String payload = readPayload(s.getTransferable());
                if (isSelfDrop(payload, TargetKind.CATEGORY_HEADER, categoryId))
                {
                    if (activeIndicator == indicatable)
                    {
                        indicatable.setDropIndicator(DropIndicatorMode.NONE);
                        activeIndicator = null;
                    }
                    return false;
                }
                onHoverEnter(indicatable, decideMode(payload, TargetKind.CATEGORY_HEADER));
                return true;
            }

            @Override public boolean importData(TransferSupport s)
            {
                String payload = read(s);
                if (payload == null) return false;
                if (payload.startsWith(WAYPOINT_PREFIX))
                {
                    UUID dragged = UUID.fromString(payload.substring(WAYPOINT_PREFIX.length()));
                    store.moveWaypointToCategory(dragged, categoryId);
                    onChange.run();
                    return true;
                }
                if (payload.startsWith(CATEGORY_PREFIX))
                {
                    UUID dragged = UUID.fromString(payload.substring(CATEGORY_PREFIX.length()));
                    if (dragged.equals(categoryId)) return false;
                    swapCategoryOrder(dragged, categoryId);
                    onChange.run();
                    return true;
                }
                return false;
            }
        });
        clearActions.add(() -> indicatable.setDropIndicator(DropIndicatorMode.NONE));
        DragGestureListener gesture = new DragGestureListener(dragTarget);
        dragTarget.addMouseListener(gesture);
        dragTarget.addMouseMotionListener(gesture);
    }

    private final class DragGestureListener extends MouseAdapter
        implements MouseMotionListener
    {
        private final JComponent dragSource;
        private Point pressPoint;
        private boolean dragStarted;

        DragGestureListener(JComponent dragSource)
        {
            this.dragSource = dragSource;
        }

        @Override public void mousePressed(MouseEvent e)
        {
            pressPoint = e.getPoint();
            dragStarted = false;
        }

        @Override public void mouseReleased(MouseEvent e)
        {
            pressPoint = null;
            dragStarted = false;
            clearAllIndicators();
        }

        @Override public void mouseDragged(MouseEvent e)
        {
            if (pressPoint == null || dragStarted) return;
            int dx = e.getX() - pressPoint.x;
            int dy = e.getY() - pressPoint.y;
            if (dx * dx + dy * dy >= DRAG_THRESHOLD_SQ)
            {
                dragStarted = true;
                TransferHandler th = dragSource.getTransferHandler();
                if (th != null) th.exportAsDrag(dragSource, e, TransferHandler.MOVE);
            }
        }

        @Override public void mouseMoved(MouseEvent e) { }
    }

    private static String read(TransferHandler.TransferSupport s)
    {
        try
        {
            return (String) s.getTransferable().getTransferData(STR);
        }
        catch (UnsupportedFlavorException | IOException e)
        {
            return null;
        }
    }

    // Called from canImport on every target the drag passes over. Clears the previous
    // active indicator (if it's a different target) and sets the new one.
    private void onHoverEnter(DropIndicatable indicatable, DropIndicatorMode mode)
    {
        if (activeIndicator != null && activeIndicator != indicatable)
        {
            activeIndicator.setDropIndicator(DropIndicatorMode.NONE);
        }
        indicatable.setDropIndicator(mode);
        activeIndicator = indicatable;
    }

    private void clearAllIndicators()
    {
        for (Runnable r : clearActions) r.run();
        activeIndicator = null;
    }

    private static String readPayload(Transferable t)
    {
        if (t == null) return null;
        try { return (String) t.getTransferData(STR); }
        catch (Exception e) { return null; }
    }

    private static boolean isSelfDrop(String payload, TargetKind kind, UUID selfId)
    {
        if (payload == null || selfId == null) return false;
        if (kind == TargetKind.WAYPOINT_ROW && payload.startsWith(WAYPOINT_PREFIX))
        {
            return payload.substring(WAYPOINT_PREFIX.length()).equals(selfId.toString());
        }
        if (kind == TargetKind.CATEGORY_HEADER && payload.startsWith(CATEGORY_PREFIX))
        {
            return payload.substring(CATEGORY_PREFIX.length()).equals(selfId.toString());
        }
        return false;
    }

    private void swapWithinCategory(UUID categoryId, UUID dragged, UUID target)
    {
        List<UUID> order = new ArrayList<>();
        for (Waypoint w : store.getWaypointsInCategory(categoryId)) order.add(w.getId());
        List<UUID> moved = move(order, dragged, target);
        if (moved == null) return;
        store.reorderWithinCategory(categoryId, moved);
    }

    private void swapCategoryOrder(UUID dragged, UUID target)
    {
        List<UUID> order = new ArrayList<>();
        for (com.waypointer.model.Category c : store.getCategoriesOrdered()) order.add(c.getId());
        List<UUID> moved = move(order, dragged, target);
        if (moved == null) return;
        store.reorderCategories(moved);
    }

    /** Which kind of component the drag is hovering over. */
    enum TargetKind { WAYPOINT_ROW, CATEGORY_HEADER }

    /**
     * Decides which indicator visual to apply for the (payload, target) combination.
     * Pure function: the per-target rule lives here and only here.
     */
    static DropIndicatorMode decideMode(String payload, TargetKind kind)
    {
        if (payload == null) return DropIndicatorMode.NONE;
        boolean isWaypoint = payload.startsWith(WAYPOINT_PREFIX);
        boolean isCategory = payload.startsWith(CATEGORY_PREFIX);
        if (kind == TargetKind.WAYPOINT_ROW)
        {
            return isWaypoint ? DropIndicatorMode.BORDER_AND_TINT : DropIndicatorMode.NONE;
        }
        // CATEGORY_HEADER
        if (isCategory) return DropIndicatorMode.BORDER_AND_TINT;
        if (isWaypoint) return DropIndicatorMode.TINT;
        return DropIndicatorMode.NONE;
    }

    // Moves dragged to immediately before target. Returns null if either id is missing or
    // dragged == target (caller treats null as no-op).
    static List<UUID> move(List<UUID> order, UUID dragged, UUID target)
    {
        int from = order.indexOf(dragged);
        int to = order.indexOf(target);
        if (from < 0 || to < 0 || from == to) return null;
        List<UUID> out = new ArrayList<>(order);
        UUID m = out.remove(from);
        int insertAt = out.indexOf(target);
        if (insertAt < 0) return null;
        out.add(insertAt, m);
        return out;
    }
}
