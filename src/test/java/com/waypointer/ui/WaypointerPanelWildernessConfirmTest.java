package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointStore;
import java.awt.Component;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

public class WaypointerPanelWildernessConfirmTest
{
    private WaypointStore store;
    private WaypointerConfig config;
    private Waypoint wildWp;
    private Waypoint safeWp;

    @Before
    public void setUp()
    {
        store = new WaypointStore();
        store.bootstrap(new Library());
        config = mock(WaypointerConfig.class);
        when(config.confirmBeforeWildernessPlay()).thenReturn(true);

        wildWp = store.createWaypoint(
            WorldPointPacker.pack(3093, 3525, 0), "Edgeville lever", store.getUncategorized().getId());
        safeWp = store.createWaypoint(
            WorldPointPacker.pack(3200, 3300, 0), "Varrock bank", store.getUncategorized().getId());
    }

    private static WildernessConfirmGate stubGate(boolean proceed, boolean dontAsk)
    {
        WildernessConfirmGate g = mock(WildernessConfirmGate.class);
        when(g.prompt(nullable(Component.class), any(Waypoint.class)))
            .thenReturn(new WildernessConfirmGate.Result(proceed, dontAsk));
        return g;
    }

    @Test
    public void configOffSkipsGateForWildTile()
    {
        when(config.confirmBeforeWildernessPlay()).thenReturn(false);
        WildernessConfirmGate gate = stubGate(false, false);
        assertTrue(WildernessConfirmGuard.shouldProceed(wildWp, config, gate, null, store));
        verifyNoInteractions(gate);
    }

    @Test
    public void safeTileSkipsGate()
    {
        WildernessConfirmGate gate = stubGate(false, false);
        assertTrue(WildernessConfirmGuard.shouldProceed(safeWp, config, gate, null, store));
        verifyNoInteractions(gate);
    }

    @Test
    public void bypassFlagOnWaypointSkipsGate()
    {
        store.setWaypointBypassWildernessConfirm(wildWp.getId(), true);
        WildernessConfirmGate gate = stubGate(false, false);
        assertTrue(WildernessConfirmGuard.shouldProceed(wildWp, config, gate, null, store));
        verifyNoInteractions(gate);
    }

    @Test
    public void gateProceedsWithoutDontAsk_PathRequestedNoBypassStored()
    {
        WildernessConfirmGate gate = stubGate(true, false);
        assertTrue(WildernessConfirmGuard.shouldProceed(wildWp, config, gate, null, store));
        assertFalse(store.getWaypointById(wildWp.getId()).isBypassWildernessConfirm());
    }

    @Test
    public void gateProceedsWithDontAsk_BypassStored()
    {
        WildernessConfirmGate gate = stubGate(true, true);
        assertTrue(WildernessConfirmGuard.shouldProceed(wildWp, config, gate, null, store));
        assertTrue(store.getWaypointById(wildWp.getId()).isBypassWildernessConfirm());
    }

    @Test
    public void gateCancels_ReturnsFalse_NoBypassStored()
    {
        WildernessConfirmGate gate = stubGate(false, false);
        assertFalse(WildernessConfirmGuard.shouldProceed(wildWp, config, gate, null, store));
        assertFalse(store.getWaypointById(wildWp.getId()).isBypassWildernessConfirm());
    }

    @Test
    public void gateCancelsWithDontAskTrue_ReturnsFalse_BypassNotStored()
    {
        WildernessConfirmGate gate = stubGate(false, true);
        assertFalse(WildernessConfirmGuard.shouldProceed(wildWp, config, gate, null, store));
        assertFalse(store.getWaypointById(wildWp.getId()).isBypassWildernessConfirm());
    }
}
