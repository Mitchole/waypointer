package com.waypointer.ui;

import com.waypointer.codec.PresetOverridesCodec;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.PresetImportResolver;
import com.waypointer.service.PresetOverrides;
import com.waypointer.service.PresetOverridesSnapshot.Waypoint;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.util.Listeners.Subscription;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.ui.ColorScheme;

@Singleton
public class PresetEditorPanel extends JPanel
{
    private final PresetCatalog catalog;
    private final PresetOverrides overrides;
    private final WaypointCapture capture;
    private final WaypointPathfinder pathfinder;
    private final JComboBox<String> categoryPicker = new JComboBox<>();
    private final PlaceholderTextField searchField = new PlaceholderTextField("Search by name");
    private final JButton addWaypointBtn = new JButton("+ Add waypoint");
    private final JButton addCategoryBtn = new JButton("+ Add category");
    private final JButton removeCategoryBtn = new JButton("Remove category");
    private final JButton importBtn = new JButton("Import contributor file");
    private final JButton exportBtn = new JButton("Export changes");
    private final PresetOverridesCodec presetOverridesCodec;
    private final JPanel body = new JPanel();
    private JPanel activeInline = null;

    private Subscription catalogSub;

    @Inject
    public PresetEditorPanel(PresetCatalog catalog, PresetOverrides overrides, WaypointCapture capture,
        WaypointPathfinder pathfinder, PresetOverridesCodec presetOverridesCodec)
    {
        this.catalog = catalog;
        this.overrides = overrides;
        this.capture = capture;
        this.pathfinder = pathfinder;
        this.presetOverridesCodec = presetOverridesCodec;
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
        Styles.secondaryButton(importBtn);
        Styles.secondaryButton(exportBtn);
        header.add(categoryPicker);
        header.add(Box.createVerticalStrut(4));
        header.add(searchField);
        header.add(Box.createVerticalStrut(4));
        header.add(addWaypointBtn);
        header.add(Box.createVerticalStrut(4));
        header.add(addCategoryBtn);
        header.add(Box.createVerticalStrut(4));
        header.add(removeCategoryBtn);
        header.add(Box.createVerticalStrut(4));
        header.add(importBtn);
        header.add(Box.createVerticalStrut(4));
        header.add(exportBtn);
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
        importBtn.addActionListener(e -> runImport());
        exportBtn.addActionListener(e -> runExport());

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
            PresetWaypointRow row = new PresetWaypointRow(w, this::navigateTo, this::openEdit, this::onDelete);
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

    // Path to the preset waypoint via the Shortest Path plugin.
    void navigateTo(PresetWaypoint w)
    {
        if (!pathfinder.isAvailable())
        {
            showToast("Install the Shortest Path plugin to navigate.");
            return;
        }
        pathfinder.requestPath(WorldPointPacker.pack(w.getX(), w.getY(), w.getPlane()), w.getName());
    }

    private void runImport()
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Preset JSON", "json"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String json;
        try
        {
            json = new String(Files.readAllBytes(fc.getSelectedFile().toPath()));
        }
        catch (IOException ex)
        {
            showToast("Couldn't read that file. Check permissions.");
            return;
        }
        List<Preset> imported = catalog.parse(json);
        if (imported.isEmpty())
        {
            showToast("No presets found in that file.");
            return;
        }
        PresetImportResolver resolver =
            new PresetImportResolver(catalog.getPresets(), imported);
        PresetImportResolver.Choice applyAll = null;
        for (PresetImportResolver.PendingConflict c : resolver.conflicts())
        {
            if (applyAll != null) { resolver.resolve(c, applyAll); continue; }
            ImportConflictDialog.Result r = ImportConflictDialog.prompt(
                SwingUtilities.getWindowAncestor(this), c);
            resolver.resolve(c, r.choice);
            if (r.applyToAll) applyAll = r.choice;
        }
        for (PresetWaypoint w : resolver.staged())
        {
            String cat = findCategoryOf(imported, w);
            if (cat == null) continue;
            overrides.upsertWaypoint(cat, null,
                new Waypoint(w.getName(), w.getDescription() == null ? "" : w.getDescription(),
                    w.getX(), w.getY(), w.getPlane()));
        }
        showToast("Imported " + resolver.staged().size() + " waypoints.");
    }

    private void runExport()
    {
        String json = presetOverridesCodec.encode(overrides.getSnapshot());
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(json), null);
        showToast("Override snapshot copied to clipboard.");
    }

    private String findCategoryOf(List<Preset> presets, PresetWaypoint target)
    {
        for (Preset p : presets)
        {
            for (PresetWaypoint w : p.getWaypoints())
            {
                if (w == target) return p.getCategory();
            }
        }
        return null;
    }

    private void showToast(String msg)
    {
        JOptionPane.showMessageDialog(this, msg, "Waypointer", JOptionPane.INFORMATION_MESSAGE);
    }
}
