package com.waypointer.ui;

import com.waypointer.sailing.SailingBoatPin;
import com.waypointer.sailing.SailingBoats;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;

/** Sidebar strip: optionally pin a specific boat for ocean journeys. Reads the live boat list
 *  on the client thread, renders on the EDT. The persisted choice is the boat slot. */
public class SailingControls extends JPanel
{
    private final SailingBoats boats;
    private final SailingBoatPin pin;
    private final Client client;
    private final ClientThread clientThread;

    final JCheckBox useSpecific = new JCheckBox("Always use a specific boat");
    private final JComboBox<BoatChoice> combo = new JComboBox<>();

    private boolean updating;

    public SailingControls(SailingBoats boats, SailingBoatPin pin, Client client, ClientThread clientThread)
    {
        this.boats = boats;
        this.pin = pin;
        this.client = client;
        this.clientThread = clientThread;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        Styles.checkBox(useSpecific);
        Styles.combo(combo);

        JPanel comboRow = new JPanel(new BorderLayout());
        comboRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        comboRow.add(combo, BorderLayout.CENTER);

        add(useSpecific);
        add(comboRow);

        useSpecific.addActionListener(e -> { combo.setEnabled(useSpecific.isSelected()); persist(); });
        combo.addActionListener(e -> persist());
    }

    /** Re-read the live boats and the saved pin. Call from the panel rebuild. Does nothing
     *  until the player is logged in, since the boat varbits mean nothing before then. */
    public void refresh()
    {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        clientThread.invoke(() -> {
            List<SailingBoats.BoatInfo> moored = boats.mooredBoats();
            boolean enabled = pin.isEnabled();
            int slot = pin.slot();
            SwingUtilities.invokeLater(() -> render(moored, enabled, slot));
            return true;
        });
    }

    void render(List<SailingBoats.BoatInfo> moored, boolean enabled, int savedSlot)
    {
        updating = true;
        try
        {
            DefaultComboBoxModel<BoatChoice> model = new DefaultComboBoxModel<>();
            BoatChoice toSelect = null;
            for (SailingBoats.BoatInfo b : moored)
            {
                BoatChoice c = new BoatChoice(b.slot, b.name, b.name + " . " + b.dock.getDockName());
                model.addElement(c);
                if (b.slot == savedSlot) toSelect = c;
            }
            combo.setModel(model);
            if (toSelect != null) combo.setSelectedItem(toSelect);
            useSpecific.setSelected(enabled);
            combo.setEnabled(enabled && model.getSize() > 0);
        }
        finally
        {
            updating = false;
        }
    }

    private void persist()
    {
        if (updating) return;
        BoatChoice c = (BoatChoice) combo.getSelectedItem();
        int slot = c == null ? -1 : c.slot;
        String name = c == null ? "" : c.name;
        pin.set(useSpecific.isSelected(), slot, name);
    }

    private static final class BoatChoice
    {
        final int slot;
        /** The bare boat name, as the journey compares it. */
        final String name;
        final String label;
        BoatChoice(int slot, String name, String label) { this.slot = slot; this.name = name; this.label = label; }
        @Override public String toString() { return label; }
    }
}
