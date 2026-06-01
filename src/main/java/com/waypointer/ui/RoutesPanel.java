package com.waypointer.ui;

import com.waypointer.model.route.Route;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.service.RouteStore;
import com.waypointer.util.Listeners;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** The Routes tab body: a list of routes, a playback bar, and create/edit navigation. */
@Singleton
public class RoutesPanel extends JPanel
{
    private static final String CARD_LIST = "list";
    private static final String CARD_EDITOR = "editor";

    private final RouteStore store;
    private final RoutePlaybackEngine engine;
    private final RoutePlaybackBar playbackBar;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);
    private final JPanel listCard = new JPanel(new BorderLayout());
    private final JPanel routeList = new JPanel();
    private final JPanel editorHost = new JPanel(new BorderLayout());

    private Listeners.Subscription storeSub;
    private Listeners.Subscription engineSub;
    private RouteEditorPanel openEditor;

    @Inject
    public RoutesPanel(RouteStore store, RoutePlaybackEngine engine)
    {
        this.store = store;
        this.engine = engine;
        this.playbackBar = new RoutePlaybackBar(engine);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(playbackBar, BorderLayout.NORTH);

        routeList.setLayout(new BoxLayout(routeList, BoxLayout.Y_AXIS));
        routeList.setBackground(ColorScheme.DARK_GRAY_COLOR);
        listCard.setBackground(ColorScheme.DARK_GRAY_COLOR);
        listCard.add(buildListHeader(), BorderLayout.NORTH);
        listCard.add(Styles.pinnedScrollPane(routeList), BorderLayout.CENTER);

        editorHost.setBackground(ColorScheme.DARK_GRAY_COLOR);

        cardHost.add(listCard, CARD_LIST);
        cardHost.add(editorHost, CARD_EDITOR);
        add(cardHost, BorderLayout.CENTER);

        storeSub = store.subscribe(() -> SwingUtilities.invokeLater(this::rebuildList));
        engineSub = engine.subscribe(() -> SwingUtilities.invokeLater(playbackBar::refresh));

        rebuildList();
        playbackBar.refresh();
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
        return header;
    }

    private void rebuildList()
    {
        routeList.removeAll();
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
        openEditor = new RouteEditorPanel(store, routeId, this::showList);
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
        menu.add(menuItem("Rename", () -> {
            Route r = store.getRouteById(routeId);
            String name = JOptionPane.showInputDialog(this, "Rename route:", r == null ? "" : r.getName());
            if (name != null && !name.trim().isEmpty()) store.renameRoute(routeId, name.trim());
        }));
        menu.add(menuItem("Duplicate", () -> store.duplicateRoute(routeId)));
        menu.add(menuItem("Delete", () -> store.deleteRoute(routeId)));
        menu.show(this, getWidth() / 2, 40);
    }

    private static JMenuItem menuItem(String text, Runnable action)
    {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        return item;
    }

    /** Called from the plugin shutDown to release subscriptions. */
    public void dispose()
    {
        if (storeSub != null) { storeSub.close(); storeSub = null; }
        if (engineSub != null) { engineSub.close(); engineSub = null; }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(PluginPanel.PANEL_WIDTH, 0); }
    @Override public Dimension getMinimumSize() { return new Dimension(PluginPanel.PANEL_WIDTH, 0); }
}
