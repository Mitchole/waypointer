package com.waypointer.ui;

import com.google.gson.Gson;
import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.Navigator;
import com.waypointer.service.PlayerPosition;
import com.waypointer.service.WaypointPathfinder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

/**
 * Grid of labelled landmark tiles in the panel header. Each button pathfinds to the nearest
 * landmark of its type from the player's current tile. The user-selected subset and order
 * is controlled by {@link LandmarkSelection}; the panel menu's "Customise landmark bar"
 * entry toggles the inline {@link ConfigureLandmarksPanel} picker.
 */
@Singleton
public final class NearestLandmarkBar extends JPanel
{
    private static final int TILES_PER_ROW = 4;
    private static final int TILE_HEIGHT = 38;
    // Cap icon dimension so MINIMAP_BOAT_SLOOP and other oversized sprites match the
    // MAP_ICON_* set, which tops out around 16x16. Smaller sprites render unscaled.
    private static final int MAX_ICON_PX = 18;

    private static final Map<LandmarkType, Integer> SPRITE_IDS = spriteIds();

    private final BboxIndex bbox;
    private final WaypointPathfinder pathfinder;
    private final Navigator navigator;
    private final Client client;
    private final ClientThread clientThread;
    private final SpriteManager spriteManager;
    private final WaypointerConfig config;
    private final Gson gson;
    private final PlayerPosition playerPosition;

    private LandmarkSelection selection;
    private final ConfigureLandmarksPanel picker;
    private final JPanel iconRow;
    private final Map<LandmarkType, JButton> primaryButtons = new EnumMap<>(LandmarkType.class);
    private boolean loggedIn;
    private Toasts toasts = Toasts.NO_OP;
    // Fires when dev-mode landmark overrides reload BboxIndex; rebuilds the row so button
    // text and click targets reflect the current data. Closed in dispose(); the field is
    // nullable because the BboxIndex mock in unit tests returns null from subscribe(...).
    private final com.waypointer.util.Listeners.Subscription bboxSub;

    public void setToasts(Toasts toasts)
    {
        this.toasts = toasts;
    }

    @Inject
    public NearestLandmarkBar(BboxIndex bbox, WaypointPathfinder pathfinder, Navigator navigator, Client client,
        ClientThread clientThread, SpriteManager spriteManager,
        WaypointerConfig config, Gson gson, PlayerPosition playerPosition)
    {
        this.playerPosition = playerPosition;
        this.bbox = bbox;
        this.pathfinder = pathfinder;
        this.navigator = navigator;
        this.client = client;
        this.clientThread = clientThread;
        this.spriteManager = spriteManager;
        this.config = config;
        this.gson = gson;

        this.selection = LandmarkSelection.parse(config.landmarkSelectionJson(), gson);
        String canonical = selection.toJson(gson);
        if (!canonical.equals(config.landmarkSelectionJson()))
        {
            config.setLandmarkSelectionJson(canonical);
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder());
        setAlignmentX(LEFT_ALIGNMENT);

        // Four tiles to a line. A grid sizes itself from the tile count alone, so the height
        // never depends on a width that is still settling during the first layout passes.
        iconRow = new JPanel(new GridLayout(0, TILES_PER_ROW, Styles.CARD_GAP, Styles.CARD_GAP));
        iconRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        iconRow.setAlignmentX(LEFT_ALIGNMENT);
        add(iconRow);

        picker = new ConfigureLandmarksPanel(
            spriteManager,
            SPRITE_IDS,
            selection,
            this::onPickerToggle,
            this::onPickerReorder,
            this::hidePicker);
        // Match the icon row's left alignment; a mismatched alignmentX makes BoxLayout narrow
        // the icon row when the picker is shown, forcing the bar to wrap earlier than it should.
        picker.setAlignmentX(LEFT_ALIGNMENT);
        add(picker);

        rebuildBar();

        this.bboxSub = bbox.subscribe(() ->
            SwingUtilities.invokeLater(this::rebuildBar));
    }

