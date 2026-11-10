package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.codec.CoordinateText;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.sailing.ChartPlotterBridge;
import com.waypointer.service.IconCatalog;
import com.waypointer.service.Navigator;
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
import net.runelite.client.events.PluginChanged;
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
    private final CaptureForm captureForm;
    private final InlineInputForm coordinatesForm = new InlineInputForm();
    private final BulkSelectController bulkSelect;
    private final CategoryMenuController categoryMenu;
    private final FooterStrip footer;
    private final Navigator navigator;
    private final SailingControls sailingControls;
    private final JPanel body = new JPanel();
    private final JScrollBar bodyScrollBar;
    private final ToastOverlay toastOverlay;
    private final JButton markBtn = Styles.roundButton("Mark spot", true);
    private final JLabel nearestCaption = Styles.caption("Nearest");
    // Pinned waypoints whose editor was opened from their tile, so it shows under the tiles
    // and not under the row in their category.
    private final Set<UUID> pinnedEditors = new HashSet<>();
    private final Map<UUID, Boolean> collapsedByCategory;
    private final Set<UUID> expandedWaypoints = new HashSet<>();
    // Waypoints whose editor is already on screen in this rebuild. A pinned waypoint has two
    // rows (Pinned and its own category); only the first one gets the editor.
    private final Set<UUID> editorsShown = new HashSet<>();

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
        WaypointShareCodec shareCodec,
        Navigator navigator,
        com.waypointer.sailing.SailingJourneyOrchestrator sailingOrchestrator,
        com.waypointer.sailing.SailingBoats sailingBoats,
        com.waypointer.sailing.SailingBoatPin sailingBoatPin)
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
        this.collapsedByCategory = collapseCodec.decode(config.categoryCollapsedJson());
        this.navigator = navigator;
        this.sailingControls = new SailingControls(sailingBoats, sailingBoatPin, client, clientThread);

        // Header: the search field with the panel menu beside it, then the nearest-landmark
        // tiles. Marking a spot is the floating button over the list (see below).
        markBtn.setIcon(Glyphs.of(Glyphs.Shape.PLUS, Color.BLACK));
        markBtn.setIconTextGap(6);
        markBtn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 14));
        markBtn.putClientProperty(Styles.CP_ARC, 34);
        markBtn.getAccessibleContext().setAccessibleName("Mark current location");
        HoverHint.shared().attach(markBtn, () -> "Save where you are standing");
        markBtn.addActionListener(e -> onMarkClicked());

        JButton overflowBtn = Styles.roundButton("", false);
        overflowBtn.setIcon(Glyphs.of(Glyphs.Shape.MENU, Styles.MUTED_TEXT));
        overflowBtn.setBackground(Styles.CARD_BG);
        overflowBtn.setBorder(BorderFactory.createEmptyBorder(6, 5, 6, 5));
        overflowBtn.getAccessibleContext().setAccessibleName("More options");
        HoverHint.shared().attach(overflowBtn, () -> "More");

        JPanel searchRow = new JPanel(new BorderLayout(4, 0));
        searchRow.setOpaque(false);
        searchRow.setAlignmentX(LEFT_ALIGNMENT);
        searchRow.add(buildSearchBar(), BorderLayout.CENTER);
        searchRow.add(overflowBtn, BorderLayout.EAST);

        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topStack.setBorder(BorderFactory.createEmptyBorder(
            Styles.CARD_GAP, Styles.SIDE_PAD, Styles.CARD_GAP, Styles.SIDE_PAD));
        topStack.setAlignmentX(LEFT_ALIGNMENT);
        topStack.add(searchRow);
        nearestCaption.setBorder(BorderFactory.createEmptyBorder(6, 2, 3, 0));
        topStack.add(nearestCaption);
        nearestLandmarkBar.setAlignmentX(LEFT_ALIGNMENT);
        topStack.add(nearestLandmarkBar);
        setLandmarkBarVisible(config.showNearestLandmarkBar());

        // BoxLayout(Y_AXIS) so children stack at their preferred heights. DynamicGridLayout
        // gave each row equal height, leaving large gaps inside sections in a tall sidebar.
        // A trailing vertical glue (added in rebuild()) absorbs leftover space.
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        body.setBorder(BorderFactory.createEmptyBorder(0, Styles.SIDE_PAD, 0, Styles.SIDE_PAD - 2));

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
        // The Mark button floats over the list's bottom-right corner, clear of the scrollbar.
        toastOverlay.setFloating(markBtn, Styles.SIDE_PAD + 4);
        nearestLandmarkBar.setToasts(toastOverlay);
        // Journey feedback comes from the client thread; toasts belong on the EDT.
        sailingOrchestrator.setFeedback((msg, warning) -> SwingUtilities.invokeLater(() ->
            toastOverlay.show(msg, warning ? Toasts.Severity.WARN : Toasts.Severity.SUCCESS)));
        overflowBtn.addActionListener(e -> overflowMenu.show(overflowBtn, this, toastOverlay));

        this.captureForm = new CaptureForm(store, capture);
        captureForm.setAlignmentX(LEFT_ALIGNMENT);
        captureForm.setOnShownChange(shown -> updateMarkButton());

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        northStack.add(topStack);
        coordinatesForm.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(coordinatesForm);
        northStack.add(captureForm);

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(northStack, BorderLayout.NORTH);
        add(toastOverlay, BorderLayout.CENTER);

        this.bulkSelect = new BulkSelectController(
            store, toastOverlay, shareCodec,
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

        this.footer = new FooterStrip();

        storeSub = store.subscribe(this::scheduleRebuild);
        pathSub = navigator.subscribe(this::onActivePathChanged);
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
        if (searchDebounceTimer != null) searchDebounceTimer.stop();
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
                toastOverlay.show("Log in to mark a location.", Toasts.Severity.WARN);
                return;
            }
            captureForm.show(packed);
        });
    }

    /**
     * Opens the inline coordinates form from the overflow menu. A valid entry hands the tile to
     * the capture form; a bad one shows its message and leaves the form open. Typed tiles need
     * no login.
     */
    void openAddByCoordinates()
    {
        coordinatesForm.openFor("Add by coordinates", "x, y or x, y, plane", "Next", text ->
        {
            int packed;
            try
            {
                packed = CoordinateText.parse(text);
            }
            catch (IllegalArgumentException ex)
            {
                return ex.getMessage();
            }
            captureForm.show(packed);
            return null;
        });
    }

    InlineInputForm coordinatesFormForTest() { return coordinatesForm; }
    CaptureForm captureFormForTest() { return captureForm; }
    void expandForTest(Waypoint w) { if (w != null) expandedWaypoints.add(w.getId()); rebuild(); }
    SailingControls sailingControlsForTest() { return sailingControls; }
    JButton markButtonForTest() { return markBtn; }

    /** The boat submenu for the panel menu, or null when sea trips cannot run. */
    javax.swing.JMenu boatMenu()
    {
        return sailingControls.isVisible() ? sailingControls.buildMenu() : null;
    }

    /** Opens or closes the landmark bar's picker. */
    void toggleLandmarkPicker()
    {
        nearestLandmarkBar.togglePicker();
    }

    boolean landmarkBarShown() { return nearestLandmarkBar.isVisible(); }

    void enterSelectMode() { bulkSelect.enterSelectMode(); }

    // The Mark button steps aside while the form it opens is showing and during bulk select.
    private void updateMarkButton()
    {
        markBtn.setVisible(!captureForm.isVisible() && (bulkSelect == null || !bulkSelect.isSelectMode()));
    }

    private void setLandmarkBarVisible(boolean visible)
    {
        nearestCaption.setVisible(visible);
        nearestLandmarkBar.setVisible(visible);
    }

    /**
     * Show the inline capture form for an externally-supplied tile (right-click capture). Used by
     * TabHost.openToCapture after it switches to this tab. {@code defaultName} prefills the
     * name field when non-empty; {@code npcName} links the new waypoint to an NPC for highlighting.
     */
    public void showCaptureForm(int packed, String defaultName, String npcName)
    {
        captureForm.show(packed, defaultName, npcName);
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
        CardPanel container = new CardPanel(new BorderLayout(4, 0));
        container.setOutline(new Color(70, 70, 70));
        container.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        container.add(new JLabel(Glyphs.of(Glyphs.Shape.SEARCH, Styles.FAINT_TEXT)), BorderLayout.WEST);

        searchField.setOpaque(false);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        // Right inset is widened to make room for the field's own clear-glyph paint.
        // ClearableTextField derives its hit zone from getInsets(), so the two stay in sync.
        searchField.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 22));
        HoverHint.shared().attach(searchField, () -> "Search waypoints");
        searchField.getAccessibleContext().setAccessibleName("Search waypoints");
        // One reusable debounce timer; restarted per keystroke rather than reallocated. Reads the
        // field text live at fire time, so no per-keystroke capture is needed.
        searchDebounceTimer = new Timer(120, e -> {
            String txt = searchField.getText();
            currentFilter = txt == null ? "" : txt.trim();
            rebuild();
        });
        searchDebounceTimer.setRepeats(false);
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

        // The search field spans the full width now. Bulk select is entered from the row /
        // category right-click "Select multiple" entry and exited from the action bar's Done
        // button, so the old toolbar toggle no longer crowds the search field.
        container.add(searchField, BorderLayout.CENTER);

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

    /**
     * A path-target change only affects the active row's Play tint here -- the ActivePathBanner is
     * refreshed separately by TabHost. Retint rows in place rather than rebuilding the whole body
     * tree (which would reallocate every section/row and lose transient state). See issue #67.
     */
    private void onActivePathChanged()
    {
        // If a store-driven rebuild is also pending, it runs first on the EDT and rebuilds rows
        // with the correct tint from the current target; this retint then no-ops (playIconButton
        // is idempotent). The reverse order can't happen -- a path change never fires storeSub.
        SwingUtilities.invokeLater(() -> {
            int target = navigator.activeTarget();
            retintRows(body, target);
        });
    }

    // Walk the live body tree and set each WaypointRow's active state from the current target.
    // Switching from row A to row B retints both naturally. Cheap relative to a full rebuild.
    private void retintRows(java.awt.Container parent, int target)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof PathTarget)
            {
                PathTarget row = (PathTarget) child;
                row.setActive(row.getPackedWorldPoint() == target
                    && target != WorldPointPacker.UNDEFINED);
            }
            else if (child instanceof java.awt.Container)
            {
                retintRows((java.awt.Container) child, target);
            }
        }
    }

    @Subscribe
    public void onGameStateChanged(net.runelite.api.events.GameStateChanged e)
    {
        boolean loggedIn = e.getGameState() == net.runelite.api.GameState.LOGGED_IN;
        SwingUtilities.invokeLater(() -> nearestLandmarkBar.setLoggedIn(loggedIn));
        // The boat list is empty until login, so fill it once the varbits are in.
        if (loggedIn && !navigator.shortestPathOnly()) sailingControls.refresh();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"waypointer".equals(e.getGroup())) return;
        if ("showNearestLandmarkBar".equals(e.getKey()))
        {
            SwingUtilities.invokeLater(() -> {
                setLandmarkBarVisible(config.showNearestLandmarkBar());
                revalidate();
                repaint();
            });
        }
        if ("showWildernessGlyph".equals(e.getKey())
            || "newestPinAtTop".equals(e.getKey())
            || "sailingEnabled".equals(e.getKey()))
        {
            SwingUtilities.invokeLater(this::rebuild);
        }
    }

    /** Chart Plotter starting or stopping adds or removes the boat choice. */
    @Subscribe
    public void onPluginChanged(PluginChanged e)
    {
        if (ChartPlotterBridge.isChartPlotter(e.getPlugin())) SwingUtilities.invokeLater(this::rebuild);
    }

    /**
     * Flips every category card to {@code expanded},
     * persists the new collapse map, and rebuilds. Powers the overflow menu's
     * Expand all / Collapse all toggle.
     */
    public void setAllSectionsExpanded(boolean expanded)
    {
        for (Category c : store.getCategoriesOrdered())
        {
            collapsedByCategory.put(c.getId(), !expanded);
        }
        config.setCategoryCollapsedJson(collapseCodec.encode(collapsedByCategory));
        scheduleRebuild();
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
        searchDebounceTimer.restart();
    }

    public void rebuild()
    {
        body.removeAll();
        editorsShown.clear();
        updateMarkButton();
        String loweredFilter = currentFilter.toLowerCase(Locale.ROOT);
        boolean isFiltering = !loweredFilter.isEmpty();
        List<UUID> visibleIds = new ArrayList<>();

        addBanners();
        // The boat choice only matters when sea targets can be routed: sailing on and Chart
        // Plotter running. It is offered from the panel menu.
        boolean sailing = !navigator.shortestPathOnly();
        sailingControls.setVisible(sailing);
        if (sailing) sailingControls.refresh();
        boolean pinnedRendered = addPinnedSection(loweredFilter, isFiltering);
        CategoryRender cat = addCategorySections(loweredFilter, isFiltering, pinnedRendered, visibleIds);

        if (isFiltering && !pinnedRendered && !cat.rendered) addNoMatchLabel();

        bulkSelect.setVisibleOrderedIds(visibleIds);
        addFooter(isFiltering, visibleIds.size(), cat);
        body.revalidate();
        body.repaint();
    }

    private static final class CategoryRender
    {
        boolean rendered;
        int nonEmptyCategoryCount;
        int totalWaypoints;
        int shownCategoryCount;
    }

    private void addBanners()
    {
        if (!pathfinder.isAvailable() && !config.shortestPathBannerDismissed())
        {
            body.add(PanelBanners.shortestPathMissing(config, this::scheduleRebuild));
            body.add(Styles.gap(Styles.CARD_GAP));
        }
        if (persistence.isRefusingSaves())
        {
            body.add(PanelBanners.loadFailedReset(persistence, store, this));
            body.add(Styles.gap(Styles.CARD_GAP));
        }
    }

    private boolean addPinnedSection(String loweredFilter, boolean isFiltering)
    {
        // Synthetic Pinned section: render before normal categories. Honors the active filter
        // (pinned waypoints not matching the filter hide here, same as in their real category).
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
        if (!visiblePinned.isEmpty() && !bulkSelect.isSelectMode())
        {
            body.add(new PinnedSection(
                visiblePinned,
                navigator.activeTarget(),
                this::handleRowAction,
                w -> pinnedEditors.contains(w.getId()) ? inlineProviderFor(w) : null,
                spriteManager));
            return true;
        }
        return false;
    }

    private CategoryRender addCategorySections(String loweredFilter, boolean isFiltering,
        boolean prevWasSection, List<UUID> visibleIds)
    {
        CategoryRender result = new CategoryRender();
        List<Category> cats = store.getCategoriesOrdered();
        result.totalWaypoints = store.getLibrary().getWaypoints().size();
        if (result.totalWaypoints == 0 && cats.size() <= 1)
        {
            if (!isFiltering) renderEmpty();
        }
        else
        {
            // Disable drag-and-drop while filtering, or rows would reorder relative to
            // invisible neighbours.
            DragAndDropHandler dnd = (isFiltering || bulkSelect.isSelectMode()) ? null
                : new DragAndDropHandler(store, this::scheduleRebuild);
            for (Category c : cats)
            {
                List<Waypoint> all = store.getWaypointsInCategory(c.getId());
                // Count against the unfiltered list, before the continues below, so the footer
                // total stays filter-independent (matches the old FooterStrip self-walk).
                if (!all.isEmpty()) result.nonEmptyCategoryCount++;
                List<Waypoint> ws = filterWaypoints(c, all, loweredFilter);

                // Hide empty Uncategorized always.
                if (c.isUncategorized() && ws.isEmpty()) continue;
                // Hide empty categories during search (when their name didn't match either).
                if (isFiltering && ws.isEmpty()) continue;

                for (Waypoint w : ws) visibleIds.add(w.getId());

                // While filtering, force expanded so matches are visible.
                boolean collapsed = isFiltering ? false
                    : collapsedByCategory.getOrDefault(c.getId(), false);
                CategorySection section = CategorySection.spec(c, ws)
                    .activePathTarget(navigator.activeTarget())
                    .collapsed(collapsed)
                    .onCollapseChange(isCollapsed -> {
                        collapsedByCategory.put(c.getId(), isCollapsed);
                        config.setCategoryCollapsedJson(collapseCodec.encode(collapsedByCategory));
                    })
                    .onRowAction(this::handleRowAction)
                    .inlineProvider(this::inlineProviderFor)
                    .dnd(dnd)
                    .actions(new CategorySection.Actions(
                        () -> categoryMenu.promptRename(c),
                        () -> categoryMenu.promptDelete(c),
                        () -> categoryMenu.promptSetIcon(c),
                        () -> categoryMenu.promptSetColour(c, this),
                        mode -> store.setCategorySortMode(c.getId(), mode),
                        bulkSelect::enterSelectMode))
                    .spriteManager(spriteManager)
                    .selectMode(bulkSelect.isSelectMode())
                    .selection(bulkSelect.selection())
                    .onRowSelectClick(bulkSelect::onRowSelectClicked)
                    .onHeaderSelectToggle(bulkSelect::onHeaderSelectToggle)
                    .build();
                if (prevWasSection) body.add(Styles.gap(Styles.CARD_GAP));
                body.add(section);
                result.rendered = true;
                result.shownCategoryCount++;
                prevWasSection = true;
            }
        }
        return result;
    }

    private void addNoMatchLabel()
    {
        JLabel none = new JLabel("<html><div style='text-align:center;padding:24px;color:" + Styles.MUTED_HEX + ";'>"
            + "No waypoints match.</div></html>", SwingConstants.CENTER);
        none.setAlignmentX(Component.LEFT_ALIGNMENT);
        none.setForeground(Color.LIGHT_GRAY);
        body.add(none);
    }

    private void addFooter(boolean filtering, int shownWaypoints, CategoryRender cat)
    {
        // Push remaining vertical space to the bottom so cards stack tight at the top. The
        // footer (#23) sits below the glue: pinned to the viewport bottom on a short list,
        // scrolled in after the last card on a long one.
        body.add(Box.createVerticalGlue());
        if (filtering)
        {
            footer.refreshFiltered(shownWaypoints, cat.totalWaypoints, cat.shownCategoryCount);
        }
        else
        {
            footer.refresh(cat.totalWaypoints, cat.nonEmptyCategoryCount);
        }
        body.add(footer);
        // Room to scroll the last row clear of the floating Mark button.
        body.add(Styles.gap(markBtn.getPreferredSize().height + 6));
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
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(Styles.FAINT_TEXT, 4f, 4f),
            BorderFactory.createEmptyBorder(18, 10, 18, 10)));

        JLabel empty = new JLabel("<html><div style='text-align:center;'>"
            + "<b>No waypoints yet</b><br><br>"
            + "<span style='color:" + Styles.MUTED_HEX + ";'>Stand somewhere you want to come back to "
            + "and press Mark spot, or open Presets for premade waypoints.</span></div></html>",
            SwingConstants.CENTER);
        empty.setForeground(Color.WHITE);
        wrap.add(empty, BorderLayout.CENTER);

        body.add(wrap);
    }

    // Shared inline-edit provider used by Pinned and real-category sections.
    // Returns the inline editor for a waypoint if the user has it expanded, else null.
    // Single source of truth for InlineEditPanel construction across both section types.
    private Component inlineProviderFor(Waypoint w)
    {
        if (!expandedWaypoints.contains(w.getId())) return null;
        if (!editorsShown.add(w.getId())) return null;
        return new InlineEditPanel(w, store, capture, spriteManager, iconCatalog, toastOverlay,
            () -> { expandedWaypoints.remove(w.getId()); pinnedEditors.remove(w.getId()); scheduleRebuild(); },
            () -> focusWorldMap(w.getPackedWorldPoint()));
    }

    private void handleRowAction(Waypoint w, CategorySection.RowAction action)
    {
        switch (action)
        {
            case PLAY:
            {
                int packed = w.getPackedWorldPoint();
                // Clicking the active row's button again stops pathing. Stopping skips the
                // availability and wilderness gates -- those only guard starting a path.
                if (navigator.activeTarget() != WorldPointPacker.UNDEFINED
                    && navigator.activeTarget() == packed)
                {
                    navigator.stop();
                    break;
                }
                // Without sailing routes this callback runs at once, on this thread.
                navigator.resolveRouter(packed, router ->
                {
                    if (router == Navigator.Router.SHORTEST_PATH)
                    {
                        if (!pathfinder.isAvailable())
                        {
                            toastOverlay.show("Install the Shortest Path plugin to use Play.", Toasts.Severity.WARN);
                            return;
                        }
                        if (!WildernessConfirmGuard.shouldProceed(w, config, wildernessGate, this, store)) return;
                    }
                    navigator.go(packed, w.getName(), router);
                });
                break;
            }
            case DELETE:
                softDeleteWithUndo(store, w, toastOverlay);
                break;
            case EXPAND:
                pinnedEditors.remove(w.getId());
                if (!expandedWaypoints.add(w.getId())) expandedWaypoints.remove(w.getId());
                scheduleRebuild();
                break;
            case EXPAND_PINNED:
                // From a pinned tile: the editor opens under the tiles, or closes if it is there.
                if (pinnedEditors.remove(w.getId())) expandedWaypoints.remove(w.getId());
                else { pinnedEditors.add(w.getId()); expandedWaypoints.add(w.getId()); }
                scheduleRebuild();
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
            case ENTER_SELECT:
                bulkSelect.enterSelectMode();
                break;
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
