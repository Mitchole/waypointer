package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import java.util.function.Consumer;
import javax.swing.JPanel;

final class LandmarkRow extends JPanel
{
    LandmarkRow(BboxIndex.Entry entry,
        Consumer<BboxIndex.Entry> onNavigate,
        Consumer<BboxIndex.Entry> onEdit,
        Consumer<BboxIndex.Entry> onDelete)
    {
        String tile = entry.x1 == entry.x2 && entry.y1 == entry.y2
            ? String.format("(%d, %d) p%d", entry.x1, entry.y1, entry.plane)
            : String.format("(%d, %d)-(%d, %d) p%d", entry.x1, entry.y1, entry.x2, entry.y2, entry.plane);
        Styles.editableMetaRow(this, entry.name, tile,
            () -> onNavigate.accept(entry),
            () -> onEdit.accept(entry),
            () -> onDelete.accept(entry));
    }
}
