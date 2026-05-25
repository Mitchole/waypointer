package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointStore;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;

/**
 * Inline capture form parked at the top of the body. Replaces the application-modal
 * CaptureDialog for the panel's Mark button so saving a waypoint never steals focus
 * from the game canvas.
 */
class CaptureForm extends JPanel
{
    private final WaypointStore store;
    private final WaypointCapture capture;

    private final JTextField nameField = new JTextField();
    private final JComboBox<CategoryComboItem> categoryCombo = new JComboBox<>();
    private final JPanel stack = new JPanel();

    private int packedPoint;

    CaptureForm(WaypointStore store, WaypointCapture capture)
    {
        this.store = store;
        this.capture = capture;

        setLayout(new BorderLayout(0, 4));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel caption = new JLabel("Save waypoint");
        caption.setForeground(java.awt.Color.WHITE);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(caption);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ColorScheme.DARK_GRAY_COLOR);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 2, 2, 2);

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        form.add(Styles.fieldLabel("Name"), g);
        Styles.textField(nameField);
        g.gridx = 1; g.weightx = 1;
        form.add(nameField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        form.add(Styles.fieldLabel("Category"), g);
        Styles.combo(categoryCombo);
        g.gridx = 1; g.weightx = 1;
        form.add(categoryCombo, g);

        stack.add(form);

        add(stack, BorderLayout.CENTER);
        setVisible(false);
    }

    void show(int packed)
    {
        this.packedPoint = packed;
        rebuildCategoryCombo(store.getUncategorized().getId());
        nameField.setText(capture.defaultName(packed));
        nameField.selectAll();
        setVisible(true);
        revalidate();
        repaint();
    }

    void dismiss()
    {
        setVisible(false);
        revalidate();
        repaint();
    }

    String getNameText()
    {
        return nameField.getText();
    }

    private void rebuildCategoryCombo(UUID selectId)
    {
        categoryCombo.removeAllItems();
        CategoryComboItem toSelect = null;
        for (Category c : store.getCategoriesOrdered())
        {
            CategoryComboItem item = new CategoryComboItem(c);
            categoryCombo.addItem(item);
            if (c.getId().equals(selectId)) toSelect = item;
        }
        categoryCombo.addItem(CategoryComboItem.sentinel("+ New category..."));
        if (toSelect != null) categoryCombo.setSelectedItem(toSelect);
    }
}
