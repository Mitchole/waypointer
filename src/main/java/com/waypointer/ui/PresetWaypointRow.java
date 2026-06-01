package com.waypointer.ui;

import com.waypointer.preset.PresetWaypoint;
import java.util.function.Consumer;
import javax.swing.JPanel;

final class PresetWaypointRow extends JPanel
{
    PresetWaypointRow(PresetWaypoint wp,
        Consumer<PresetWaypoint> onNavigate,
        Consumer<PresetWaypoint> onEdit,
        Consumer<PresetWaypoint> onDelete)
    {
        String tile = String.format("(%d, %d) p%d", wp.getX(), wp.getY(), wp.getPlane());
        Styles.editableMetaRow(this, wp.getName(), tile,
            () -> onNavigate.accept(wp),
            () -> onEdit.accept(wp),
            () -> onDelete.accept(wp));
    }
}