    /**
     * Releases the {@link BboxIndex} subscription so the bar doesn't outlive the plugin
     * lifecycle. Called from {@link WaypointerPanel#dispose()}.
     */
    public void dispose()
    {
        if (bboxSub != null) bboxSub.close();
    }

    /**
     * Rebuilds the icon row from the current selection. Old buttons leave the component
     * hierarchy along with their hover listeners; new buttons get a fresh
     * HoverHint.shared().attach in the per-type loop and on the overflow button.
     */
    private void rebuildBar()
    {
        iconRow.removeAll();
        primaryButtons.clear();

        for (LandmarkType type : selection.selectedInBarOrder())
        {
            JButton b = makeButton(type);
            b.addActionListener(e -> onPick(type));
            b.getAccessibleContext().setAccessibleName("Path to nearest " + type.displayName());
            applySprite(b, type);
            primaryButtons.put(type, b);
            iconRow.add(b);
            HoverHint.shared().attach(b, () -> type.displayName());
        }

        applyEnabledState();
        iconRow.revalidate();
        iconRow.repaint();
    }

    /** Opens or closes the inline picker. Called from the panel menu. */
    void togglePicker()
    {
        picker.setVisible(!picker.isVisible());
        revalidate();
        repaint();
    }

    // Closes the inline picker (the X in its header).
    private void hidePicker()
    {
        picker.setVisible(false);
        revalidate();
        repaint();
    }

    private void onPickerToggle(LandmarkType type, boolean include)
    {
        applySelection(selection.withSelected(type, include));
    }

    private void onPickerReorder(int fromIndex, int toIndex)
    {
        applySelection(selection.withOrderMove(fromIndex, toIndex));
    }

    private void applySelection(LandmarkSelection next)
    {
        if (next == selection) return;
        selection = next;
        config.setLandmarkSelectionJson(selection.toJson(gson));
        rebuildBar();
        picker.setSelection(selection);
    }

    private JButton makeButton(LandmarkType type)
    {
        JButton b = Styles.roundButton(shortName(type), false);
        b.setBackground(Styles.CARD_BG);
        b.setForeground(Styles.MUTED_TEXT);
        b.setFont(net.runelite.client.ui.FontManager.getRunescapeSmallFont());
        b.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        b.setIconTextGap(2);
        b.setBorder(BorderFactory.createEmptyBorder(4, 0, 3, 0));
        Dimension d = new Dimension(0, TILE_HEIGHT);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        return b;
    }

    // Tile labels have about 50 px, so the longer names are cut to one word.
    static String shortName(LandmarkType type)
    {
        switch (type)
        {
            case SPINNING_WHEEL: return "Wheel";
            case SPIRIT_TREE:    return "Spirit tree";
            case CHARTER_SHIP:   return "Charter";
            case SLAYER_MASTER:  return "Slayer";
            default:             return type.displayName();
        }
    }

    private void applyEnabledState()
    {
        // setEnabled(false) triggers Swing's setDisabledIcon swap (see applySprite),
        // which is the only visible signal we need. Tooltips were replaced by the
        // immediate hover hint in rebuildBar.
        for (JButton b : primaryButtons.values()) b.setEnabled(loggedIn);
    }

    public void setLoggedIn(boolean loggedIn)
    {
        this.loggedIn = loggedIn;
        applyEnabledState();
    }

    private void applySprite(AbstractButton b, LandmarkType type)
    {
        Integer id = SPRITE_IDS.get(type);
        if (id == null || spriteManager == null) return;
        spriteManager.getSpriteAsync(id, 0, img -> {
            if (img == null) return;
            SwingUtilities.invokeLater(() -> {
                Image scaled = scaleDownIfNeeded(img);
                b.setIcon(new ImageIcon(scaled));
                // GrayFilter produces a desaturated copy so the disabled (logged-out)
                // state reads as obviously inactive on the RuneLite dark theme.
                b.setDisabledIcon(new ImageIcon(GrayFilter.createDisabledImage(scaled)));
                b.revalidate();
                b.repaint();
            });
        });
    }

