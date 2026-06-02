package com.waypointer.ui;

import com.waypointer.service.PresetImportResolver.Choice;
import com.waypointer.service.PresetImportResolver.PendingConflict;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

public final class ImportConflictDialog
{
    public static final class Result
    {
        public final Choice choice;
        public final boolean applyToAll;
        public Result(Choice c, boolean a) { this.choice = c; this.applyToAll = a; }
    }

    public static Result prompt(Window owner, PendingConflict c)
    {
        Result[] out = { new Result(Choice.SKIP, false) };
        JDialog d = new JDialog(owner, "Conflict in " + c.category, JDialog.ModalityType.APPLICATION_MODAL);
        Dialogs.applyDarkContentPane(d);
        Dialogs.bindEscape(d);

        JLabel msg = new JLabel("'" + c.imported.getName() + "' already exists.");
        msg.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        d.add(msg, BorderLayout.NORTH);

        JCheckBox applyAll = new JCheckBox("Apply to all conflicts");
        applyAll.setBackground(ColorScheme.DARK_GRAY_COLOR);
        applyAll.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        d.add(applyAll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton keep = new JButton("Keep existing");
        Styles.secondaryButton(keep);
        JButton repl = new JButton("Replace with imported");
        Styles.secondaryButton(repl);
        JButton skip = new JButton("Skip");
        Styles.secondaryButton(skip);
        keep.addActionListener(e -> { out[0] = new Result(Choice.KEEP_EXISTING, applyAll.isSelected()); d.dispose(); });
        repl.addActionListener(e -> { out[0] = new Result(Choice.REPLACE, applyAll.isSelected()); d.dispose(); });
        skip.addActionListener(e -> { out[0] = new Result(Choice.SKIP, applyAll.isSelected()); d.dispose(); });
        buttons.add(keep);
        buttons.add(repl);
        buttons.add(skip);
        d.add(buttons, BorderLayout.SOUTH);

        Dialogs.finish(d, owner);
        d.setVisible(true);
        return out[0];
    }
}
