package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.PresetOverrides;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import com.waypointer.service.WaypointCapture;
import com.waypointer.util.Listeners.Subscription;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;

@Singleton
public class PresetEditorPanel extends JPanel
{
    private final PresetCatalog catalog;
    private final PresetOverrides overrides;
    private final WaypointCapture capture;
    private final JComboBox<String> categoryPicker = new JComboBox<>();
    private final PlaceholderTextField searchField = new PlaceholderTextField("Search by name");
    private final JButton addWaypointBtn = new JButton("+ Add waypoint");
    private final JButton addCategoryBtn = new JButton("+ Add category");
    private final JButton removeCategoryBtn = new JButton("Remove category");
    private final JPanel body = new JPanel();
    private JPanel activeInline = null;

    private Subscription catalogSub;

    @Inject
    public PresetEditorPanel(PresetCatalog catalog, PresetOverrides overrides, WaypointCapture capture)
    {
        this.catalog = catalog;
        this.overrides = overrides;
        this.capture = capture;
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        Styles.combo(categoryPicker);
        Styles.textField(searchField);
        Styles.secondaryButton(addWaypointBtn);
        Styles.secondaryButton(addCategoryBtn);
        Styles.secondaryButton(removeCategoryBtn);
        header.add(categoryPicker);
        header.add(Box.createVerticalStrut(4));
        header.add(searchField);
        header.add(Box.createVerticalStrut(4));
        header.add(addWaypointBtn);
        header.add(Box.createVerticalStrut(4));
        header.add(addCategoryBtn);
        header.add(Box.createVerticalStrut(4));
        header.add(removeCategoryBtn);
        add(header, BorderLayout.NORTH);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JScrollPane scroll = new JScrollPane(body,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(scroll, BorderLayout.CENTER);

        categoryPicker.addActionListener(e -> { closeInline(); rebuild(); });
        searchField.getDocument().addDocumentListener(Styles.documentListener(this::rebuild));
        addWaypointBtn.addActionListener(e -> openAddWaypoint());
        addCategoryBtn.addActionListener(e -> openAddCategory());
        removeCategoryBtn.addActionListener(e -> removeCurrentCategory());

        catalogSub = catalog.subscribe(() -> SwingUtilities.invokeLater(this::reloadCategoriesAndRebuild));
        reloadCategoriesAndRebuild();
    }

    public void dispose()
    {
        if (catalogSub != null) { catalogSub.close(); catalogSub = null; }
    }

    private void reloadCategoriesAndRebuild()
    {
        String prev = (String) categoryPicker.getSelectedItem();
        categoryPicker.removeAllItems();
        for (Preset p : catalog.getPresets()) categoryPicker.addItem(p.getCategory());
        if (prev != null)
        {
            for (int i = 0; i < categoryPicker.getItemCount(); i++)
            {
                if (prev.equals(categoryPicker.getItemAt(i))) { categoryPicker.setSelectedIndex(i); break; }
            }
        }
        rebuild();
    }

    private void rebuild()
    {
        body.removeAll();
        if (activeInline != null) body.add(activeInline);
        String category = (String) categoryPicker.getSelectedItem();
        Preset preset = findPreset(category);
        removeCategoryBtn.setEnabled(preset != null && preset.getWaypoints().isEmpty());
        if (preset == null) { body.revalidate(); body.repaint(); return; }
        String query = searchField.getText().toLowerCase();
        for (PresetWaypoint w : preset.getWaypoints())
        {
            if (!query.isEmpty() && !w.getName().toLowerCase().contains(query)) continue;
            PresetWaypointRow row = new PresetWaypointRow(category, w, this::openEdit, this::onDelete);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            body.add(row);
        }
        body.revalidate();
        body.repaint();
    }

    private Preset findPreset(String category)
    {
        if (category == null) return null;
        for (Preset p : catalog.getPresets())
        {
            if (category.equals(p.getCategory())) return p;
        }
        return null;
    }

    private void openAddWaypoint()
    {
        closeInline();
        String category = (String) categoryPicker.getSelectedItem();
        if (category == null) return;
        capture.readCurrentLocation(packed -> {
            if (packed == WorldPointPacker.UNDEFINED)
            {
                showToast("Log in to capture a tile.");
                return;
            }
            overrides.upsertWaypoint(category, null,
                new Waypoint("New waypoint", "",
                    WorldPointPacker.getX(packed),
                    WorldPointPacker.getY(packed),
                    WorldPointPacker.getPlane(packed)));
        });
    }

    private void openAddCategory()
    {
        closeInline();
        activeInline = new AddCategoryForm(overrides, this::closeInline, this::showToast);
        rebuild();
    }

    private void removeCurrentCategory()
    {
        String category = (String) categoryPicker.getSelectedItem();
        if (category == null) return;
        Preset preset = findPreset(category);
        if (preset == null || !preset.getWaypoints().isEmpty()) return;
        overrides.deleteCategory(category);
    }

    private void openEdit(PresetWaypoint w)
    {
        closeInline();
        String category = (String) categoryPicker.getSelectedItem();
        if (category == null) return;
        activeInline = new InlinePresetWaypointEdit(category, w, overrides, capture, this::closeInline);
        rebuild();
    }

    private void closeInline()
    {
        activeInline = null;
        rebuild();
    }

    private void onDelete(PresetWaypoint w)
    {
        String category = (String) categoryPicker.getSelectedItem();
        if (category == null) return;
        overrides.deleteBundledWaypoint(category, w.getName(), w.getX(), w.getY(), w.getPlane());
    }

    private void showToast(String msg)
    {
        JOptionPane.showMessageDialog(this, msg, "Waypointer", JOptionPane.INFORMATION_MESSAGE);
    }
}
