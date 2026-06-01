package com.waypointer.ui;

import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.service.WaypointStore;
import java.awt.Window;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class BulkSelectControllerTest
{
    // No-op host: counts rebuilds, never touches a real window.
    private static final class CountingHost implements BulkSelectController.Host
    {
        int rebuilds;
        @Override public void rebuild() { rebuilds++; }
        @Override public void revalidateAndRepaint() { }
        @Override public Window windowAncestor() { return null; }
    }

    private static WaypointStore freshStore()
    {
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        return store;
    }

    private static BulkSelectController newController(WaypointStore store, CountingHost host)
    {
        return new BulkSelectController(
            store,
            Toasts.NO_OP,
            mock(WaypointShareCodec.class),
            mock(LibraryJsonCodec.class),
            host);
    }

    @Test
    public void enterAndExitSelectModeFlipTheFlag()
    {
        WaypointStore store = freshStore();
        BulkSelectController c = newController(store, new CountingHost());

        assertFalse(c.isSelectMode());
        c.enterSelectMode();
        assertTrue(c.isSelectMode());
        c.exitSelectMode();
        assertFalse(c.isSelectMode());
    }

    @Test
    public void toggleSelectModeFlipsTheFlag()
    {
        WaypointStore store = freshStore();
        BulkSelectController c = newController(store, new CountingHost());

        assertFalse(c.isSelectMode());
        c.toggleSelectMode();
        assertTrue(c.isSelectMode());
        c.toggleSelectMode();
        assertFalse(c.isSelectMode());
    }

    @Test
    public void bulkDeleteRemovesSelectedAndClearsSelection()
    {
        WaypointStore store = freshStore();
        Category cat = store.getUncategorized();
        Waypoint a = store.createWaypoint(1, "A", cat.getId());
        Waypoint b = store.createWaypoint(2, "B", cat.getId());

        CountingHost host = new CountingHost();
        BulkSelectController c = newController(store, host);
        c.toggleSelectMode();
        c.onRowSelectClicked(a, false);
        c.onRowSelectClicked(b, false);
        assertEquals(2, c.selection().size());

        c.bulkDeleteForTest();

        assertTrue("selection cleared after delete", c.selection().isEmpty());
        assertTrue("both waypoints removed from the store",
            store.getWaypointsInCategory(cat.getId()).isEmpty());
    }

    @Test
    public void bulkMoveReassignsCategoryAndClearsSelection()
    {
        WaypointStore store = freshStore();
        Category src = store.getUncategorized();
        Category dst = store.createCategory("Dest");
        Waypoint a = store.createWaypoint(1, "A", src.getId());

        CountingHost host = new CountingHost();
        BulkSelectController c = newController(store, host);
        c.toggleSelectMode();
        c.onRowSelectClicked(a, false);

        c.bulkMoveToForTest(dst.getId());

        assertTrue("selection cleared after move", c.selection().isEmpty());
        assertEquals(1, store.getWaypointsInCategory(dst.getId()).size());
        assertTrue(store.getWaypointsInCategory(src.getId()).isEmpty());
    }
}
