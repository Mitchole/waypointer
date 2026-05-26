package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.Waypoint;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.Wilderness;
import java.awt.Component;

/**
 * Pre-flight check for the PLAY action. Returns true to proceed with pathing, false to
 * cancel. Encapsulates the config/wild-check/bypass-flag/dialog cascade so it can be
 * unit-tested without spinning up Swing or a full {@link WaypointerPanel}.
 */
final class WildernessConfirmGuard
{
    private WildernessConfirmGuard() {}

    static boolean shouldProceed(
        Waypoint w,
        WaypointerConfig config,
        WildernessConfirmGate gate,
        Component parent,
        WaypointStore store)
    {
        if (!config.confirmBeforeWildernessPlay()) return true;
        if (!Wilderness.isInWilderness(w.getPackedWorldPoint())) return true;
        if (w.isBypassWildernessConfirm()) return true;

        WildernessConfirmGate.Result r = gate.prompt(parent, w);
        if (!r.proceed) return false;
        if (r.dontAskAgain) store.setWaypointBypassWildernessConfirm(w.getId(), true);
        return true;
    }
}
