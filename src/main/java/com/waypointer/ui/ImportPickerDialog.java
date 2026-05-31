package com.waypointer.ui;

import com.waypointer.codec.LibraryJsonCodec;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.ui.ColorScheme;

/**
 * Modal import dialog. A source row at the top loads an incoming library from a pasted share code
 * ({@code WP1:} / {@code WPL1:}) or a JSON file; the tree below then lets the recipient pick which
 * categories / waypoints to merge. {@code Import selected} stays disabled until a source is loaded
 * and the selection is non-empty. Decode/populate/import are package-private so they can be tested
 * without driving modal Swing.
 */
final class ImportPickerDialog extends JDialog
{
    private final WaypointStore store;
    private final WaypointShareCodec shareCodec;
    private final LibraryJsonCodec libraryCodec;
    private final Toasts toasts;

    private final JTextArea codeArea = new JTextArea(3, 32);
    private final JPanel treeHolder = new JPanel(new BorderLayout());
    private final JButton importBtn = new JButton("Import selected");

    private Library incoming;            // null until a source loads
    private WaypointPickerModel model;   // null until a source loads

    ImportPickerDialog(Window owner, WaypointStore store, WaypointShareCodec shareCodec,
        LibraryJsonCodec libraryCodec, Toasts toasts)
    {
        super(owner, "Import waypoints", Dialog.ModalityType.APPLICATION_MODAL);
        this.store = store;
        this.shareCodec = shareCodec;
        this.libraryCodec = libraryCodec;
        this.toasts = toasts == null ? Toasts.NO_OP : toasts;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);

        content.add(buildSourceRow(), BorderLayout.NORTH);

        treeHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
        treeHolder.setPreferredSize(new Dimension(320, 320));
        content.add(treeHolder, BorderLayout.CENTER);

        content.add(buildFooter(), BorderLayout.SOUTH);

        updateImportButton();
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildSourceRow()
    {
        JPanel wrap = new JPanel(new BorderLayout(4, 4));
        wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel prompt = new JLabel("Paste a WP1: or WPL1: code, or load a file:");
        prompt.setForeground(Color.WHITE);
        wrap.add(prompt, BorderLayout.NORTH);

        Styles.textArea(codeArea);
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        wrap.add(codeScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton loadCode = new JButton("Load code");
        JButton loadFile = new JButton("Load from file...");
        Styles.secondaryButton(loadCode);
        Styles.secondaryButton(loadFile);
        loadCode.addActionListener(e -> onLoadCode());
        loadFile.addActionListener(e -> onLoadFile());
        buttons.add(loadCode);
        buttons.add(loadFile);
        wrap.add(buttons, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton cancel = new JButton("Cancel");
        Styles.secondaryButton(cancel);
        Styles.secondaryButton(importBtn);
        cancel.addActionListener(e -> dispose());
        importBtn.addActionListener(e -> onImport());
        footer.add(cancel);
        footer.add(importBtn);
        return footer;
    }

    // ---- package-private seams (no dialogs popped; used by handlers and by tests) ----

    Library decodeCodeOrNull(String text)
    {
        try { return shareCodec.decodeLibrary(text); }
        catch (RuntimeException ex) { return null; }
    }

    Library decodeFileOrNull(File f)
    {
        try
        {
            String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            return libraryCodec.decode(json);
        }
        catch (IOException | RuntimeException ex) { return null; }
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
        Library lib = decodeCodeOrNull(codeArea.getText());
        if (lib == null)
        {
            warn("Not a readable share code (expected WP1: or WPL1:).");
            return;
        }
        populate(lib);
    }

    private void onLoadFile()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Waypointer JSON", "json"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Library lib = decodeFileOrNull(fc.getSelectedFile());
        if (lib == null)
        {
            warn("Could not read that file as a Waypointer library.");
            return;
        }
        populate(lib);
    }

    private void onImport()
    {
        WaypointStore.ImportResult r = importSelected();
        if (r != null)
        {
            toasts.show(String.format("Imported %d waypoints, %d categories. Skipped %d.",
                r.waypointsAdded, r.categoriesAdded, r.waypointsSkipped));
        }
        dispose();
    }

    private void updateImportButton()
    {
        importBtn.setEnabled(model != null && !model.isEmptySelection());
    }

    private void warn(String msg)
    {
        JOptionPane.showMessageDialog(this, msg, "Waypointer", JOptionPane.WARNING_MESSAGE);
    }
}
