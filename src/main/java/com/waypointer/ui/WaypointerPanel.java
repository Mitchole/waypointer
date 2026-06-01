package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.IconCatalog;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointFilter;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.util.Listeners;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

@Slf4j
@Singleton
public class WaypointerPanel extends PluginPanel
{
    private final WaypointStore store;
    private final WaypointCapture capture;
    private final WaypointPathfinder pathfinder;
    private final WaypointerConfig config;
    private final CollapseStateCodec collapseCodec;
    private final WaypointStorePersistence persistence;
    private final SpriteManager spriteManager;
    private final IconCatalog iconCatalog;
    private final OverflowMenu overflowMenu;
    private final NearestLandmarkBar nearestLandmarkBar;
    private final Client client;
    private final ClientThread clientThread;
    private final WildernessConfirmGate wildernessGate;
    private final WaypointShareCodec shareCodec;
    private final LibraryJsonCodec libraryCodec;
    private final CaptureForm captureForm;
    private final BulkSelectController bulkSelect;
    private final CategoryMenuController categoryMenu;
    private final FooterStrip footer;
    private final JPanel body = new JPanel();
    private final JScrollBar bodyScrollBar;
    private final ToastOverlay toastOverlay;
    private final JButton markBtn = new JButton("Mark current location");
    private final JButton selectToggleBtn = new JButton("Select");
    private final Map<UUID, Boolean> collapsedByCategory;
    private final Set<UUID> expandedWaypoints = new HashSet<>();

    // Sentinel UUID used as the key into collapsedByCategory for the synthetic Pinned section.
    // Real categories use UUID.randomUUID(); collision probability is 1 in 2^122.
    private static final UUID PINNED_COLLAPSE_KEY =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ClearableTextField searchField = new ClearableTextField("Search waypoints...");
    private String currentFilter = "";

    // Subscription tokens so the panel can deregister cleanly. Held even though the panel is
    // a Singleton today, in case shutdown ordering changes or the panel ever gets re-created.
    private Listeners.Subscription storeSub;
    private Listeners.Subscription pathSub;

    // Coalesce back-to-back rebuild requests within one EDT cycle into a single call.
    private volatile boolean rebuildPending = false;
    // Debounce search-field keystrokes so the panel only rebuilds after 120 ms of inactivity.
    private Timer searchDebounceTimer;

