package com.waypointer.ui;

import com.google.gson.Gson;
import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointPathfinder;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

/**
 * Row of clickable landmark icons in the panel header. Each button pathfinds to the nearest
 * landmark of its type from the player's current tile. The user-selected subset and order
 * is controlled by {@link LandmarkSelection}; the trailing {@code ▾} button toggles the
 * inline {@link ConfigureLandmarksPanel} picker.
 */
@Singleton
public final class NearestLandmarkBar extends JPanel
{
    private static final int BUTTON_SIZE = 34;
    // Cap icon dimension so MINIMAP_BOAT_SLOOP and other oversized sprites match the
    // MAP_ICON_* set, which tops out around 16x16. Smaller sprites render unscaled.
    private static final int MAX_ICON_PX = 18;

    private static final Map<LandmarkType, Integer> SPRITE_IDS = spriteIds();

    private final BboxIndex bbox;
    private final WaypointPathfinder pathfinder;
    private final Client client;
    private final ClientThread clientThread;
    private final SpriteManager spriteManager;
    private final WaypointerConfig config;
    private final Gson gson;

    private LandmarkSelection selection;
    private final ConfigureLandmarksPanel picker;
    private final JPanel iconRow;
    private final Map<LandmarkType, JButton> primaryButtons = new EnumMap<>(LandmarkType.class);
    private JButton overflowBtn;
    private boolean loggedIn;
    private Toasts toasts = Toasts.NO_OP;
    // Fires when dev-mode landmark overrides reload BboxIndex; rebuilds the row so button
    // text and click targets reflect the current data. Closed in dispose(); the field is
    // nullable because the BboxIndex mock in unit tests returns null from subscribe(...).
    private final com.waypointer.util.Listeners.Subscription bboxSub;
    private final LandmarkHoverPopover popover = new LandmarkHoverPopover();

    public void setToasts(Toasts toasts)
    {
        this.toasts = toasts;
    }

    @Inject
    public NearestLandmarkBar(BboxIndex bbox, WaypointPathfinder pathfinder, Client client,
        ClientThread clientThread, SpriteManager spriteManager,
        WaypointerConfig config, Gson gson)
    {
        this.bbox = bbox;
        this.pathfinder = pathfinder;
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
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        setAlignmentX(LEFT_ALIGNMENT);

        iconRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        iconRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        iconRow.setAlignmentX(LEFT_ALIGNMENT);
        add(iconRow);

        picker = new ConfigureLandmarksPanel(
            spriteManager,
            SPRITE_IDS,
            selection,
            this::onPickerToggle,
            this::onPickerReorder);
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
        popover.dispose();
    }

    private void rebuildBar()
    {
        iconRow.removeAll();
        primaryButtons.clear();

        for (LandmarkType type : selection.selectedInBarOrder())
        {
            JButton b = makeButton();
            b.addActionListener(e -> onPick(type));
            applySprite(b, type);
            primaryButtons.put(type, b);
            iconRow.add(b);
            popover.attach(b, () -> type.displayName());
        }

        overflowBtn = makeButton();
        overflowBtn.setText("▾"); // U+25BE BLACK DOWN-POINTING SMALL TRIANGLE
        overflowBtn.addActionListener(e -> {
            picker.setVisible(!picker.isVisible());
            revalidate();
            repaint();
        });
        iconRow.add(overflowBtn);
        popover.attach(overflowBtn, () -> "Customize");

        applyEnabledState();
        iconRow.revalidate();
        iconRow.repaint();
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

    private JButton makeButton()
    {
        JButton b = new JButton();
        Styles.secondaryButton(b);
        Dimension d = new Dimension(BUTTON_SIZE, BUTTON_SIZE);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        return b;
    }

    private void applyEnabledState()
    {
        // setEnabled(false) triggers Swing's setDisabledIcon swap (see applySprite),
        // which is the only visible signal we need. Tooltips were replaced by the
        // immediate hover popover in rebuildBar.
        for (JButton b : primaryButtons.values()) b.setEnabled(loggedIn);
        // The customize button stays enabled regardless of login state.
        if (overflowBtn != null) overflowBtn.setEnabled(true);
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
        if (!pathfinder.isAvailable())
        {
            toasts.show("Install the Shortest Path plugin to use Play.", Toasts.Severity.WARN);
            return;
        }
        clientThread.invoke(() -> {
            if (client.getGameState() != GameState.LOGGED_IN) return true;
            Player p = client.getLocalPlayer();
            if (p == null) return true;
            WorldPoint loc = p.getWorldLocation();
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
        pathfinder.requestPath(hit.packed, hit.name);
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
