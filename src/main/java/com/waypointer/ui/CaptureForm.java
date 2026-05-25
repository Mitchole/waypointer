package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointStore;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.UUID;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
    private final JButton saveBtn = new JButton("Save");
    private final JButton cancelBtn = new JButton("Cancel");

    private final JPanel newCategoryRow = new JPanel(new GridBagLayout());
    private final JTextField newCategoryName = new JTextField();
    private final JButton newCategoryCreate = new JButton("Create");
    private final JButton newCategoryCancel = new JButton("Cancel");
    private CategoryComboItem lastNonSentinelSelection;

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

        newCategoryRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        newCategoryRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        newCategoryRow.setVisible(false);
        Styles.textField(newCategoryName);
        Styles.compactSecondaryButton(newCategoryCreate);
        Styles.compactSecondaryButton(newCategoryCancel);
        newCategoryCreate.addActionListener(e -> doCreateInlineCategory());
        newCategoryCancel.addActionListener(e -> cancelInlineCategory());
        GridBagConstraints gn = new GridBagConstraints();
        gn.fill = GridBagConstraints.HORIZONTAL;
        gn.insets = new Insets(2, 2, 2, 2);
        gn.gridx = 0; gn.gridy = 0; gn.weightx = 1;
        newCategoryRow.add(newCategoryName, gn);
        gn.gridx = 1; gn.weightx = 0;
        newCategoryRow.add(newCategoryCreate, gn);
        gn.gridx = 2;
        newCategoryRow.add(newCategoryCancel, gn);
        stack.add(newCategoryRow);

        add(stack, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        Styles.secondaryButton(cancelBtn);
        cancelBtn.addActionListener(e -> dismiss());
        Styles.primaryButton(saveBtn);
        saveBtn.addActionListener(e -> doSave());
        buttons.add(cancelBtn);
        buttons.add(saveBtn);
        add(buttons, BorderLayout.SOUTH);

        // ESC cancels, ENTER on the name field saves. Both bound on the form's own
        // input map so they don't fire when focus is somewhere else in the panel.
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "captureCancel");
        getActionMap().put("captureCancel", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e) { dismiss(); }
        });
        nameField.getInputMap(JComponent.WHEN_FOCUSED)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "captureSave");
        nameField.getActionMap().put("captureSave", new AbstractAction()
        {
            @Override public void actionPerformed(ActionEvent e) { saveBtn.doClick(); }
        });

        categoryCombo.addActionListener(e -> onCategorySelectionChanged());
        setVisible(false);
    }

    @Override
    public void addNotify()
    {
        super.addNotify();
        JRootPane root = getRootPane();
        if (root != null) root.setDefaultButton(saveBtn);
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
            lastNonSentinelSelection = toSelect;
        }

        for (ActionListener l : listeners) categoryCombo.addActionListener(l);
    }

    private void onCategorySelectionChanged()
    {
        CategoryComboItem sel = (CategoryComboItem) categoryCombo.getSelectedItem();
        if (sel == null) return;
        if (!sel.isSentinel())
        {
            lastNonSentinelSelection = sel;
            newCategoryRow.setVisible(false);
            revalidate();
            repaint();
            return;
        }
        newCategoryName.setText("");
        newCategoryRow.setVisible(true);
        revalidate();
        repaint();
        newCategoryName.requestFocusInWindow();
    }

    private void doCreateInlineCategory()
    {
        String name = newCategoryName.getText();
        if (name == null || name.trim().isEmpty()) return;
        Category created = store.createCategory(name.trim());
        rebuildCategoryCombo(created.getId());
        newCategoryRow.setVisible(false);
        revalidate();
        repaint();
    }

    private void cancelInlineCategory()
    {
        UUID revertTo = lastNonSentinelSelection != null
            ? lastNonSentinelSelection.id()
            : store.getUncategorized().getId();
        rebuildCategoryCombo(revertTo);
        newCategoryRow.setVisible(false);
        revalidate();
        repaint();
    }

    private void doSave()
    {
        String typed = nameField.getText();
        String trimmed = typed == null ? "" : typed.trim();
        if (trimmed.isEmpty()) trimmed = capture.defaultName(packedPoint);

        CategoryComboItem sel = (CategoryComboItem) categoryCombo.getSelectedItem();
        UUID categoryId = (sel == null || sel.isSentinel())
            ? store.getUncategorized().getId() : sel.id();

        store.createWaypoint(packedPoint, trimmed, categoryId);
        dismiss();
    }

    void setNameText(String text)
    {
        nameField.setText(text);
    }

    void selectCategoryByName(String name)
    {
        for (int i = 0; i < categoryCombo.getItemCount(); i++)
        {
            CategoryComboItem item = categoryCombo.getItemAt(i);
            if (!item.isSentinel() && name.equals(item.toString()))
            {
                categoryCombo.setSelectedIndex(i);
                return;
            }
        }
        throw new IllegalArgumentException("No category named: " + name);
    }

    void clickSave()
    {
        saveBtn.doClick();
    }

    void clickCancel()
    {
        cancelBtn.doClick();
    }

    boolean isNewCategoryRowVisible()
    {
        return newCategoryRow.isVisible();
    }

    void selectNewCategorySentinel()
    {
        for (int i = 0; i < categoryCombo.getItemCount(); i++)
        {
            if (categoryCombo.getItemAt(i).isSentinel())
            {
                categoryCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    void setNewCategoryNameText(String text)
    {
        newCategoryName.setText(text);
    }

    void clickCreateNewCategory()
    {
        newCategoryCreate.doClick();
    }

    void clickCancelNewCategory()
    {
        newCategoryCancel.doClick();
    }

    Object selectedCategoryItem()
    {
        return categoryCombo.getSelectedItem();
    }
}