    private static Image scaleDownIfNeeded(BufferedImage src)
    {
        int w = src.getWidth();
        int h = src.getHeight();
        int longest = Math.max(w, h);
        if (longest <= MAX_ICON_PX) return src;
        double scale = (double) MAX_ICON_PX / longest;
        return src.getScaledInstance((int) Math.round(w * scale),
            (int) Math.round(h * scale), Image.SCALE_SMOOTH);
    }

    void onPick(LandmarkType type)
    {
        // Without sailing routes every landmark goes to Shortest Path, so check before the lookup.
        // Otherwise the check waits until the landmark's router is known.
        if (navigator.shortestPathOnly() && !pathfinder.isAvailable())
        {
            toasts.show("Install the Shortest Path plugin to use Play.", Toasts.Severity.WARN);
            return;
        }
        clientThread.invoke(() -> {
            if (client.getGameState() != GameState.LOGGED_IN) return true;
            WorldPoint loc = playerPosition.current();
            if (loc == null) return true;
            int fromPacked = WorldPointPacker.pack(loc);
            BboxIndex.Hit hit = bbox.nearest(type, fromPacked);
            SwingUtilities.invokeLater(() -> applyHit(type, hit));
            return true;
        });
    }

    void applyHit(LandmarkType type, @javax.annotation.Nullable BboxIndex.Hit hit)
    {
        if (hit == null)
        {
            toasts.show("No " + type.displayName().toLowerCase() + " found.", Toasts.Severity.WARN);
            return;
        }
        if (navigator.shortestPathOnly())
        {
            // onPick already checked that Shortest Path is installed.
            navigator.go(hit.packed, hit.name, Navigator.Router.SHORTEST_PATH);
            return;
        }
        navigator.resolveRouter(hit.packed, router ->
        {
            if (router == Navigator.Router.SHORTEST_PATH && !pathfinder.isAvailable())
            {
                toasts.show("Install the Shortest Path plugin to use Play.", Toasts.Severity.WARN);
                return;
            }
            navigator.go(hit.packed, hit.name, router);
        });
    }

    private static Map<LandmarkType, Integer> spriteIds()
    {
        // IDs correspond to net.runelite.api.SpriteID constants. RuneLite has no
        // dedicated SpriteID for fairy rings, spirit trees, or charter ships -- the
        // stock WorldMapPlugin uses bundled PNGs for those. We substitute the closest
        // visual match from the MAP_ICON_* sprite set instead.
        EnumMap<LandmarkType, Integer> m = new EnumMap<>(LandmarkType.class);
        m.put(LandmarkType.BANK,           1453);  // MAP_ICON_BANK
        m.put(LandmarkType.ALTAR,          1467);  // MAP_ICON_ALTAR
        m.put(LandmarkType.ANVIL,          1458);  // MAP_ICON_ANVIL
        m.put(LandmarkType.FURNACE,        1457);  // MAP_ICON_FURNACE
        m.put(LandmarkType.LOOM,           1507);  // MAP_ICON_LOOM
        m.put(LandmarkType.SPINNING_WHEEL, 1483);  // MAP_ICON_SPINNING_WHEEL
        m.put(LandmarkType.TANNER,         1481);  // MAP_ICON_TANNERY
        m.put(LandmarkType.SPIRIT_TREE,    1482);  // MAP_ICON_RARE_TREES (tree silhouette)
        m.put(LandmarkType.CHARTER_SHIP,   7290);  // MINIMAP_BOAT_SLOOP
        m.put(LandmarkType.FAIRY_RING,     1504);  // MAP_ICON_TRANSPORTATION
        m.put(LandmarkType.SLAYER_MASTER,  1499);  // MAP_ICON_SLAYER_MASTER
        return m;
    }
}
