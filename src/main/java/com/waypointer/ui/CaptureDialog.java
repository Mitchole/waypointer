package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointStore;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.UUID;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import net.runelite.client.ui.ColorScheme;

// Modal dialog for picking name + category before the new waypoint commits to the store.
// No callback: WaypointStore.createWaypoint fires observers that trigger the panel rebuild.
public class CaptureDialog extends JDialog
{
    private final WaypointStore store;
    private final int packedPoint;
    private final String targetNpcName;

    private JComboBox<CategoryComboItem> categoryCombo;
    private CategoryComboItem lastSelected;

    public CaptureDialog(Window owner, WaypointStore store, WaypointCapture capture, int packedPoint)
    {
        this(owner, store, capture, packedPoint, null, null);
    }

    public CaptureDialog(Window owner, WaypointStore store, WaypointCapture capture, int packedPoint,
        String defaultNameOverride, String targetNpcName)
    {
        super(owner, "Save waypoint", Dialog.ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.store = store;
        this.packedPoint = packedPoint;
        this.targetNpcName = targetNpcName;

        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);

        String defaultName = (defaultNameOverride != null && !defaultNameOverride.isEmpty())
            ? defaultNameOverride : capture.defaultName(packedPoint);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(ColorScheme.DARK_GRAY_COLOR);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        form.add(Styles.fieldLabel("Name"), g);
        JTextField nameField = new JTextField(defaultName);
        Styles.textField(nameField);
        nameField.selectAll();
        g.gridx = 1; g.weightx = 1;
        form.add(nameField, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        form.add(Styles.fieldLabel("Category"), g);
        categoryCombo = new JComboBox<>();
        Styles.combo(categoryCombo);
        rebuildCategoryCombo(store.getUncategorized().getId());
        categoryCombo.addActionListener(e -> handleCategorySelection());
        g.gridx = 1; g.weightx = 1;
        form.add(categoryCombo, g);

        content.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton cancel = new JButton("Cancel");
        Styles.secondaryButton(cancel);
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Save");
        Styles.primaryButton(save);
        save.addActionListener(e -> doSave(nameField.getText(), defaultName));
        buttons.add(cancel);
        buttons.add(save);
        content.add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(save);
        // ESC closes (Cancel)
        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Enter on the name field triggers save (default button already does this, but be safe).
        nameField.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "save");
        nameField.getActionMap().put("save", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e) { save.doClick(); }
        });

        pack();
        setMinimumSize(new java.awt.Dimension(280, getHeight()));
        setLocationRelativeTo(owner);
    }

    private void doSave(String typedName, String defaultName)
    {
        String trimmed = typedName == null ? "" : typedName.trim();
        if (trimmed.isEmpty()) trimmed = defaultName;
        CategoryComboItem sel = (CategoryComboItem) categoryCombo.getSelectedItem();
        UUID categoryId = (sel == null || sel.isSentinel())
            ? store.getUncategorized().getId() : sel.id();
        store.createWaypoint(packedPoint, trimmed, categoryId, targetNpcName);
        dispose();
    }

    private void handleCategorySelection()
    {
        CategoryComboItem sel = (CategoryComboItem) categoryCombo.getSelectedItem();
        if (sel == null) return;
        if (!sel.isSentinel())
        {
            lastSelected = sel;
            return;
        }
        // "+ New category..." chosen: prompt for a name
        String name = JOptionPane.showInputDialog(this, "New category name:");
        if (name == null || name.trim().isEmpty())
        {
            // revert
            revertSelection();
            return;
        }
        try
        {
            Category created = store.createCategory(name.trim());
            rebuildCategoryCombo(created.getId());
        }
        catch (IllegalArgumentException ex)
        {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Waypointer",
                JOptionPane.WARNING_MESSAGE);
            revertSelection();
        }
    }

    private void revertSelection()
    {
        UUID id = lastSelected != null ? lastSelected.id() : store.getUncategorized().getId();
        rebuildCategoryCombo(id);
    }

    private void rebuildCategoryCombo(UUID selectId)
    {
        // suppress action events while rebuilding
        ActionListener[] listeners = categoryCombo.getActionListeners();
        for (ActionListener l : listeners) categoryCombo.removeActionListener(l);

        categoryCombo.removeAllItems();
        CategoryComboItem toSelect = null;
        for (Category c : store.getCategoriesOrdered())
        {
            CategoryComboItem item = new CategoryComboItem(c);
            categoryCombo.addItem(item);
            if (c.getId().equals(selectId)) toSelect = item;
        }
        categoryCombo.addItem(CategoryComboItem.sentinel("+ New category..."));
        if (toSelect != null)
        {
            categoryCombo.setSelectedItem(toSelect);
            lastSelected = toSelect;
        }

        for (ActionListener l : listeners) categoryCombo.addActionListener(l);
    }
}
