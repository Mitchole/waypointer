package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.util.Listeners;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;

/**
 * Top-level panel hosting the {@link TabStrip}, the active path banner (moved up from
 * {@link WaypointerPanel}), and a {@link CardLayout} body holding the child panels.
 * Replaces {@link net.runelite.client.ui.MultiplexingPluginPanel} in the plugin's wiring.
 */
@Singleton
public class TabHost extends PluginPanel
{
    private static final String CARD_MY_WAYPOINTS = "my";
    private static final String CARD_PRESETS = "presets";
    private static final String CARD_ROUTES = "routes";

    private final WaypointerPanel waypointerPanel;
    private final PresetBrowserPanel presetBrowserPanel;
    private final RoutesPanel routesPanel;
    private final WaypointerConfig config;
    private final ClientToolbar clientToolbar;
    private NavigationButton navButton;
    private final ActivePathBanner banner;
    private final JPanel northStack;
    private TabStrip tabStrip;
    private java.util.List<TabStrip.Tab> visibleTabs;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final ToastOverlay sharedToasts = new ToastOverlay(cards);

    private Listeners.Subscription pathSub;
    private TabStrip.Tab active = TabStrip.Tab.MY_WAYPOINTS;
    private String visibleCard = CARD_MY_WAYPOINTS;

    @Inject
    public TabHost(WaypointerPanel waypointerPanel, PresetBrowserPanel presetBrowserPanel,
        RoutesPanel routesPanel, WaypointPathfinder pathfinder,
        WaypointerConfig config, ClientToolbar clientToolbar, WaypointStore store)
    {
        super(false);
        this.waypointerPanel = waypointerPanel;
        this.presetBrowserPanel = presetBrowserPanel;
        this.routesPanel = routesPanel;
        this.config = config;
        this.clientToolbar = clientToolbar;
        this.banner = new ActivePathBanner(pathfinder, config, store::hasWaypointAt);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(banner);
        add(northStack, BorderLayout.NORTH);

        cards.setBackground(ColorScheme.DARK_GRAY_COLOR);
        cards.add(waypointerPanel, CARD_MY_WAYPOINTS);
        cards.add(presetBrowserPanel, CARD_PRESETS);
        cards.add(routesPanel, CARD_ROUTES);
        add(sharedToasts, BorderLayout.CENTER);
        routesPanel.setToasts(sharedToasts);

        rebuildStrip();

        pathSub = pathfinder.subscribe(() ->
            SwingUtilities.invokeLater(banner::refresh));
        banner.refresh();
    }

    // Same Windows-unmaximize regression guard as the inner panels.
    @Override public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width, 0); }
    @Override public Dimension getMinimumSize() { return new Dimension(super.getMinimumSize().width, 0); }

    public void selectMyWaypoints()
    {
        if (active == TabStrip.Tab.MY_WAYPOINTS) return;
        active = TabStrip.Tab.MY_WAYPOINTS;
        visibleCard = CARD_MY_WAYPOINTS;
        tabStrip.setActive(active);
        cardLayout.show(cards, CARD_MY_WAYPOINTS);
        revalidate();
    }

    public void selectPresets()
    {
        if (active == TabStrip.Tab.PRESETS) return;
        active = TabStrip.Tab.PRESETS;
        visibleCard = CARD_PRESETS;
        tabStrip.setActive(active);
        cardLayout.show(cards, CARD_PRESETS);
        revalidate();
    }

    public void selectRoutes()
    {
        if (active == TabStrip.Tab.ROUTES) return;
        active = TabStrip.Tab.ROUTES;
        visibleCard = CARD_ROUTES;
        tabStrip.setActive(active);
        cardLayout.show(cards, CARD_ROUTES);
        revalidate();
    }

    /** The plugin sets this after building the nav button so openToCapture can open the panel. */
    public void setNavButton(NavigationButton navButton)
    {
        this.navButton = navButton;
    }

    /**
     * Right-click capture entry point: open the sidebar (if not already), switch to the Waypoints
     * tab, and pop the inline capture form prefilled for {@code packed}. The form stays inline
     * rather than modal so saving a waypoint never steals focus from the game canvas.
     */
    public void openToCapture(int packed, String defaultName, String npcName)
    {
        if (clientToolbar != null && navButton != null)
        {
            clientToolbar.openPanel(navButton);
        }
        selectMyWaypoints();
        waypointerPanel.showCaptureForm(packed, defaultName, npcName);
    }

    public void refreshScrollbarStyling()
    {
        waypointerPanel.refreshScrollbarStyling();
        presetBrowserPanel.refreshScrollbarStyling();
        routesPanel.refreshScrollbarStyling();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"waypointer".equals(e.getGroup())) return;
        if ("showPathingBanner".equals(e.getKey()))
        {
            SwingUtilities.invokeLater(banner::refresh);
        }
        if ("routesEnabled".equals(e.getKey()))
        {
            SwingUtilities.invokeLater(this::rebuildStrip);
        }
    }

    public void dispose()
    {
        if (pathSub != null) { pathSub.close(); pathSub = null; }
        waypointerPanel.dispose();
        presetBrowserPanel.dispose();
        routesPanel.dispose();
    }

    private void rebuildStrip()
    {
        if (tabStrip != null) northStack.remove(tabStrip);
        java.util.List<TabStrip.Tab> tabs = new java.util.ArrayList<>();
        tabs.add(TabStrip.Tab.MY_WAYPOINTS);
        tabs.add(TabStrip.Tab.PRESETS);
        if (config.routesEnabled()) tabs.add(TabStrip.Tab.ROUTES);
        visibleTabs = tabs;
        tabStrip = new TabStrip(this::onTabSelected, visibleTabs);
        tabStrip.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(tabStrip, 0);
        if (!visibleTabs.contains(active))
        {
            active = TabStrip.Tab.MY_WAYPOINTS;
            visibleCard = CARD_MY_WAYPOINTS;
            cardLayout.show(cards, CARD_MY_WAYPOINTS);
        }
        tabStrip.setActive(active);
        northStack.revalidate();
        northStack.repaint();
    }

    private void onTabSelected(TabStrip.Tab tab)
    {
        switch (tab)
        {
            case MY_WAYPOINTS: selectMyWaypoints(); break;
            case PRESETS:      selectPresets();     break;
            case ROUTES:       selectRoutes();      break;
        }
    }

    // Test seams.
    TabStrip.Tab getActiveTabForTest() { return active; }
    String getVisibleCardNameForTest() { return visibleCard; }
    int visibleTabCountForTest() { return visibleTabs.size(); }
}
