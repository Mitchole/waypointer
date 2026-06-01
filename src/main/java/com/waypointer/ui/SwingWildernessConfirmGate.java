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

        // Cancel is the default-focused button so a stray Enter does not start a Wilderness path.
        String[] options = {"Path anyway", "Cancel"};
        int choice = JOptionPane.showOptionDialog(parent, msg, "Wilderness destination",
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return new Result(choice == 0, dontAsk.isSelected());
    }
}
