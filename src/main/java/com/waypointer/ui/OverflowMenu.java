package com.waypointer.ui;

import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Library;
import com.waypointer.service.WaypointDefaults;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Builds the panel's "⋮" overflow menu. Constructed by Guice with all of its services so the
 * panel doesn't need to hold them just to forward them in. The panel passes its own component
 * as the anchor at {@link #show(Component, Component)} time so JOption dialogs land on the
 * right window.
 */
@javax.inject.Singleton
final class OverflowMenu
{
    private final WaypointStore store;
    private final WaypointDefaults defaults;
    private final WaypointShareCodec shareCodec;
    private final LibraryJsonCodec libraryCodec;
    private final WaypointStorePersistence persistence;
    private final WaypointPathfinder pathfinder;

    @javax.inject.Inject
    OverflowMenu(WaypointStore store, WaypointDefaults defaults, WaypointShareCodec shareCodec,
        LibraryJsonCodec libraryCodec, WaypointStorePersistence persistence,
        WaypointPathfinder pathfinder)
    {
        this.store = store;
        this.defaults = defaults;
        this.shareCodec = shareCodec;
        this.libraryCodec = libraryCodec;
        this.persistence = persistence;
        this.pathfinder = pathfinder;
    }

    /** Show the popup anchored just below {@code near}; uses {@code panel} as JOption parent. */
    void show(Component near, Component panel)
    {
        Component anchor = panel;
        LibraryFileIo fileIo = new LibraryFileIo(store, libraryCodec, anchor);
        JPopupMenu menu = new JPopupMenu();

        JMenuItem importDefaults = new JMenuItem("Import defaults");
        importDefaults.addActionListener(e -> {
            WaypointStore.ImportResult r = defaults.importIntoStore();
            JOptionPane.showMessageDialog(anchor,
                String.format("Imported %d waypoints, skipped %d.",
                    r.waypointsAdded, r.waypointsSkipped),
                "Waypointer", JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(importDefaults);

        JMenuItem importLib = new JMenuItem("Import library...");
        importLib.addActionListener(e ->
            new PasteImportDialog(SwingUtilities.getWindowAncestor(anchor), store, shareCodec)
                .setVisible(true));
        menu.add(importLib);

        JMenuItem exportLib = new JMenuItem("Export library");
        exportLib.addActionListener(e -> {
            String code = shareCodec.encodeLibrary(store.getLibrary());
            fileIo.copyShareCodeToClipboard(code, store.getLibrary().getWaypoints().size());
        });
        menu.add(exportLib);

        JMenuItem importFile = new JMenuItem("Import library from file...");
        importFile.addActionListener(e -> fileIo.importFromFile());
        menu.add(importFile);

        JMenuItem exportFile = new JMenuItem("Export library to file...");
        exportFile.addActionListener(e -> fileIo.exportToFile());
        menu.add(exportFile);

        JMenuItem openFolder = new JMenuItem("Open data folder");
        openFolder.addActionListener(e -> openDataFolder(anchor));
        menu.add(openFolder);

        menu.addSeparator();

        JMenuItem stop = new JMenuItem("Stop pathing");
        stop.setEnabled(pathfinder.isAvailable()
            && pathfinder.getActiveTarget() != com.waypointer.model.WorldPointPacker.UNDEFINED);
        stop.addActionListener(e -> pathfinder.clearPath());
        menu.add(stop);

        JMenuItem reset = new JMenuItem("Reset library...");
        reset.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(anchor,
                "Delete ALL waypoints and categories? This cannot be undone.",
                "Reset library", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) store.bootstrap(new Library());
        });
        menu.add(reset);

        menu.show(near, 0, near.getHeight());
    }

    private void openDataFolder(Component anchor)
    {
        try { Desktop.getDesktop().open(persistence.getDir().toFile()); }
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(anchor, "Could not open: " + ex.getMessage(),
                "Waypointer", JOptionPane.WARNING_MESSAGE);
        }
    }
}
