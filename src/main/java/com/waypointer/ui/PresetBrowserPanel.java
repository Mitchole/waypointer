package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetImport;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.WaypointStore;
import com.waypointer.util.Listeners;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ScrollBarUI;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;

// Singleton preset browser. Lives for the lifetime of the plugin and reacts to store
// mutations from any source so the per-row "+" / "added" toggles stay in sync.
@Singleton
public class PresetBrowserPanel extends PluginPanel
{
    private final PresetCatalog catalog;
    private final WaypointStore store;
    private final SpriteManager spriteManager;
    private final JPanel body = new JPanel();
    private final ToastOverlay toastOverlay;
    private final JScrollBar bodyScrollBar;
    private String lastToastTextForTest;

    private Listeners.Subscription storeSub;
    private volatile boolean rebuildPending = false;

    @Inject
    public PresetBrowserPanel(PresetCatalog catalog, WaypointStore store,
        SpriteManager spriteManager)
    {
        super(false);
        this.catalog = catalog;
        this.store = store;
        this.spriteManager = spriteManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = buildHeader();
        header.setAlignmentX(LEFT_ALIGNMENT);
        add(header, BorderLayout.NORTH);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel bodyHolder = new JPanel(new BorderLayout());
        bodyHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
        bodyHolder.add(body, BorderLayout.NORTH);

        // Mirror WaypointerPanel's scrollbar-pin pattern. As a Singleton constructed
        // during loadCorePlugins() (before ClientUI.init() installs RuneLiteLAF), the
        // vertical scrollbar's UI delegate picks up Metal defaults unless we pin width
        // + showButtons + install RuneLiteScrollBarUI here, then call
        // refreshScrollbarStyling() from startUp() once LAF is live.
        JScrollPane scroll = new JScrollPane(bodyHolder);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JScrollBar vBar = scroll.getVerticalScrollBar();
        vBar.putClientProperty("JScrollBar.width", 7);
        vBar.putClientProperty("JScrollBar.showButtons", Boolean.FALSE);
        vBar.setPreferredSize(new Dimension(7, 0));
        vBar.setUI((ScrollBarUI) RuneLiteScrollBarUI.createUI(vBar));
        this.bodyScrollBar = vBar;
        toastOverlay = new ToastOverlay(scroll);
        add(toastOverlay, BorderLayout.CENTER);

        storeSub = store.subscribe(this::scheduleRebuild);
        rebuild();
    }

    // Same Windows-unmaximize regression pattern as WaypointerPanel - see CLAUDE.md
    // "Expanding/collapsing panel sections unmaximizes RuneLite" gotcha.
    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(super.getPreferredSize().width, 0);
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(super.getMinimumSize().width, 0);
    }

    /** Called from {@link com.waypointer.WaypointerPlugin#startUp()} after LAF init. */
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

    public void dispose()
    {
        if (storeSub != null) { storeSub.close(); storeSub = null; }
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Preset waypoints");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont());
        header.add(title, BorderLayout.EAST);

        return header;
    }

    // Coalesce back-to-back rebuilds within one EDT cycle into a single call.
    // Mirrors WaypointerPanel.scheduleRebuild.
    private void scheduleRebuild()
    {
        if (rebuildPending) return;
        rebuildPending = true;
        SwingUtilities.invokeLater(() ->
        {
            rebuildPending = false;
            rebuild();
        });
    }

    // Test seam - drives the same rebuild path the store listener does, without
    // depending on SwingUtilities.invokeLater scheduling.
    void scheduleRebuildForTest()
    {
        rebuild();
    }

    private void rebuild()
    {
        Set<String> expandedPresetNames = snapshotExpandedPresetNames();
        body.removeAll();
        Map<Integer, UUID> existing = existingIdByPacked();
        List<Preset> presets = catalog.getPresets();
        if (presets.isEmpty())
        {
            JLabel none = new JLabel("<html><div style='text-align:center;padding:24px 12px;"
                + "color:#9b9b9b;'>No presets available.</div></html>");
            none.setAlignmentX(LEFT_ALIGNMENT);
            none.setForeground(Color.LIGHT_GRAY);
            body.add(none);
        }
        else
        {
            for (Preset preset : presets)
            {
                PresetSection section = new PresetSection(
                    preset, spriteManager, existing,
                    wp -> addWaypoint(preset, wp),
                    id -> removeWaypoint(id));
                section.setAlignmentX(LEFT_ALIGNMENT);
                if (expandedPresetNames.contains(preset.getCategory()))
                {
                    // Restore previously-expanded state. PresetSection defaults to collapsed.
                    section.setExpanded(true);
                }
                body.add(section);
            }
        }
        body.add(Box.createVerticalGlue());
        body.revalidate();
        body.repaint();
    }

    private Set<String> snapshotExpandedPresetNames()
    {
        Set<String> out = new HashSet<>();
        for (java.awt.Component c : body.getComponents())
        {
            if (c instanceof PresetSection)
            {
                PresetSection s = (PresetSection) c;
                if (s.isExpanded())
                {
                    out.add(s.getPresetName());
                }
            }
        }
        return out;
    }

    private Map<Integer, UUID> existingIdByPacked()
    {
        Map<Integer, UUID> map = new HashMap<>();
        for (Waypoint w : store.getLibrary().getWaypoints())
        {
            map.put(w.getPackedWorldPoint(), w.getId());
        }
        return map;
    }

    UUID addWaypoint(Preset preset, PresetWaypoint wp)
    {
        store.importMerge(PresetImport.singleEntryLibrary(preset, wp));
        int packed = WorldPointPacker.pack(wp.getX(), wp.getY(), wp.getPlane());
        return findIdByPacked(store.getLibrary(), packed);
    }

    void removeWaypoint(UUID id)
    {
        Waypoint w = findWaypointById(store.getLibrary(), id);
        String name = w != null ? w.getName() : "waypoint";
        store.deleteWaypoint(id);
        String msg = "Removed " + name;
        lastToastTextForTest = msg;
        toastOverlay.show(msg);
    }

    private static UUID findIdByPacked(Library library, int packed)
    {
        for (Waypoint w : library.getWaypoints())
        {
            if (w.getPackedWorldPoint() == packed) return w.getId();
        }
        return null;
    }

    private static Waypoint findWaypointById(Library library, UUID id)
    {
        for (Waypoint w : library.getWaypoints())
        {
            if (w.getId().equals(id)) return w;
        }
        return null;
    }

    String lastToastTextForTest()
    {
        return lastToastTextForTest;
    }
}
