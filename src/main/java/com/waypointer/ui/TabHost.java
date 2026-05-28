package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.service.WaypointPathfinder;
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
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Top-level panel hosting the {@link TabStrip}, the active path banner (moved up from
 * {@link WaypointerPanel}), and a {@link CardLayout} body holding the two child panels.
 * Replaces {@link net.runelite.client.ui.MultiplexingPluginPanel} in the plugin's wiring.
 */
@Singleton
public class TabHost extends PluginPanel
{
    private static final String CARD_MY_WAYPOINTS = "my";
    private static final String CARD_PRESETS = "presets";

    private final WaypointerPanel waypointerPanel;
    private final PresetBrowserPanel presetBrowserPanel;
    private final ActivePathBanner banner;
    private final TabStrip tabStrip;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private Listeners.Subscription pathSub;
    private TabStrip.Tab active = TabStrip.Tab.MY_WAYPOINTS;
    private String visibleCard = CARD_MY_WAYPOINTS;

    @Inject
    public TabHost(WaypointerPanel waypointerPanel, PresetBrowserPanel presetBrowserPanel,
        WaypointPathfinder pathfinder, WaypointerConfig config)
    {
        super(false);
        this.waypointerPanel = waypointerPanel;
        this.presetBrowserPanel = presetBrowserPanel;
        this.banner = new ActivePathBanner(pathfinder, config);
        this.tabStrip = new TabStrip(this::onTabSelected);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        tabStrip.setAlignmentX(LEFT_ALIGNMENT);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(tabStrip);
        northStack.add(banner);
        add(northStack, BorderLayout.NORTH);

        cards.setBackground(ColorScheme.DARK_GRAY_COLOR);
        cards.add(waypointerPanel, CARD_MY_WAYPOINTS);
        cards.add(presetBrowserPanel, CARD_PRESETS);
        add(cards, BorderLayout.CENTER);

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

    public void refreshScrollbarStyling()
    {
        waypointerPanel.refreshScrollbarStyling();
        presetBrowserPanel.refreshScrollbarStyling();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"waypointer".equals(e.getGroup())) return;
        if ("showPathingBanner".equals(e.getKey()))
        {
            SwingUtilities.invokeLater(banner::refresh);
        }
    }

    public void dispose()
    {
        if (pathSub != null) { pathSub.close(); pathSub = null; }
        waypointerPanel.dispose();
        presetBrowserPanel.dispose();
    }

    private void onTabSelected(TabStrip.Tab tab)
    {
        switch (tab)
        {
            case MY_WAYPOINTS: selectMyWaypoints(); break;
            case PRESETS:      selectPresets();     break;
        }
    }

    // Test seams.
    TabStrip.Tab getActiveTabForTest() { return active; }
    String getVisibleCardNameForTest() { return visibleCard; }
}
