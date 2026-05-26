package com.waypointer.ui;

import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.model.Library;
import com.waypointer.service.WaypointStore;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Disk-backed library import / export driven from the panel's overflow menu. The dialog parent
 * is supplied so JOptionPane / JFileChooser anchor to the right window, but the methods are
 * otherwise pure I/O wrappers.
 */
final class LibraryFileIo
{
    private final WaypointStore store;
    private final LibraryJsonCodec codec;
    private final Component parent;
    private final Toasts toasts;

    LibraryFileIo(WaypointStore store, LibraryJsonCodec codec, Component parent, Toasts toasts)
    {
        this.store = store;
        this.codec = codec;
        this.parent = parent;
        this.toasts = toasts == null ? Toasts.NO_OP : toasts;
    }

    void importFromFile()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Waypointer JSON", "json"));
        if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try
        {
            String json = new String(Files.readAllBytes(fc.getSelectedFile().toPath()), StandardCharsets.UTF_8);
            Library incoming = codec.decode(json);
            WaypointStore.ImportResult r = store.importMerge(incoming);
            toasts.show(String.format("Imported %d waypoints, %d categories. Skipped %d.",
                r.waypointsAdded, r.categoriesAdded, r.waypointsSkipped));
        }
        catch (IOException | RuntimeException ex)
        {
            JOptionPane.showMessageDialog(parent, "Import failed: " + ex.getMessage(),
                "Waypointer", JOptionPane.WARNING_MESSAGE);
        }
    }

    void exportToFile()
    {
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        exportLibraryToFile(store.getLibrary(), "waypointer-library-" + stamp + ".json");
    }

    void exportLibraryToFile(Library lib, String suggestedFileName)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Waypointer JSON", "json"));
        fc.setSelectedFile(new File(suggestedFileName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File target = fc.getSelectedFile();
        if (target.exists())
        {
            int ok = JOptionPane.showConfirmDialog(parent,
                target.getName() + " already exists. Overwrite?",
                "Confirm overwrite", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
        }
        try
        {
            String json = codec.encode(lib);
            Files.write(target.toPath(), json.getBytes(StandardCharsets.UTF_8));
            toasts.show("Exported to " + target.getName());
        }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(parent, "Export failed: " + ex.getMessage(),
                "Waypointer", JOptionPane.WARNING_MESSAGE);
        }
    }

    void copyShareCodeToClipboard(String code, int waypointCount)
    {
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(code), null);
        toasts.show(String.format("Library code copied - %d waypoints.", waypointCount));
    }
}