    @Inject
    public WaypointerPanel(WaypointStore store, WaypointCapture capture,
        WaypointPathfinder pathfinder, WaypointerConfig config, CollapseStateCodec collapseCodec,
        WaypointStorePersistence persistence, SpriteManager spriteManager,
        IconCatalog iconCatalog, OverflowMenu overflowMenu,
        NearestLandmarkBar nearestLandmarkBar,
        Client client, ClientThread clientThread, WildernessConfirmGate wildernessGate,
        WaypointShareCodec shareCodec, LibraryJsonCodec libraryCodec)
    {
        super(false);
        this.store = store;
        this.capture = capture;
        this.pathfinder = pathfinder;
        this.config = config;
        this.collapseCodec = collapseCodec;
        this.persistence = persistence;
        this.spriteManager = spriteManager;
        this.iconCatalog = iconCatalog;
        this.overflowMenu = overflowMenu;
        this.nearestLandmarkBar = nearestLandmarkBar;
        this.client = client;
        this.clientThread = clientThread;
        this.wildernessGate = wildernessGate;
        this.shareCodec = shareCodec;
        this.libraryCodec = libraryCodec;
        this.collapsedByCategory = collapseCodec.decode(config.categoryCollapsedJson());

        // Build the panel header: Mark current location with the overflow trigger pinned
        // to its right, then the nearest-landmark bar, then the search bar.
        JPanel header = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        markBtn.addActionListener(e -> onMarkClicked());
        Styles.primaryButton(markBtn);

        JPanel markRow = new JPanel(new BorderLayout(4, 0));
        markRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        markRow.add(markBtn, BorderLayout.CENTER);
        JButton overflowBtn = new JButton("⋮"); // U+22EE vertical ellipsis
        overflowBtn.setToolTipText("More");
        overflowBtn.getAccessibleContext().setAccessibleName("More options");
        Styles.secondaryButton(overflowBtn);
        Dimension overflowSize = new Dimension(30, markBtn.getPreferredSize().height);
        overflowBtn.setPreferredSize(overflowSize);
        overflowBtn.setMinimumSize(overflowSize);
        overflowBtn.setMaximumSize(overflowSize);
        markRow.add(overflowBtn, BorderLayout.EAST);
        header.add(markRow);

        // Stack header + search bar in NORTH so the search field sits between
        // the action buttons and the body category list. The 8-px horizontal inset on
        // topStack replaces the panel-level border (removed below so the body can reach
        // the full PANEL_WIDTH after the scrollbar).
        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topStack.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        topStack.setAlignmentX(LEFT_ALIGNMENT);
        header.setAlignmentX(LEFT_ALIGNMENT);
        topStack.add(header);
        nearestLandmarkBar.setAlignmentX(LEFT_ALIGNMENT);
        nearestLandmarkBar.setVisible(config.showNearestLandmarkBar());
        topStack.add(nearestLandmarkBar);
        JComponent searchBar = buildSearchBar();
        searchBar.setAlignmentX(LEFT_ALIGNMENT);
        topStack.add(searchBar);

        // BoxLayout(Y_AXIS) so children stack at their preferred heights. DynamicGridLayout
        // gave each row equal height, leaving large gaps inside sections in a tall sidebar.
        // A trailing vertical glue (added in rebuild()) absorbs leftover space.
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Body lives inside a vertical scroll pane so an expanded category list cannot push
        // the panel's preferred height up to ClientUI. (Without it, PluginPanel passes the
        // layout-computed height through to frame.getPreferredSize, ClientUI calls
        // frame.containedSetSize to match, and the JFrame is unmaximized on Windows the
        // moment the body grows past the visible region.) bodyHolder pins body to NORTH so
        // children stack tight at the top of the viewport, and reports PluginPanel.PANEL_WIDTH
        // so the horizontal scrollbar never engages.
        JPanel bodyHolder = new JPanel(new BorderLayout())
        {
            @Override public Dimension getPreferredSize()
            {
                return new Dimension(PluginPanel.PANEL_WIDTH, super.getPreferredSize().height);
            }
        };
        bodyHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
        bodyHolder.add(body, BorderLayout.NORTH);

        // The panel is constructed during PluginManager.loadCorePlugins(), which runs before
        // ClientUI.init() installs RuneLiteLAF. So at this point UIDefaults belong to Java's
        // Metal L&F, and anything UI-delegate-derived (scrollbar colors, scroll-pane border)
        // picks up Metal's defaults. Pin width + zero out the border explicitly here, then
        // WaypointerPlugin.startUp() calls refreshScrollbarStyling() once the LAF is live.
        JScrollPane bodyScroll = Styles.pinnedScrollPane(bodyHolder);
        this.bodyScrollBar = bodyScroll.getVerticalScrollBar();
        this.toastOverlay = new ToastOverlay(bodyScroll);
        nearestLandmarkBar.setToasts(toastOverlay);
        overflowBtn.addActionListener(e -> overflowMenu.show(overflowBtn, this, toastOverlay));

        this.captureForm = new CaptureForm(store, capture);
        captureForm.setAlignmentX(LEFT_ALIGNMENT);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        northStack.add(topStack);
        northStack.add(captureForm);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(northStack, BorderLayout.NORTH);
        add(toastOverlay, BorderLayout.CENTER);

        this.bulkSelect = new BulkSelectController(
            store, toastOverlay, selectToggleBtn, shareCodec, libraryCodec,
            new BulkSelectController.Host()
            {
                @Override public void rebuild() { scheduleRebuild(); }
                @Override public void revalidateAndRepaint() { revalidate(); repaint(); }
                @Override public Window windowAncestor()
                {
                    return SwingUtilities.getWindowAncestor(WaypointerPanel.this);
                }
            });
        add(bulkSelect.bar(), BorderLayout.SOUTH);

        this.categoryMenu = new CategoryMenuController(store, spriteManager, iconCatalog,
            toastOverlay, this);

        this.footer = new FooterStrip(store);

        storeSub = store.subscribe(this::scheduleRebuild);
        pathSub = pathfinder.subscribe(this::scheduleRebuild);
        rebuild();
    }

