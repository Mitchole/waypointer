package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.IconCatalog;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointDefaults;
import com.waypointer.service.WaypointFilter;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
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
    private final WaypointDefaults defaults;
    private final WaypointShareCodec shareCodec;
    private final WaypointStorePersistence persistence;
    private final SpriteManager spriteManager;
    private final IconCatalog iconCatalog;
    private final OverflowMenu overflowMenu;
    private final JPanel body = new JPanel();
    private final JButton markBtn = new JButton("Mark current location");
    private final Map<UUID, Boolean> collapsedByCategory;
    private final Set<UUID> expandedWaypoints = new HashSet<>();

    private final JTextField searchField = new JTextField();
    private final JLabel clearButton = new JLabel("✕"); // U+2715 (multiplication X)
    private String currentFilter = "";

    // Subscription tokens so the panel can deregister cleanly. Held even though the panel is
    // a Singleton today, in case shutdown ordering changes or the panel ever gets re-created.
    private com.waypointer.util.Listeners.Subscription storeSub;
    private com.waypointer.util.Listeners.Subscription pathSub;

    // Coalesce back-to-back rebuild requests within one EDT cycle into a single call.
    private volatile boolean rebuildPending = false;
    // Debounce search-field keystrokes so the panel only rebuilds after 120 ms of inactivity.
    private javax.swing.Timer searchDebounceTimer;

    @Inject
    public WaypointerPanel(WaypointStore store, WaypointCapture capture,
        WaypointPathfinder pathfinder, WaypointerConfig config, CollapseStateCodec collapseCodec,
        WaypointDefaults defaults, WaypointShareCodec shareCodec,
        WaypointStorePersistence persistence, SpriteManager spriteManager,
        IconCatalog iconCatalog, OverflowMenu overflowMenu)
    {
        super(false);
        this.store = store;
        this.capture = capture;
        this.pathfinder = pathfinder;
        this.config = config;
        this.collapseCodec = collapseCodec;
        this.defaults = defaults;
        this.shareCodec = shareCodec;
        this.persistence = persistence;
        this.spriteManager = spriteManager;
        this.iconCatalog = iconCatalog;
        this.overflowMenu = overflowMenu;
        this.collapsedByCategory = collapseCodec.decode(config.categoryCollapsedJson());

        // Build the waypoints-tab inner header (Mark current + Category + overflow buttons,
        // search bar). This is the same UI as before; the change is that it now lives inside
        // a tab panel rather than on the root.
        JPanel header = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        markBtn.addActionListener(e -> onMarkClicked());
        Styles.primaryButton(markBtn);
        header.add(markBtn);

        JPanel toolRow = new JPanel(new BorderLayout(4, 0));
        toolRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton newCatBtn = new JButton("+ Category");
        newCatBtn.addActionListener(e -> onNewCategoryClicked());
        Styles.secondaryButton(newCatBtn);
        toolRow.add(newCatBtn, BorderLayout.CENTER);
        JButton overflowBtn = new JButton("⋮"); // U+22EE vertical ellipsis
        overflowBtn.setToolTipText("More");
        overflowBtn.addActionListener(e -> overflowMenu.show(overflowBtn, this));
        Styles.secondaryButton(overflowBtn);
        Dimension overflowSize = new Dimension(30, newCatBtn.getPreferredSize().height);
        overflowBtn.setPreferredSize(overflowSize);
        overflowBtn.setMinimumSize(overflowSize);
        overflowBtn.setMaximumSize(overflowSize);
        toolRow.add(overflowBtn, BorderLayout.EAST);
        header.add(toolRow);

        // Stack header + search bar in NORTH so the search field sits between
        // the action buttons and the body category list.
        JPanel topStack = new JPanel();
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topStack.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        header.setAlignmentX(LEFT_ALIGNMENT);
        topStack.add(header);
        JComponent searchBar = buildSearchBar();
        searchBar.setAlignmentX(LEFT_ALIGNMENT);
        topStack.add(searchBar);

        // BoxLayout(Y_AXIS) so children stack at their preferred heights. DynamicGridLayout
        // gave each row equal height, leaving large gaps inside sections in a tall sidebar.
        // A trailing vertical glue (added in rebuild()) absorbs leftover space.
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Single waypoints layout: top stack (header buttons + search) above the body.
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(topStack, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

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

    private void onMarkClicked()
    {
        capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED)
            {
                JOptionPane.showMessageDialog(this, "Log in to mark a location.",
                    "Waypointer", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Window owner = SwingUtilities.getWindowAncestor(this);
            new CaptureDialog(owner, store, capture, packed).setVisible(true);
        });
    }

    private void onNewCategoryClicked()
    {
        String name = JOptionPane.showInputDialog(this, "Category name:", "New category",
            JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        try { store.createCategory(name.trim()); }
        catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Waypointer",
                JOptionPane.WARNING_MESSAGE);
        }
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

    private void onFilterChanged()
    {
        String txt = searchField.getText();
        final String typed = txt == null ? "" : txt.trim();
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) searchDebounceTimer.stop();
        searchDebounceTimer = new javax.swing.Timer(120, e -> {
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

        if (snap.getWaypoints().isEmpty() && !config.defaultsImportPromptSeen())
        {
            body.add(buildDefaultsBanner());
        }
        if (!pathfinder.isAvailable() && !config.shortestPathBannerDismissed())
        {
            body.add(buildShortestPathMissingBanner());
        }
        if (persistence.isRefusingSaves())
        {
            body.add(buildResetBanner());
        }
        List<Category> cats = store.getCategoriesOrdered();
        long totalWaypoints = snap.getWaypoints().size();
        boolean rendered = false;
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
                Runnable onRename = () -> {
                    String newName = JOptionPane.showInputDialog(this,
                        "Rename '" + c.getName() + "' to:", c.getName());
                    if (newName != null && !newName.trim().isEmpty())
                    {
                        try { store.renameCategory(c.getId(), newName.trim()); }
                        catch (IllegalArgumentException ex) {
                            JOptionPane.showMessageDialog(this, ex.getMessage(), "Waypointer",
                                JOptionPane.WARNING_MESSAGE);
                        }
                    }
                };
                Runnable onDelete = () -> {
                    String[] options = {"Move to Uncategorized", "Delete waypoints", "Cancel"};
                    int choice = JOptionPane.showOptionDialog(this,
                        "Delete category '" + c.getName() + "'?\n\nWhat to do with its waypoints?",
                        "Delete category", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);
                    if (choice == 0) store.deleteCategory(c.getId(), true);
                    else if (choice == 1) store.deleteCategory(c.getId(), false);
                };
                Runnable onSetIcon = () -> {
                    Window owner = SwingUtilities.getWindowAncestor(this);
                    new IconPickerDialog(owner, spriteManager, iconCatalog, c.getIconId(), iconId -> {
                        store.setCategoryIcon(c.getId(), iconId);
                    }).setVisible(true);
                };
                CategorySection section = new CategorySection(
                    c, ws, collapsed,
                    isCollapsed -> {
                        collapsedByCategory.put(c.getId(), isCollapsed);
                        config.setCategoryCollapsedJson(collapseCodec.encode(collapsedByCategory));
                    },
                    this::handleRowAction,
                    w -> expandedWaypoints.contains(w.getId())
                        ? new InlineEditPanel(w, store, capture, shareCodec, spriteManager, iconCatalog) : null,
                    dnd,
                    onRename,
                    onDelete,
                    onSetIcon,
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
        JLabel empty = new JLabel("<html><div style='text-align:center;padding:24px;color:#9b9b9b;'>"
            + "No waypoints yet.<br>Click 'Mark current location' to add one,<br>"
            + "or import defaults from the menu.</div></html>", SwingConstants.CENTER);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        empty.setForeground(Color.LIGHT_GRAY);
        body.add(empty);
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
                pathfinder.requestPath(w.getPackedWorldPoint(), w.getName());
                break;
            case DELETE:
                confirmAndDelete(this, store, w);
                break;
            case EXPAND:
                if (!expandedWaypoints.add(w.getId())) expandedWaypoints.remove(w.getId());
                rebuild();
                break;
        }
    }

    private JComponent buildDefaultsBanner()
    {
        JPanel p = newCappedHeightPanel(new BorderLayout(4, 4));
        p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel intro = new JLabel("<html>Library is empty. Import bundled defaults?</html>");
        intro.setForeground(Color.WHITE);
        p.add(intro, BorderLayout.NORTH);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JButton imp = new JButton("Import");
        JButton not = new JButton("Not now");
        JButton never = new JButton("Don't ask again");
        Styles.secondaryButton(imp);
        Styles.secondaryButton(not);
        Styles.secondaryButton(never);
        imp.addActionListener(e -> {
            WaypointStore.ImportResult r = defaults.importIntoStore();
            config.setDefaultsImportPromptSeen(true);
            JOptionPane.showMessageDialog(this,
                String.format("Imported %d waypoints, skipped %d.",
                    r.waypointsAdded, r.waypointsSkipped),
                "Waypointer", JOptionPane.INFORMATION_MESSAGE);
        });
        not.addActionListener(e -> rebuild());
        never.addActionListener(e -> { config.setDefaultsImportPromptSeen(true); rebuild(); });
        btns.add(imp);
        btns.add(not);
        btns.add(never);
        p.add(btns, BorderLayout.CENTER);
        return p;
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
     * Shared confirm-then-delete dialog. Used by {@link InlineEditPanel}'s Delete link and
     * by {@link WaypointRow}'s right-click popup. Centralised so both entry points use the
     * same wording and the same OK_CANCEL semantics.
     */
    static void confirmAndDelete(java.awt.Component anchor, WaypointStore store,
        com.waypointer.model.Waypoint w)
    {
        if (w == null) return;
        int ok = JOptionPane.showConfirmDialog(anchor,
            "Delete '" + w.getName() + "'?", "Delete waypoint",
            JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) store.deleteWaypoint(w.getId());
    }
}
