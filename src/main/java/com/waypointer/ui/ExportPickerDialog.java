package com.waypointer.ui;

import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.util.Text;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.service.LibrarySubsetBuilder;
import com.waypointer.service.WaypointStore;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.ColorScheme;

/**
 * Modal export dialog: tick any mix of categories / waypoints from the live library, then copy a
 * share code or save a JSON file. Opens with everything checked, unless an initial waypoint
 * selection is supplied (issue #28 bulk export passes its selected ids to pre-check that subset).
 * Both confirm buttons stay disabled while the selection is empty.
 */
final class ExportPickerDialog extends JDialog
{
    private final WaypointStore store;
    private final WaypointShareCodec shareCodec;
    private final LibraryJsonCodec libraryCodec;
    private final Toasts toasts;
    private final WaypointPickerModel model;
    private final WaypointTreePicker tree;
    private final JButton copyBtn = new JButton("Copy code");
    private final JButton saveBtn = new JButton("Save to file...");

    ExportPickerDialog(Window owner, WaypointStore store, WaypointShareCodec shareCodec,
        LibraryJsonCodec libraryCodec, Toasts toasts, Set<UUID> initialWaypointIds)
    {
        super(owner, "Export waypoints", Dialog.ModalityType.APPLICATION_MODAL);
        this.store = store;
        this.shareCodec = shareCodec;
        this.libraryCodec = libraryCodec;
        this.toasts = toasts == null ? Toasts.NO_OP : toasts;

        this.model = new WaypointPickerModel(store.getLibrary());
        if (initialWaypointIds != null)
        {
            model.selectNone();
            for (UUID id : initialWaypointIds) model.setWaypointChecked(id, true);
        }
        this.tree = new WaypointTreePicker(model, this::updateButtons);

        JPanel content = Dialogs.applyDarkContentPane(this);
        Dialogs.bindEscape(this);

        content.add(buildSelectBar(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.setPreferredSize(new Dimension(320, 360));
        content.add(scroll, BorderLayout.CENTER);

        content.add(buildFooter(), BorderLayout.SOUTH);

        updateButtons();
        getRootPane().setDefaultButton(copyBtn);
        Dialogs.finish(this, owner);
    }

    private JPanel buildSelectBar()
    {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton all = new JButton("Select all");
        JButton none = new JButton("Select none");
        Styles.secondaryButton(all);
        Styles.secondaryButton(none);
        all.addActionListener(e -> { model.selectAll(); tree.refreshAll(); updateButtons(); });
        none.addActionListener(e -> { model.selectNone(); tree.refreshAll(); updateButtons(); });
        bar.add(all);
        bar.add(none);
        return bar;
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton cancel = new JButton("Cancel");
        Styles.secondaryButton(cancel);
        Styles.secondaryButton(saveBtn);
        Styles.primaryButton(copyBtn);
        cancel.addActionListener(e -> dispose());
        copyBtn.addActionListener(e -> copyCode());
        saveBtn.addActionListener(e -> saveToFile());
        footer.add(cancel);
        footer.add(saveBtn);
        footer.add(copyBtn);
        return footer;
    }

    private void updateButtons()
    {
        boolean enabled = !model.isEmptySelection();
        copyBtn.setEnabled(enabled);
        saveBtn.setEnabled(enabled);
    }

    private Library buildSubset()
    {
        return LibrarySubsetBuilder.build(store.getLibrary(),
            model.getSelectedWaypointIds(), model.getSelectedCategoryIds());
    }

    private void copyCode()
    {
        Library subset = buildSubset();
        String code = shareCodec.encodeLibrary(subset);
        new LibraryFileIo(libraryCodec, this, toasts)
            .copyShareCodeToClipboard(code, subset.getWaypoints().size());
        dispose();
    }

    private void saveToFile()
    {
        Library subset = buildSubset();
        new LibraryFileIo(libraryCodec, this, toasts)
            .exportLibraryToFile(subset, suggestedName(subset));
    }

    private static String suggestedName(Library subset)
    {
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        if (subset.getCategories().size() == 1)
        {
            return "waypointer-"
                + Text.sanitizeFilenameSegment(subset.getCategories().get(0).getName())
                + "-" + stamp + ".json";
        }
        return "waypointer-library-" + stamp + ".json";
    }
}
