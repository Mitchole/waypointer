package com.waypointer.ui;

import com.waypointer.sailing.SailingBoatPin;
import com.waypointer.sailing.SailingBoats;
import java.util.Collections;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

/** The boat choice for sea trips, offered as a submenu of the panel menu. Reads the live boat
 *  list on the client thread and keeps the last result for the menu. The persisted choice is
 *  the boat slot. */
public class SailingControls
{
    static final String MENU_TITLE = "Boat for sea trips";
    static final String NEAREST = "Nearest to the waypoint";

    private final SailingBoats boats;
    private final SailingBoatPin pin;
    private final Client client;
    private final ClientThread clientThread;

    private List<SailingBoats.BoatInfo> moored = Collections.emptyList();
    private boolean pinned;
    private int pinnedSlot = -1;
    private boolean visible;

    public SailingControls(SailingBoats boats, SailingBoatPin pin, Client client, ClientThread clientThread)
    {
        this.boats = boats;
        this.pin = pin;
        this.client = client;
        this.clientThread = clientThread;
    }

    /** Whether the menu entry is offered. The panel turns it on only when sea trips can run. */
    public boolean isVisible() { return visible; }

    public void setVisible(boolean visible) { this.visible = visible; }

    /** Re-read the live boats and the saved pin. Call from the panel rebuild. Does nothing
     *  until the player is logged in, since the boat varbits mean nothing before then. */
    public void refresh()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        clientThread.invoke(() -> {
            List<SailingBoats.BoatInfo> list = boats.mooredBoats();
            boolean enabled = pin.isEnabled();
            int slot = pin.slot();
            SwingUtilities.invokeLater(() -> render(list, enabled, slot));
            return true;
        });
    }

    void render(List<SailingBoats.BoatInfo> moored, boolean enabled, int savedSlot)
    {
        this.moored = moored;
        this.pinned = enabled;
        this.pinnedSlot = savedSlot;
    }

    /** The submenu: the default choice first, then one entry per moored boat. */
    JMenu buildMenu()
    {
        JMenu menu = new JMenu(MENU_TITLE);
        ButtonGroup group = new ButtonGroup();

        JRadioButtonMenuItem nearest = new JRadioButtonMenuItem(NEAREST, !pinned);
        nearest.addActionListener(e -> choose(false, -1, ""));
        group.add(nearest);
        menu.add(nearest);

        if (moored.isEmpty())
        {
            JMenuItem none = new JMenuItem("No boats moored at a port");
            none.setEnabled(false);
            menu.add(none);
            return menu;
        }
        menu.addSeparator();
        for (SailingBoats.BoatInfo b : moored)
        {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                b.name + " (" + b.dock.getDockName() + ")", pinned && b.slot == pinnedSlot);
            // The journey compares the bare boat name, so that is what gets stored.
            item.addActionListener(e -> choose(true, b.slot, b.name));
            group.add(item);
            menu.add(item);
        }
        return menu;
    }

    private void choose(boolean enabled, int slot, String name)
    {
        pinned = enabled;
        pinnedSlot = slot;
        pin.set(enabled, slot, name);
    }
}
