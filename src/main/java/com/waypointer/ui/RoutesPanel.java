package com.waypointer.ui;

import com.waypointer.codec.RouteShareCodec;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.service.RouteRecorder;
import com.waypointer.service.RouteStore;
import com.waypointer.service.RouteStorePersistence;
import com.waypointer.util.Listeners;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** The Routes tab body: a list of routes, a playback bar, a recording bar, and create/edit nav. */
@Singleton
public class RoutesPanel extends JPanel
{
    private static final String CARD_LIST = "list";
    private static final String CARD_EDITOR = "editor";

    private final RouteStore store;
    private final RoutePlaybackEngine engine;
    private final RouteRecorder recorder;
    private final RouteShareCodec shareCodec;
    private final com.waypointer.service.WaypointStore waypointStore;
    private final RouteStorePersistence persistence;
    private final RoutePlaybackBar playbackBar;
    private final JPanel recordingBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));

    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JPanel listCard = new JPanel(new BorderLayout());
    private final JPanel routeList = new JPanel();
    private final JPanel editorHost = new JPanel(new BorderLayout());

    private final javax.swing.JScrollBar routeListScrollBar;
    private Listeners.Subscription storeSub;
    private Listeners.Subscription engineSub;
    private Listeners.Subscription recorderSub;
    private RouteEditorPanel openEditor;

    @Inject
    public RoutesPanel(RouteStore store, RoutePlaybackEngine engine, RouteRecorder recorder,
        RouteShareCodec shareCodec, com.waypointer.service.WaypointStore waypointStore,
        RouteStorePersistence persistence)
    {
        this.store = store;
        this.engine = engine;
        this.recorder = recorder;
        this.shareCodec = shareCodec;
        this.waypointStore = waypointStore;
        this.persistence = persistence;
        this.playbackBar = new RoutePlaybackBar(engine);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(ColorScheme.DARK_GRAY_COLOR);
        playbackBar.setAlignmentX(LEFT_ALIGNMENT);
        north.add(playbackBar);
        north.add(buildRecordingBar());
        add(north, BorderLayout.NORTH);

        routeList.setLayout(new BoxLayout(routeList, BoxLayout.Y_AXIS));
        routeList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        listCard.setBackground(ColorScheme.DARK_GRAY_COLOR);
        listCard.add(buildListHeader(), BorderLayout.NORTH);
        // The panel is built before RuneLiteLAF installs, so pin the dark scrollbar now and
        // re-derive it from startUp() via refreshScrollbarStyling() once the LAF is live.
        javax.swing.JScrollPane listScroll = Styles.pinnedScrollPane(routeList);
        this.routeListScrollBar = listScroll.getVerticalScrollBar();
        listCard.add(listScroll, BorderLayout.CENTER);

        editorHost.setBackground(ColorScheme.DARK_GRAY_COLOR);

        cardHost.add(listCard, CARD_LIST);
        cardHost.add(editorHost, CARD_EDITOR);
        add(cardHost, BorderLayout.CENTER);

        storeSub = store.subscribe(() -> SwingUtilities.invokeLater(this::rebuildList));
        engineSub = engine.subscribe(() -> SwingUtilities.invokeLater(playbackBar::refresh));
        recorderSub = recorder.subscribe(() -> SwingUtilities.invokeLater(() -> {
            refreshRecordingBar();
            if (openEditor != null) openEditor.rebuild();
        }));

        rebuildList();
        playbackBar.refresh();
        refreshRecordingBar();
        cards.show(cardHost, CARD_LIST);
    }

    private JPanel buildListHeader()
    {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton create = new JButton("New route");
        Styles.secondaryButton(create);
        create.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Route name:");
            if (name != null && !name.trim().isEmpty())
            {
                Route r = store.createRoute(name.trim());
                openEditorFor(r.getId());
            }
        });
        header.add(create);

        JButton record = new JButton("Record");
        Styles.secondaryButton(record);
        record.addActionListener(e -> {
            if (recorder.isRecording())
            {
                recorder.stopAndSave();
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Record new route - name:");
            if (name != null && !name.trim().isEmpty())
            {
                recorder.start(name.trim());
                openEditorFor(recorder.getDraftRouteId());
            }
        });
        header.add(record);

        JButton importBtn = new JButton("Import");
        Styles.secondaryButton(importBtn);
        importBtn.addActionListener(e -> importFromCode());
        header.add(importBtn);
        return header;
    }

    private JPanel buildRecordingBar()
    {
        recordingBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        recordingBar.setAlignmentX(LEFT_ALIGNMENT);
        JLabel rec = new JLabel("● Recording");   // filled circle
        rec.setForeground(new Color(0xFF5B5B));
        recordingBar.add(rec);

        JButton mark = new JButton("Mark location");
        JButton manual = new JButton("Add manual");
        JButton stop = new JButton("Stop & save");
        Styles.compactSecondaryButton(mark);
        Styles.compactSecondaryButton(manual);
        Styles.compactSecondaryButton(stop);
        mark.addActionListener(e -> recorder.markCurrentLocation());
        manual.addActionListener(e -> {
            String t = JOptionPane.showInputDialog(this, "Instruction:");
            if (t != null && !t.trim().isEmpty()) recorder.addManualStep(t.trim());
        });
        stop.addActionListener(e -> recorder.stopAndSave());
        recordingBar.add(mark);
        recordingBar.add(manual);
        recordingBar.add(stop);
        recordingBar.setVisible(false);
        return recordingBar;
    }

    private void refreshRecordingBar()
    {
        recordingBar.setVisible(recorder.isRecording());
        recordingBar.revalidate();
        recordingBar.repaint();
    }

    private void rebuildList()
    {
        routeList.removeAll();
        if (persistence.isRefusingSaves())
        {
            routeList.add(PanelBanners.routeLoadFailedReset(persistence, store, this));
        }
        for (Route r : store.getRoutesOrdered())
        {
            UUID id = r.getId();
            routeList.add(new RouteRow(r,
                () -> engine.start(store.getRouteById(id)),
                () -> openEditorFor(id),
                () -> showOverflow(id)));
        }
        if (openEditor != null) openEditor.rebuild();
        routeList.revalidate();
        routeList.repaint();
    }

    private void openEditorFor(UUID routeId)
    {
        editorHost.removeAll();
        openEditor = new RouteEditorPanel(store, routeId, this::showList,
            () -> recorder.addCurrentLocationTo(routeId),
            () -> addFromLibrary(routeId));
        editorHost.add(openEditor, BorderLayout.CENTER);
        editorHost.revalidate();
        editorHost.repaint();
        cards.show(cardHost, CARD_EDITOR);
    }

    private void showList()
    {
        openEditor = null;
        cards.show(cardHost, CARD_LIST);
        rebuildList();
    }

    private void showOverflow(UUID routeId)
    {
        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("Export (copy code)", () -> exportToClipboard(routeId)));
        menu.add(menuItem("Rename", () -> {
            Route r = store.getRouteById(routeId);
            String name = JOptionPane.showInputDialog(this, "Rename route:", r == null ? "" : r.getName());
            if (name != null && !name.trim().isEmpty()) store.renameRoute(routeId, name.trim());
        }));
        menu.add(menuItem("Duplicate", () -> store.duplicateRoute(routeId)));
        menu.add(menuItem("Delete", () -> {
            Route r = store.getRouteById(routeId);
            String name = r == null ? "this route" : "'" + r.getName() + "'";
            String[] options = {"Cancel", "Delete"};
            int choice = JOptionPane.showOptionDialog(this,
                "Delete " + name + "? This cannot be undone.",
                "Delete route", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
            if (choice == 1) store.deleteRoute(routeId);
        }));
        menu.show(this, getWidth() / 2, 40);
    }

    private void exportToClipboard(UUID routeId)
    {
        Route r = store.getRouteById(routeId);
        if (r == null) return;
        String code = shareCodec.encodeRoute(r);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new java.awt.datatransfer.StringSelection(code), null);
        JOptionPane.showMessageDialog(this, "Route code copied to clipboard.",
            "Export", JOptionPane.INFORMATION_MESSAGE);
    }

    private void importFromCode()
    {
        String code = JOptionPane.showInputDialog(this, "Paste a route code (RT1:...):");
        if (code == null || code.trim().isEmpty()) return;
        final Route imported;
        try
        {
            imported = shareCodec.decodeRoute(code.trim());
        }
        catch (RouteShareCodec.MalformedCodeException ex)
        {
            JOptionPane.showMessageDialog(this, "That is not a valid route code.",
                "Import failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Re-create through the store so the imported route gets fresh ids and persists.
        Route created = store.createRoute(imported.getName());
        store.setRepeating(created.getId(), imported.isRepeating());
        for (RouteStep s : imported.getSteps())
        {
            if (s.getType() == StepType.WAYPOINT)
            {
                store.addWaypointStep(created.getId(), s.getPackedWorldPoint(), s.getLabel(), null);
            }
            else
            {
                store.addManualStep(created.getId(), s.getLabel());
            }
        }
        JOptionPane.showMessageDialog(this, "Imported route: " + imported.getName(),
            "Import", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Pick a saved waypoint from the library and append it as a route step, snapshotting the
     * tile + name and recording the source waypoint id (hybrid sourcing: snapshot is
     * authoritative, so deleting the library waypoint never breaks the route).
     */
    private void addFromLibrary(UUID routeId)
    {
        java.util.List<com.waypointer.model.Waypoint> all = waypointStore.getLibrary().getWaypoints();
        if (all.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "No saved waypoints to choose from.",
                "From saved", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        java.util.List<WaypointChoice> choices = new java.util.ArrayList<>();
        for (com.waypointer.model.Waypoint w : all) choices.add(new WaypointChoice(w));
        choices.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));
        Object picked = JOptionPane.showInputDialog(this, "Pick a saved waypoint:", "From saved",
            JOptionPane.QUESTION_MESSAGE, null, choices.toArray(), choices.get(0));
        if (!(picked instanceof WaypointChoice)) return;
        com.waypointer.model.Waypoint w = ((WaypointChoice) picked).waypoint;
        store.addWaypointStep(routeId, w.getPackedWorldPoint(), w.getName(), w.getId());
    }

    /** Wraps a library waypoint so the chooser shows its name; carries the waypoint for selection. */
    private static final class WaypointChoice
    {
        final com.waypointer.model.Waypoint waypoint;
        WaypointChoice(com.waypointer.model.Waypoint waypoint) { this.waypoint = waypoint; }
        @Override public String toString() { return waypoint.getName(); }
    }

    private static JMenuItem menuItem(String text, Runnable action)
    {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    /** Re-derive the dark scrollbar styling once RuneLiteLAF is live (called from startUp). */
    public void refreshScrollbarStyling()
    {
        SwingUtilities.invokeLater(() -> Styles.reapplyScrollbarPin(routeListScrollBar));
    }

    /** Called from the plugin shutDown to release subscriptions. */
    public void dispose()
    {
        if (storeSub != null) { storeSub.close(); storeSub = null; }
        if (engineSub != null) { engineSub.close(); engineSub = null; }
        if (recorderSub != null) { recorderSub.close(); recorderSub = null; }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(PluginPanel.PANEL_WIDTH, 0); }
    @Override public Dimension getMinimumSize() { return new Dimension(PluginPanel.PANEL_WIDTH, 0); }
}
