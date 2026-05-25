package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkType;
import com.waypointer.service.WaypointPathfinder;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

/**
 * Row of clickable landmark icons in the panel header. Each primary button pathfinds to
 * the nearest landmark of its type from the player's current tile. Long-tail types live
 * behind the trailing overflow button.
 */
@Singleton
public final class NearestLandmarkBar extends JPanel
{
    private static final int BUTTON_SIZE = 30;

    // The 5 primary types shown as top-level buttons.
    private static final List<LandmarkType> PRIMARY = Arrays.asList(
        LandmarkType.BANK,
        LandmarkType.ALTAR,
        LandmarkType.SPIRIT_TREE,
        LandmarkType.FAIRY_RING,
        LandmarkType.SLAYER_MASTER);

    // Order shown inside the overflow popup.
    private static final List<LandmarkType> OVERFLOW = Arrays.asList(
        LandmarkType.ANVIL,
        LandmarkType.FURNACE,
        LandmarkType.LOOM,
        LandmarkType.SPINNING_WHEEL,
        LandmarkType.TANNER,
        LandmarkType.CHARTER_SHIP);

    // Sprite IDs picked during manual verification (Task 11). Until then the buttons render
    // text-fallback initials so the layout is still verifiable.
    private static final Map<LandmarkType, Integer> SPRITE_IDS = spriteIds();

    private final BboxIndex bbox;
    private final WaypointPathfinder pathfinder;
    private final Client client;
    private final ClientThread clientThread;
    private final SpriteManager spriteManager;

    private final Map<LandmarkType, JButton> primaryButtons = new EnumMap<>(LandmarkType.class);
    private final JButton overflowBtn;

    @Inject
    public NearestLandmarkBar(BboxIndex bbox, WaypointPathfinder pathfinder, Client client,
        ClientThread clientThread, SpriteManager spriteManager)
    {
        this.bbox = bbox;
        this.pathfinder = pathfinder;
        this.client = client;
        this.clientThread = clientThread;
        this.spriteManager = spriteManager;

        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        setAlignmentX(LEFT_ALIGNMENT);

        for (LandmarkType type : PRIMARY)
        {
            JButton b = makeButton(type.displayName());
            primaryButtons.put(type, b);
            add(b);
        }

        overflowBtn = makeButton("More");
        overflowBtn.setText("⋮"); // vertical ellipsis
        overflowBtn.addActionListener(e -> {
            JPopupMenu menu = buildOverflowMenu();
            menu.show(overflowBtn, 0, overflowBtn.getHeight());
        });
        add(overflowBtn);

        setButtonsEnabled(false);
    }

    private JButton makeButton(String tooltip)
    {
        JButton b = new JButton();
        Styles.secondaryButton(b);
        Dimension d = new Dimension(BUTTON_SIZE, BUTTON_SIZE);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        b.setToolTipText("Path to nearest " + tooltip.toLowerCase());
        return b;
    }

    private void setButtonsEnabled(boolean enabled)
    {
        for (JButton b : primaryButtons.values()) b.setEnabled(enabled);
        overflowBtn.setEnabled(enabled);
        String suffix = enabled ? "" : " (log in to use)";
        for (Map.Entry<LandmarkType, JButton> e : primaryButtons.entrySet())
        {
            e.getValue().setToolTipText("Path to nearest " + e.getKey().displayName().toLowerCase() + suffix);
        }
        overflowBtn.setToolTipText("More" + suffix);
    }

    JPopupMenu buildOverflowMenu()
    {
        JPopupMenu menu = new JPopupMenu();
        for (LandmarkType type : OVERFLOW)
        {
            JMenuItem item = new JMenuItem(type.displayName());
            item.addActionListener(e -> onPick(type));
            menu.add(item);
        }
        return menu;
    }

    // Click handler -- Task 8 wires this to BboxIndex + WaypointPathfinder. Stub for now.
    void onPick(LandmarkType type)
    {
    }

    private static Map<LandmarkType, Integer> spriteIds()
    {
        EnumMap<LandmarkType, Integer> m = new EnumMap<>(LandmarkType.class);
        // Sprite IDs picked during manual verification (Task 11). See plan section 11.
        // Buttons render text-fallback initials until populated.
        return m;
    }
}
