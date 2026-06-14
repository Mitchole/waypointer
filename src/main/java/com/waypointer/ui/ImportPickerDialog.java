package com.waypointer.ui;

import com.waypointer.codec.DecodeReport;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.service.LibrarySubsetBuilder;
import com.waypointer.service.WaypointStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;

/**
 * Modal import dialog. A source row at the top loads an incoming library from a pasted share code
 * ({@code WP1:} / {@code WPL1:}); the tree below then lets the recipient pick which categories /
 * waypoints to merge. {@code Import selected} stays disabled until a source is loaded and the
 * selection is non-empty. Decode/populate/import are package-private so they can be tested without
 * driving modal Swing.
 */
final class ImportPickerDialog extends JDialog
{
    private final WaypointStore store;
    private final WaypointShareCodec shareCodec;
    private final Toasts toasts;

    private final JTextArea codeArea = new JTextArea(3, 32);
    private final JPanel treeHolder = new JPanel(new BorderLayout());
    private final JButton importBtn = new JButton("Import selected");
    private final JLabel errorLabel = Styles.errorLabel();

    private Library incoming;            // null until a source loads
    private WaypointPickerModel model;   // null until a source loads

    ImportPickerDialog(Window owner, WaypointStore store, WaypointShareCodec shareCodec,
        Toasts toasts)
    {
        super(owner, "Import waypoints", Dialog.ModalityType.APPLICATION_MODAL);
        this.store = store;
        this.shareCodec = shareCodec;
        this.toasts = toasts == null ? Toasts.NO_OP : toasts;

        JPanel content = Dialogs.applyDarkContentPane(this);
        Dialogs.bindEscape(this);

        content.add(buildSourceRow(), BorderLayout.NORTH);

        treeHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
        treeHolder.setPreferredSize(new Dimension(320, 320));
        content.add(treeHolder, BorderLayout.CENTER);

        content.add(buildFooter(), BorderLayout.SOUTH);

        updateImportButton();
        getRootPane().setDefaultButton(importBtn);
        Dialogs.finish(this, owner);
    }

    private JPanel buildSourceRow()
    {
        JPanel wrap = new JPanel(new BorderLayout(4, 4));
        wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel prompt = new JLabel("Paste a WP1: or WPL1: code:");
        prompt.setForeground(Color.WHITE);
        wrap.add(prompt, BorderLayout.NORTH);

        Styles.textArea(codeArea);
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        wrap.add(codeScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton loadCode = new JButton("Load code");
        Styles.secondaryButton(loadCode);
        loadCode.addActionListener(e -> onLoadCode());
        buttons.add(loadCode);
        buttons.add(errorLabel);
        wrap.add(buttons, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton cancel = new JButton("Cancel");
        Styles.secondaryButton(cancel);
        Styles.primaryButton(importBtn);
        cancel.addActionListener(e -> dispose());
        importBtn.addActionListener(e -> onImport());
        footer.add(cancel);
        footer.add(importBtn);
        return footer;
    }

    // ---- package-private seams (no dialogs popped; used by handlers and by tests) ----

    Library decodeCodeOrNull(String text)
    {
        DecodeReport r = decodeReportOrNull(text);
        return r == null ? null : r.library;
    }

    DecodeReport decodeReportOrNull(String text)
    {
        try { return shareCodec.decodeLibraryWithReport(text); }
        catch (RuntimeException ex) { return null; }
    }

    /** Load-time notice when a pasted code had unreadable entries; null when nothing was dropped. */
    static String loadNoticeOrNull(DecodeReport report)
    {
        if (report == null || report.totalDropped() == 0) return null;
        return String.format("Loaded %d waypoints, %d categories; %d invalid entries skipped.",
            report.library.getWaypoints().size(), report.library.getCategories().size(),
            report.totalDropped());
    }

    /** Import summary: base counts, plus a duplicates clause only when some were skipped. */
    static String importSummary(WaypointStore.ImportResult r)
    {
        String summary = String.format("Imported %d waypoints, %d categories.",
            r.waypointsAdded, r.categoriesAdded);
        if (r.waypointsSkipped > 0)
        {
            summary += String.format(" Skipped %d already in your library.", r.waypointsSkipped);
        }
        return summary;
    }

    void populate(Library loaded)
    {
        this.incoming = loaded;
        this.model = new WaypointPickerModel(loaded);
        WaypointTreePicker tree = new WaypointTreePicker(model, this::updateImportButton);
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        treeHolder.removeAll();
        treeHolder.add(scroll, BorderLayout.CENTER);
        treeHolder.revalidate();
        treeHolder.repaint();
        updateImportButton();
    }

    boolean hasLoadedSource() { return model != null; }

    WaypointStore.ImportResult importSelected()
    {
        if (model == null || incoming == null) return null;
        Library subset = LibrarySubsetBuilder.build(incoming,
            model.getSelectedWaypointIds(), model.getSelectedCategoryIds());
        return store.importMerge(subset);
    }

    // ---- handlers ----

    private void onLoadCode()
    {
        loadCode(codeArea.getText());
    }

    /** Package-private so a test can drive load without popping any dialog. */
    void loadCode(String text)
    {
        DecodeReport report = decodeReportOrNull(text);
        if (report == null)
        {
            showError("Not a readable share code (expected WP1: or WPL1:).");
            return;
        }
        clearError();
        populate(report.library);
        String notice = loadNoticeOrNull(report);
        if (notice != null) toasts.show(notice);
    }

    private void showError(String msg)
    {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.getParent().revalidate();
        errorLabel.getParent().repaint();
        codeArea.requestFocusInWindow();
    }

    private void clearError()
    {
        errorLabel.setText(" ");
        errorLabel.setVisible(false);
    }

    String getErrorText() { return errorLabel.getText(); }

    boolean isErrorVisible() { return errorLabel.isVisible(); }

    private void onImport()
    {
        WaypointStore.ImportResult r = importSelected();
        if (r != null)
        {
            toasts.show(importSummary(r));
        }
        dispose();
    }

    private void updateImportButton()
    {
        importBtn.setEnabled(model != null && !model.isEmptySelection());
    }
}
