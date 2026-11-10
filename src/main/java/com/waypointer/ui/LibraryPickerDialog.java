package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.ColorScheme;

/**
 * Modal picker for adding saved waypoints to a route: the export dialog's tree, starting with
 * nothing checked. Confirm hands the checked waypoints, in library order, to {@code onAdd}.
 */
final class LibraryPickerDialog extends JDialog
{
    private final WaypointPickerModel model;
    private final WaypointTreePicker tree;
    private final Consumer<List<Waypoint>> onAdd;
    private final JButton addBtn = new JButton("Add to route");

    LibraryPickerDialog(Window owner, Library library, Consumer<List<Waypoint>> onAdd)
    {
        super(owner, "From saved", Dialog.ModalityType.APPLICATION_MODAL);
        this.onAdd = onAdd;
        this.model = new WaypointPickerModel(library);
        model.selectNone();
        this.tree = new WaypointTreePicker(model, this::updateButtons);

        JPanel content = Dialogs.applyDarkContentPane(this);
        Dialogs.bindEscape(this);

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.setPreferredSize(new Dimension(320, 360));
        content.add(scroll, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);

        updateButtons();
        getRootPane().setDefaultButton(addBtn);
        Dialogs.finish(this, owner);
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton cancel = new JButton("Cancel");
        Styles.secondaryButton(cancel);
        Styles.primaryButton(addBtn);
        cancel.addActionListener(e -> dispose());
        addBtn.addActionListener(e ->
        {
            onAdd.accept(model.checkedWaypointsInOrder());
            dispose();
        });
        footer.add(cancel);
        footer.add(addBtn);
        return footer;
    }

    private void updateButtons()
    {
        addBtn.setEnabled(!model.checkedWaypointsInOrder().isEmpty());
    }
}
