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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ScrollBarUI;
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
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;

@Slf4j
@Singleton
public class WaypointerPanel extends PluginPanel
{
    private final WaypointStore store;
    private final WaypointCapture capture;
    private final WaypointPathfinder pathfinder;
    private final WaypointerConfig config;
    private final CollapseStateCodec collapseCodec;
    private final WaypointShareCodec shareCodec;
    private final WaypointStorePersistence persistence;
    private final SpriteManager spriteManager;
    private final IconCatalog iconCatalog;
    private final OverflowMenu overflowMenu;
    private final NearestLandmarkBar nearestLandmarkBar;
    private final LibraryJsonCodec libraryCodec;
    private final Client client;
    private final ClientThread clientThread;
    private final WildernessConfirmGate wildernessGate;
    private final CaptureForm captureForm;
    private final JPanel body = new JPanel();
    private JScrollBar bodyScrollBar;
    private final ToastOverlay toastOverlay;
    private final JButton markBtn = new JButton("Mark current location");
    private final Map<UUID, Boolean> collapsedByCategory;
    private final Set<UUID> expandedWaypoints = new HashSet<>();

    // Sentinel UUID used as the key into collapsedByCategory for the synthetic Pinned section.
    // Real categories use UUID.randomUUID(); collision probability is 1 in 2^122.
    private static final UUID PINNED_COLLAPSE_KEY =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final PlaceholderTextField searchField = new PlaceholderTextField("Search waypoints...");
    private final JLabel clearButton = new JLabel("✕"); // U+2715 (multiplication X)
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
        WaypointShareCodec shareCodec,
        WaypointStorePersistence persistence, SpriteManager spriteManager,
        IconCatalog iconCatalog, OverflowMenu overflowMenu,
        NearestLandmarkBar nearestLandmarkBar, LibraryJsonCodec libraryCodec,
        Client client, ClientThread clientThread, WildernessConfirmGate wildernessGate)
    {
        super(false);
        this.store = store;
        this.capture = capture;
        this.pathfinder = pathfinder;
        this.config = config;
        this.collapseCodec = collapseCodec;
        this.shareCodec = shareCodec;
        this.persistence = persistence;
        this.spriteManager = spriteManager;
        this.iconCatalog = iconCatalog;
        this.overflowMenu = overflowMenu;
        this.nearestLandmarkBar = nearestLandmarkBar;
        this.libraryCodec = libraryCodec;
        this.client = client;
        this.clientThread = clientThread;
        this.wildernessGate = wildernessGate;
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
        JScrollPane bodyScroll = new JScrollPane(bodyHolder);
        bodyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        JScrollBar vBar = bodyScroll.getVerticalScrollBar();
        vBar.putClientProperty("JScrollBar.width", 7);
        vBar.putClientProperty("JScrollBar.showButtons", Boolean.FALSE);
        vBar.setPreferredSize(new Dimension(7, 0));
        vBar.setUI((ScrollBarUI) RuneLiteScrollBarUI.createUI(vBar));
        this.bodyScrollBar = vBar;
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
    }

    // Re-derive the body scrollbar's UI delegate now that RuneLiteLAF is the active LAF.
    // See the constructor comment on bodyScroll for why this is needed. Called from
    // WaypointerPlugin.startUp(); updateUI() resets the preferred size and client properties,
    // so the 7-px pin is re-applied afterwards.
    public void refreshScrollbarStyling()
    {
        if (bodyScrollBar == null) return;
        SwingUtilities.invokeLater(() ->
        {
            bodyScrollBar.updateUI();
            bodyScrollBar.setPreferredSize(new Dimension(7, 0));
            bodyScrollBar.putClientProperty("JScrollBar.width", 7);
            bodyScrollBar.putClientProperty("JScrollBar.showButtons", Boolean.FALSE);
        });
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

    // Category-level menu actions, wired into each CategorySection via CategorySection.Actions.

    private void promptRenameCategory(Category c)
    {
        String newName = JOptionPane.showInputDialog(this,
            "Rename '" + c.getName() + "' to:", c.getName());
        if (newName == null || newName.trim().isEmpty()) return;
        try { store.renameCategory(c.getId(), newName.trim()); }
        catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Waypointer",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void promptDeleteCategory(Category c)
    {
        String[] options = {"Move to Uncategorized", "Delete waypoints", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Delete category '" + c.getName() + "'?\n\nWhat to do with its waypoints?",
            "Delete category", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);
        String name = c.getName();
        if (choice == 0)
        {
            store.deleteCategory(c.getId(), true);
            toastOverlay.show("Deleted category '" + name + "'", "Undo", store::undoLast);
        }
        else if (choice == 1)
        {
            // childCount is captured BEFORE the delete - deleteCategory(_, false) removes
            // those waypoints so getWaypointsInCategory would return 0 afterwards.
            int childCount = store.getWaypointsInCategory(c.getId()).size();
            store.deleteCategory(c.getId(), false);
            String msg = "Deleted '" + name + "' and " + childCount
                + (childCount == 1 ? " waypoint" : " waypoints");
            toastOverlay.show(msg, "Undo", store::undoLast);
        }
    }

    private void promptSetCategoryIcon(Category c)
    {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new IconPickerDialog(owner, spriteManager, iconCatalog, c.getIconId(),
            iconId -> store.setCategoryIcon(c.getId(), iconId)).setVisible(true);
    }

    private Library waypointSubset(Waypoint w)
    {
        Library subset = new Library();
        Category c = store.getCategoryById(w.getCategoryId());
        if (c != null) subset.getCategories().add(c);
        subset.getWaypoints().add(w);
        return subset;
    }

    private Library categorySubset(Category c)
    {
        Library subset = new Library();
        subset.getCategories().add(c);
        subset.setWaypoints(new ArrayList<>(store.getWaypointsInCategory(c.getId())));
        return subset;
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

    private void exportCategory(Category c)
    {
        Library subset = categorySubset(c);
        String code = shareCodec.encodeLibrary(subset);
        new LibraryFileIo(store, libraryCodec, this, toastOverlay)
            .copyShareCodeToClipboard(code, subset.getWaypoints().size());
    }

    private void exportCategoryToFile(Category c)
    {
        Library subset = categorySubset(c);
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        String suggested = "waypointer-category-"
            + Styles.sanitizeFilenameSegment(c.getName()) + "-" + stamp + ".json";
        new LibraryFileIo(store, libraryCodec, this, toastOverlay).exportLibraryToFile(subset, suggested);
    }

    private JComponent buildSearchBar()
    {
        JPanel container = new JPanel(new BorderLayout(4, 0));
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        container.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        searchField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
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

        clearButton.setForeground(Color.LIGHT_GRAY);
        clearButton.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.setToolTipText("Clear search");
        clearButton.setVisible(false);
        clearButton.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e) { searchField.setText(""); }
        });
        container.add(clearButton, BorderLayout.EAST);

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

    private void onFilterChanged()
    {
        String txt = searchField.getText();
        final String typed = txt == null ? "" : txt.trim();
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) searchDebounceTimer.stop();
        searchDebounceTimer = new Timer(120, e -> {
            currentFilter = typed;
            clearButton.setVisible(!currentFilter.isEmpty());
            rebuild();
        });
        searchDebounceTimer.setRepeats(false);
        searchDebounceTimer.start();
    }

    public void rebuild()
    {
        body.removeAll();
        Library snap = store.getLibrary();
        // Lowercase once for the whole render pass; per-row matchers reuse this string.
        String loweredFilter = currentFilter.toLowerCase(Locale.ROOT);
        boolean isFiltering = !loweredFilter.isEmpty();

        if (!pathfinder.isAvailable() && !config.shortestPathBannerDismissed())
        {
            body.add(buildShortestPathMissingBanner());
        }
        if (persistence.isRefusingSaves())
        {
            body.add(buildResetBanner());
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
            DragAndDropHandler dnd = isFiltering ? null : new DragAndDropHandler(store, this::rebuild);
            for (Category c : cats)
            {
                List<Waypoint> all = store.getWaypointsInCategory(c.getId());
                List<Waypoint> ws = filterWaypoints(c, all, loweredFilter);

                // Hide empty Uncategorized always.
                if (c.isUncategorized() && ws.isEmpty()) continue;
                // Hide empty categories during search (when their name didn't match either).
                if (isFiltering && ws.isEmpty()) continue;

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
                        () -> promptRenameCategory(c),
                        () -> promptDeleteCategory(c),
                        () -> promptSetCategoryIcon(c),
                        () -> exportCategory(c),
                        () -> exportCategoryToFile(c),
                        mode -> store.setCategorySortMode(c.getId(), mode)),
                    spriteManager);
                body.add(section);
                rendered = true;
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

        // Push remaining vertical space to the bottom so sections stack tight at the top.
        body.add(Box.createVerticalGlue());
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
        JPanel wrap = newCappedHeightPanel(new BorderLayout());
        wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel empty = new JLabel("<html><div style='text-align:center;padding:20px 12px 8px;"
            + "color:#9b9b9b;'>No waypoints yet.<br>Mark a location to begin, or start "
            + "from a curated set.</div></html>", SwingConstants.CENTER);
        empty.setForeground(Color.LIGHT_GRAY);
        wrap.add(empty, BorderLayout.NORTH);

        body.add(wrap);
    }

    // JPanel whose maximum height collapses to its preferred height, so banners don't get
    // stretched vertically by the body's BoxLayout(Y_AXIS).
    private static JPanel newCappedHeightPanel(LayoutManager lm)
    {
        return new JPanel(lm)
        {
            @Override public Dimension getMaximumSize()
            {
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
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
            case EXPORT:
            {
                Category c = store.getCategoryById(w.getCategoryId());
                if (c == null)
                {
                    JOptionPane.showMessageDialog(this,
                        "Waypoint has no category - cannot export.",
                        "Waypointer", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String code = shareCodec.encodeSingle(w, c);
                new LibraryFileIo(store, libraryCodec, this, toastOverlay).copyShareCodeToClipboard(code, 1);
                break;
            }
            case EXPORT_FILE:
            {
                if (store.getCategoryById(w.getCategoryId()) == null)
                {
                    JOptionPane.showMessageDialog(this,
                        "Waypoint has no category - cannot export.",
                        "Waypointer", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Library subset = waypointSubset(w);
                String stamp = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
                String suggested = "waypointer-waypoint-"
                    + Styles.sanitizeFilenameSegment(w.getName()) + "-" + stamp + ".json";
                new LibraryFileIo(store, libraryCodec, this, toastOverlay).exportLibraryToFile(subset, suggested);
                break;
            }
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

    private JComponent buildShortestPathMissingBanner()
    {
        JPanel p = newCappedHeightPanel(new BorderLayout(4, 4));
        p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(140, 100, 0)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel msg = new JLabel("<html>Install the Shortest Path plugin to use Play.</html>");
        msg.setForeground(Color.WHITE);
        p.add(msg, BorderLayout.CENTER);
        JButton dismiss = new JButton("Don't show again");
        Styles.secondaryButton(dismiss);
        dismiss.addActionListener(e -> { config.setShortestPathBannerDismissed(true); rebuild(); });
        p.add(dismiss, BorderLayout.EAST);
        return p;
    }

    private JComponent buildResetBanner()
    {
        JPanel p = newCappedHeightPanel(new BorderLayout(4, 4));
        p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 40, 40)),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel resetMsg = new JLabel("<html>Library failed to load - backup also unreadable.<br>"
            + "Click Reset to start fresh, or fix the file at:<br><tt>"
            + persistence.libraryFile() + "</tt></html>");
        resetMsg.setForeground(Color.WHITE);
        p.add(resetMsg, BorderLayout.CENTER);
        JButton reset = new JButton("Reset library");
        Styles.secondaryButton(reset);
        reset.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                "This will discard the unreadable library files. Continue?",
                "Reset library", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            try { Files.deleteIfExists(persistence.libraryFile()); }
            catch (IOException ignored) {}
            try { Files.deleteIfExists(persistence.backupFile()); }
            catch (IOException ignored) {}
            persistence.allowSavesAfterReset();
            store.bootstrap(new Library());
        });
        p.add(reset, BorderLayout.EAST);
        return p;
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
