package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import java.awt.Component;
import java.awt.GridLayout;
import javax.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Production gate: opens a Swing confirm dialog. Test code uses a fake impl of the interface. */
@Singleton
public class SwingWildernessConfirmGate implements WildernessConfirmGate
{
    @Override
    public Result prompt(Component parent, Waypoint w)
    {
        JPanel msg = new JPanel(new GridLayout(0, 1, 0, 4));
        msg.setOpaque(false);
        msg.add(new JLabel("Path to '" + w.getName() + "'?"));
        msg.add(new JLabel("This destination is in the Wilderness."));
        JCheckBox dontAsk = new JCheckBox("Don't ask again for this waypoint");
        msg.add(dontAsk);

        int r = JOptionPane.showConfirmDialog(parent, msg, "Wilderness destination",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return new Result(r == JOptionPane.OK_OPTION, dontAsk.isSelected());
    }
}
