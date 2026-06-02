package com.waypointer.ui;

import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.service.RouteStore;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * Drag-to-reorder for route step rows. Payload is "step:&lt;uuid&gt;"; a drop on a target row moves
 * the dragged step to immediately before it via {@link RouteStore#reorderSteps}. Drag fires from
 * the row's grip handle after a 5px threshold so the row's edit/delete buttons still work. A fresh
 * instance is created per editor rebuild because it closes over the route id.
 */
final class RouteStepDragHandler
{
    private static final DataFlavor STR = DataFlavor.stringFlavor;
    private static final String STEP_PREFIX = "step:";
    // 5 px squared: how far the mouse must move before a press becomes a drag.
    private static final int DRAG_THRESHOLD_SQ = 25;

    private final RouteStore store;
    private final UUID routeId;

    RouteStepDragHandler(RouteStore store, UUID routeId)
    {
        this.store = store;
        this.routeId = routeId;
    }

    /** Wires {@code row} as a drop target and {@code dragHandle} as the drag source for {@code stepId}. */
    void attach(JComponent row, JComponent dragHandle, UUID stepId)
    {
        row.setTransferHandler(new TransferHandler()
        {
            @Override public int getSourceActions(JComponent c) { return MOVE; }

            @Override protected Transferable createTransferable(JComponent c)
            {
                return new StringSelection(STEP_PREFIX + stepId);
            }

            @Override public boolean canImport(TransferSupport s)
            {
                if (!s.isDataFlavorSupported(STR)) return false;
                String payload = readPayload(s.getTransferable());
                return payload != null && payload.startsWith(STEP_PREFIX);
            }

            @Override public boolean importData(TransferSupport s)
            {
                String payload = readPayload(s.getTransferable());
                if (payload == null || !payload.startsWith(STEP_PREFIX)) return false;
                UUID dragged = UUID.fromString(payload.substring(STEP_PREFIX.length()));
                if (dragged.equals(stepId)) return false;
                moveBefore(dragged, stepId);
                return true;
            }
        });

        DragGesture gesture = new DragGesture(row);
        dragHandle.addMouseListener(gesture);
        dragHandle.addMouseMotionListener(gesture);
    }

    // Move dragged to immediately before target, then push the new order to the store.
    private void moveBefore(UUID dragged, UUID target)
    {
        Route r = store.getRouteById(routeId);
        if (r == null) return;
        List<UUID> order = new ArrayList<>();
        for (RouteStep s : r.getSteps()) order.add(s.getId());
        int from = order.indexOf(dragged);
        if (from < 0) return;
        order.remove(from);
        int to = order.indexOf(target);
        if (to < 0) return;
        order.add(to, dragged);
        store.reorderSteps(routeId, order);
    }

    private static String readPayload(Transferable t)
    {
        if (t == null) return null;
        try { return (String) t.getTransferData(STR); }
        catch (UnsupportedFlavorException | IOException e) { return null; }
    }

    // Starts a drag from the handle once the mouse moves past the threshold; until then a press
    // is just a click so the grip does not swallow taps.
    private final class DragGesture extends MouseAdapter implements MouseMotionListener
    {
        private final JComponent dragSource;
        private Point pressPoint;
        private boolean dragStarted;

        DragGesture(JComponent dragSource) { this.dragSource = dragSource; }

        @Override public void mousePressed(MouseEvent e) { pressPoint = e.getPoint(); dragStarted = false; }

        @Override public void mouseReleased(MouseEvent e) { pressPoint = null; dragStarted = false; }

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
}