    /**
     * Called from {@link com.waypointer.WaypointerPlugin#shutDown()} so the panel's listeners
     * release their references to the store and pathfinder. Currently a no-op in practice (the
     * panel is @Singleton and gets garbage-collected with the rest of the plugin's injector
     * on disable), but keeps the lifecycle honest if either side becomes longer-lived.
     */
    public void dispose()
    {
        if (storeSub != null) { storeSub.close(); storeSub = null; }
        if (pathSub != null) { pathSub.close(); pathSub = null; }
        nearestLandmarkBar.dispose();
    }

    // Re-derive the body scrollbar's UI delegate now that RuneLiteLAF is the active LAF.
    // See the constructor comment on bodyScroll for why this is needed. Called from
    // WaypointerPlugin.startUp(); updateUI() resets the preferred size and client properties,
    // so the 7-px pin is re-applied afterwards.
    public void refreshScrollbarStyling()
    {
        SwingUtilities.invokeLater(() -> Styles.reapplyScrollbarPin(bodyScrollBar));
    }

    // PluginPanel.getPreferredSize / getMinimumSize pass the JPanel's layout-computed
    // height through to ClientUI. Without a cap, BoxLayout body's preferred height grows
    // with every expanded WaypointRow, ClientUI uses it for frame.getPreferredSize, and the
    // JFrame is resized to fit; on Windows that unmaximizes the window and resets the game
    // canvas. The body's own JScrollPane handles overflow so the height we report outward
    // can stay at 0, letting the frame size stay driven by the game canvas.
    @Override
    public Dimension getPreferredSize()
    {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 0);
    }

    @Override
    public Dimension getMinimumSize()
    {
        Dimension d = super.getMinimumSize();
        return new Dimension(d.width, 0);
    }

    private void onMarkClicked()
    {
        capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED)
            {
                JOptionPane.showMessageDialog(this, "Log in to mark a location.",
                    "Waypointer", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            captureForm.show(packed);
        });
    }

    // Scrolls an already-open world map to the given packed tile. setWorldMapPositionTarget
    // hits the game-side WorldMap, so it must be invoked from the client thread. The map
    // itself isn't opened programmatically (no public RuneLite API for that); if the user
    // hasn't opened it, the call is a no-op until they do.
    private void focusWorldMap(int packed)
    {
        if (packed == WorldPointPacker.UNDEFINED) return;
        clientThread.invoke(() ->
        {
            WorldMap worldMap = client.getWorldMap();
            if (worldMap == null) return;
            worldMap.setWorldMapPositionTarget(WorldPointPacker.unpack(packed));
        });
    }

    private JComponent buildSearchBar()
    {
        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        // Right inset is widened to make room for the field's own clear-glyph paint.
        // ClearableTextField derives its hit zone from getInsets(), so the two stay in sync.
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 6, 4, 22)));
        searchField.setToolTipText("Search waypoints");
        searchField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e)  { onFilterChanged(); }
            @Override public void removeUpdate(DocumentEvent e)  { onFilterChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onFilterChanged(); }
        });
        // ESC clears the field but keeps focus.
        searchField.addKeyListener(new KeyAdapter()
        {
            @Override public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                {
                    searchField.setText("");
                }
            }
        });

        container.add(searchField, BorderLayout.CENTER);

        selectToggleBtn.setToolTipText("Select multiple waypoints");
        Styles.secondaryButton(selectToggleBtn);
        Dimension toggleSize = new Dimension(58, searchField.getPreferredSize().height);
        selectToggleBtn.setPreferredSize(toggleSize);
        selectToggleBtn.addActionListener(e -> bulkSelect.toggleSelectMode());
        JPanel east = new JPanel(new BorderLayout());
        east.setOpaque(false);
        east.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        east.add(selectToggleBtn, BorderLayout.CENTER);
        container.add(east, BorderLayout.EAST);

        return container;
    }

    private void scheduleRebuild()
    {
        if (rebuildPending) return;
        rebuildPending = true;
        SwingUtilities.invokeLater(() -> {
            rebuildPending = false;
            rebuild();
        });
    }

    @Subscribe
    public void onGameStateChanged(net.runelite.api.events.GameStateChanged e)
    {
        boolean loggedIn = e.getGameState() == net.runelite.api.GameState.LOGGED_IN;
        SwingUtilities.invokeLater(() -> nearestLandmarkBar.setLoggedIn(loggedIn));
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"waypointer".equals(e.getGroup())) return;
        if ("showNearestLandmarkBar".equals(e.getKey()))
        {
            SwingUtilities.invokeLater(() -> {
                nearestLandmarkBar.setVisible(config.showNearestLandmarkBar());
                revalidate();
                repaint();
            });
        }
        if ("showWildernessGlyph".equals(e.getKey())
            || "newestPinAtTop".equals(e.getKey()))
        {
            SwingUtilities.invokeLater(this::rebuild);
        }
    }

    /**
     * Flips every collapsible body section (Pinned + real categories) to {@code expanded},
     * persists the new collapse map, and rebuilds. Powers the overflow menu's
     * Expand all / Collapse all toggle.
     */
    public void setAllSectionsExpanded(boolean expanded)
    {
        for (Category c : store.getCategoriesOrdered())
        {
            collapsedByCategory.put(c.getId(), !expanded);
        }
        collapsedByCategory.put(PINNED_COLLAPSE_KEY, !expanded);
        config.setCategoryCollapsedJson(collapseCodec.encode(collapsedByCategory));
        rebuild();
    }

    /**
     * True when at least half of the visible collapsible sections are currently collapsed.
     * Drives the overflow menu's label flip between "Expand all" and "Collapse all". Mirrors
     * rebuild's visibility rules so a section that's not on screen doesn't sway the count.
     */
    public boolean isMajorityCollapsed()
    {
        int collapsed = 0;
        int total = 0;
        if (!store.getPinnedWaypoints(config.newestPinAtTop()).isEmpty())
        {
            total++;
            if (collapsedByCategory.getOrDefault(PINNED_COLLAPSE_KEY, false)) collapsed++;
        }
        for (Category c : store.getCategoriesOrdered())
        {
            if (c.isUncategorized() && store.getWaypointsInCategory(c.getId()).isEmpty()) continue;
            total++;
            if (collapsedByCategory.getOrDefault(c.getId(), false)) collapsed++;
        }
        return total == 0 || collapsed * 2 >= total;
    }

    private void onFilterChanged()
    {
        String txt = searchField.getText();
        final String typed = txt == null ? "" : txt.trim();
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) searchDebounceTimer.stop();
        searchDebounceTimer = new Timer(120, e -> {
            currentFilter = typed;
            rebuild();
        });
        searchDebounceTimer.setRepeats(false);
        searchDebounceTimer.start();
    }

    public void rebuild()
    {
        body.removeAll();
        Library snap = store.getLibrary();
        java.util.List<UUID> visibleIds = new ArrayList<>();
        // Lowercase once for the whole render pass; per-row matchers reuse this string.
        String loweredFilter = currentFilter.toLowerCase(Locale.ROOT);
        boolean isFiltering = !loweredFilter.isEmpty();

        if (!pathfinder.isAvailable() && !config.shortestPathBannerDismissed())
        {
            body.add(PanelBanners.shortestPathMissing(config, this::rebuild));
        }
        if (persistence.isRefusingSaves())
        {
            body.add(PanelBanners.loadFailedReset(persistence, store, this));
        }

        // Synthetic Pinned section: render before normal categories. Honors the active filter
        // (pinned waypoints not matching the filter hide here, same as in their real category).
        boolean rendered = false;
        List<Waypoint> allPinned = store.getPinnedWaypoints(config.newestPinAtTop());
        List<Waypoint> visiblePinned;
        if (loweredFilter.isEmpty())
        {
            visiblePinned = allPinned;
        }
        else
        {
            visiblePinned = new ArrayList<>();
            for (Waypoint w : allPinned)
            {
                Category origin = store.getCategoryById(w.getCategoryId());
                if (origin == null) continue;
                if (WaypointFilter.categoryNameMatchesLowered(origin, loweredFilter)
                    || WaypointFilter.matchesLowered(w, origin, loweredFilter))
                {
                    visiblePinned.add(w);
                }
            }
        }
        if (!visiblePinned.isEmpty())
        {
            boolean pinnedCollapsed = isFiltering ? false
                : collapsedByCategory.getOrDefault(PINNED_COLLAPSE_KEY, false);
            PinnedSection pinnedSec = new PinnedSection(
                visiblePinned,
                pathfinder.getActiveTarget(),
                pinnedCollapsed,
                isCollapsed -> {
                    collapsedByCategory.put(PINNED_COLLAPSE_KEY, isCollapsed);
                    config.setCategoryCollapsedJson(collapseCodec.encode(collapsedByCategory));
                },
                this::handleRowAction,
                this::inlineProviderFor,
                spriteManager,
                w -> store.getCategoryById(w.getCategoryId()));
            body.add(pinnedSec);
            rendered = true;
        }

        // A section was rendered as the immediate previous body child. Used to insert a 1-px
        // divider between adjacent section blocks (banners + empty state don't count, so the
        // flag resets after the pinned/category-loop block decides what to add).
        boolean prevWasSection = rendered;

        List<Category> cats = store.getCategoriesOrdered();
        long totalWaypoints = snap.getWaypoints().size();
        if (totalWaypoints == 0 && cats.size() <= 1)
        {
            if (!isFiltering) renderEmpty();
        }
        else
        {
            // Disable drag-and-drop while filtering, or rows would reorder relative to
            // invisible neighbours.
            DragAndDropHandler dnd = (isFiltering || bulkSelect.isSelectMode()) ? null
                : new DragAndDropHandler(store, this::rebuild);
            for (Category c : cats)
            {
                List<Waypoint> all = store.getWaypointsInCategory(c.getId());
                List<Waypoint> ws = filterWaypoints(c, all, loweredFilter);

                // Hide empty Uncategorized always.
                if (c.isUncategorized() && ws.isEmpty()) continue;
                // Hide empty categories during search (when their name didn't match either).
                if (isFiltering && ws.isEmpty()) continue;

                for (Waypoint w : ws) visibleIds.add(w.getId());

                // While filtering, force expanded so matches are visible.
                boolean collapsed = isFiltering ? false
                    : collapsedByCategory.getOrDefault(c.getId(), false);
                CategorySection section = new CategorySection(
                    c, ws, pathfinder.getActiveTarget(), collapsed,
                    isCollapsed -> {
                        collapsedByCategory.put(c.getId(), isCollapsed);
                        config.setCategoryCollapsedJson(collapseCodec.encode(collapsedByCategory));
                    },
                    this::handleRowAction,
                    this::inlineProviderFor,
                    dnd,
                    new CategorySection.Actions(
                        () -> categoryMenu.promptRename(c),
                        () -> categoryMenu.promptDelete(c),
                        () -> categoryMenu.promptSetIcon(c),
                        () -> categoryMenu.promptSetColour(c, this),
                        mode -> store.setCategorySortMode(c.getId(), mode)),
                    spriteManager,
                    bulkSelect.isSelectMode(),
                    bulkSelect.selection(),
                    bulkSelect::onRowSelectClicked,
                    bulkSelect::onHeaderSelectToggle);
                if (prevWasSection) body.add(buildSectionDivider());
                body.add(section);
                rendered = true;
                prevWasSection = true;
            }
        }

        if (isFiltering && !rendered)
        {
            JLabel none = new JLabel("<html><div style='text-align:center;padding:24px;color:#9b9b9b;'>"
                + "No waypoints match.</div></html>", SwingConstants.CENTER);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            none.setForeground(Color.LIGHT_GRAY);
            body.add(none);
        }

        bulkSelect.setVisibleOrderedIds(visibleIds);

        // Push remaining vertical space to the bottom so sections stack tight at the top.
        body.add(Box.createVerticalGlue());
        // Footer (#23) sits below the glue: pinned to the viewport bottom on a short list,
        // scrolls in after the last section on a long one. Divider separates it from the list.
        body.add(buildSectionDivider());
        footer.refresh();
        body.add(footer);
        body.revalidate();
        body.repaint();
    }

    private List<Waypoint> filterWaypoints(Category c, List<Waypoint> all, String loweredFilter)
    {
        if (loweredFilter == null || loweredFilter.isEmpty()) return all;
        if (WaypointFilter.categoryNameMatchesLowered(c, loweredFilter)) return all;
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : all)
        {
            if (WaypointFilter.matchesLowered(w, c, loweredFilter)) out.add(w);
        }
        return out;
    }

    private void renderEmpty()
    {
        JPanel wrap = Styles.cappedHeightPanel(new BorderLayout());
        wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Faint up-arrow (#24) at the top of the body, aimed up past the search bar at the orange
        // Mark button. Only painted here, so it vanishes the moment the first waypoint exists.
        JLabel arrow = new JLabel("↑", SwingConstants.CENTER);
        arrow.setForeground(new Color(0x6e, 0x6e, 0x6e));
        arrow.setFont(arrow.getFont().deriveFont(28f));
        arrow.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        wrap.add(arrow, BorderLayout.NORTH);

        JLabel empty = new JLabel("<html><div style='text-align:center;padding:8px 12px 8px;"
            + "color:#9b9b9b;'>No waypoints yet.<br>Mark a location to begin. Or select "
            + "'Presets' to add premade Waypoints</div></html>", SwingConstants.CENTER);
        empty.setForeground(Color.LIGHT_GRAY);
        wrap.add(empty, BorderLayout.CENTER);

        body.add(wrap);
    }

    // 1-px DARKER_GRAY divider between adjacent section blocks. Pinned by both max and
    // preferred size so BoxLayout(Y_AXIS) doesn't stretch it vertically.
    private static JComponent buildSectionDivider()
    {
        JPanel divider = new JPanel();
        divider.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        divider.setOpaque(true);
        divider.setPreferredSize(new Dimension(0, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        return divider;
    }

    // Shared inline-edit provider used by Pinned and real-category sections.
    // Returns the inline editor for a waypoint if the user has it expanded, else null.
    // Single source of truth for InlineEditPanel construction across both section types.
    private Component inlineProviderFor(Waypoint w)
    {
        if (!expandedWaypoints.contains(w.getId())) return null;
        return new InlineEditPanel(w, store, capture, spriteManager, iconCatalog, toastOverlay,
            () -> { expandedWaypoints.remove(w.getId()); scheduleRebuild(); },
            () -> focusWorldMap(w.getPackedWorldPoint()));
    }

    private void handleRowAction(Waypoint w, CategorySection.RowAction action)
    {
        switch (action)
        {
            case PLAY:
                if (!pathfinder.isAvailable())
                {
                    JOptionPane.showMessageDialog(this,
                        "Install the Shortest Path plugin to use Play.",
                        "Waypointer", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (!WildernessConfirmGuard.shouldProceed(w, config, wildernessGate, this, store)) return;
                pathfinder.requestPath(w.getPackedWorldPoint(), w.getName());
                break;
            case DELETE:
                softDeleteWithUndo(store, w, toastOverlay);
                break;
            case EXPAND:
                if (!expandedWaypoints.add(w.getId())) expandedWaypoints.remove(w.getId());
                rebuild();
                break;
            case TOGGLE_PIN:
            {
                boolean nowPinned = !w.isPinned();
                store.setWaypointPinned(w.getId(), nowPinned);
                toastOverlay.show(
                    (nowPinned ? "Pinned '" : "Unpinned '") + w.getName() + "'",
                    null, null);
                break;
            }
        }
    }

    /**
     * Shared soft-delete helper. Deletes the waypoint immediately and shows an undo toast
     * (6-second window). Used by {@link InlineEditPanel}'s Delete link and by
     * {@link WaypointRow}'s right-click popup so both surfaces share the same affordance.
     */
    static void softDeleteWithUndo(WaypointStore store, Waypoint w, Toasts toasts)
    {
        if (w == null) return;
        String name = w.getName();
        store.deleteWaypoint(w.getId());
        toasts.show("Deleted '" + name + "'", "Undo", store::undoLast);
    }
}
