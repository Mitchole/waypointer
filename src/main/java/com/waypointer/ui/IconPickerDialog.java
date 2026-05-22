package com.waypointer.ui;

import com.waypointer.service.IconCatalog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

/** Modal icon picker for a category or waypoint. Category dropdown drives a 5x10 grid
 *  paginated 50 per page. ESC closes without changing anything. Opens to the category and
 *  page containing the current icon when there is one. */
@SuppressWarnings("deprecation") // SpriteID is deprecated upstream but still the public surface.
public class IconPickerDialog extends JDialog
{
    private static final int PAGE_SIZE = 50;
    private static final int GRID_COLS = 5;
    private static final int GRID_ROWS = 10;

    private final SpriteManager spriteManager;
    private final Consumer<Integer> onSelect;
    private final Integer currentIconId;
    private final List<JLabel> cellLabels = new ArrayList<>(PAGE_SIZE);

    private final List<CategoryView> categories;
    private int currentCategoryIdx;
    private int totalPages;
    private int currentPage;

    private JComboBox<String> categoryCombo;
    private JButton prevBtn;
    private JButton nextBtn;
    private JLabel pageLabel;

    public IconPickerDialog(Window owner, SpriteManager spriteManager, IconCatalog catalog,
        Integer currentIconId, Consumer<Integer> onSelect)
    {
        super(owner, "Pick icon", ModalityType.APPLICATION_MODAL);
        this.spriteManager = spriteManager;
        this.onSelect = onSelect;
        this.currentIconId = currentIconId;
        this.categories = buildCategoryViews(catalog);

        // Find the category that currently owns the active icon, if any.
        this.currentCategoryIdx = findCategoryFor(currentIconId);
        recomputePagination();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 6));
        root.setBackground(ColorScheme.DARK_GRAY_COLOR);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Choose an icon", SwingConstants.LEFT);
        title.setForeground(Color.WHITE);
        root.add(title, BorderLayout.NORTH);

        // Center stack: category combo + None row (NORTH), grid (CENTER), pagination (SOUTH).
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel topStack = new JPanel(new BorderLayout(0, 6));
        topStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        topStack.add(buildCategoryRow(), BorderLayout.NORTH);
        JPanel noneRow = new JPanel(new BorderLayout());
        noneRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
        noneRow.add(buildNoneCell(currentIconId == null), BorderLayout.CENTER);
        topStack.add(noneRow, BorderLayout.SOUTH);
        center.add(topStack, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(GRID_ROWS, GRID_COLS, 4, 4));
        grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
        for (int i = 0; i < PAGE_SIZE; i++)
        {
            JLabel cell = buildIconCell();
            cellLabels.add(cell);
            grid.add(cell);
        }
        center.add(grid, BorderLayout.CENTER);

        center.add(buildPaginationFooter(), BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);

        setContentPane(root);
        bindEscapeToClose(root);

        renderPage(currentPage);

        pack();
        setLocationRelativeTo(owner);
    }

    private void recomputePagination()
    {
        List<Integer> ids = currentCategoryIds();
        this.totalPages = Math.max(1, (ids.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        this.currentPage = computeInitialPage(currentIconId, ids, PAGE_SIZE);
    }

    private List<Integer> currentCategoryIds()
    {
        if (categories.isEmpty()) return Collections.emptyList();
        return categories.get(currentCategoryIdx).ids;
    }

    private int findCategoryFor(Integer iconId)
    {
        if (iconId == null) return 0;
        for (int i = 0; i < categories.size(); i++)
        {
            if (categories.get(i).ids.contains(iconId)) return i;
        }
        return 0;
    }

    static int computeInitialPage(Integer currentIconId, List<Integer> sortedIds, int pageSize)
    {
        if (currentIconId == null) return 0;
        int idx = Collections.binarySearch(sortedIds, currentIconId);
        if (idx < 0) return 0;
        return idx / pageSize;
    }

    private JComponent buildCategoryRow()
    {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JLabel label = new JLabel("Category:");
        label.setForeground(Color.LIGHT_GRAY);
        row.add(label, BorderLayout.WEST);

        String[] names = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) names[i] = categories.get(i).label;
        categoryCombo = new JComboBox<>(names);
        categoryCombo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        categoryCombo.setForeground(Color.WHITE);
        if (!categories.isEmpty()) categoryCombo.setSelectedIndex(currentCategoryIdx);
        categoryCombo.addActionListener(e -> {
            int sel = categoryCombo.getSelectedIndex();
            if (sel < 0 || sel == currentCategoryIdx) return;
            currentCategoryIdx = sel;
            // Start at page 0 on manual category switch; finding the current icon in the
            // newly-selected list rarely matches user intent.
            currentPage = 0;
            totalPages = Math.max(1, (currentCategoryIds().size() + PAGE_SIZE - 1) / PAGE_SIZE);
            renderPage(0);
        });
        row.add(categoryCombo, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildPaginationFooter()
    {
        JPanel footer = new JPanel(new BorderLayout(6, 0));
        footer.setBackground(ColorScheme.DARK_GRAY_COLOR);

        prevBtn = new JButton("< Prev");
        Styles.secondaryButton(prevBtn);
        prevBtn.addActionListener(e -> {
            if (currentPage > 0) renderPage(currentPage - 1);
        });

        nextBtn = new JButton("Next >");
        Styles.secondaryButton(nextBtn);
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages - 1) renderPage(currentPage + 1);
        });

        pageLabel = new JLabel("", SwingConstants.CENTER);
        pageLabel.setForeground(Color.LIGHT_GRAY);

        footer.add(prevBtn, BorderLayout.WEST);
        footer.add(pageLabel, BorderLayout.CENTER);
        footer.add(nextBtn, BorderLayout.EAST);
        return footer;
    }

    private void renderPage(int page)
    {
        currentPage = page;
        List<Integer> ids = currentCategoryIds();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, ids.size());

        for (int i = 0; i < PAGE_SIZE; i++)
        {
            JLabel cell = cellLabels.get(i);
            for (java.awt.event.MouseListener ml : cell.getMouseListeners())
            {
                cell.removeMouseListener(ml);
            }
            cell.setIcon(null);

            int globalIdx = start + i;
            if (globalIdx >= end)
            {
                cell.setVisible(false);
                cell.setToolTipText(null);
                styleCell(cell, false);
                continue;
            }

            cell.setVisible(true);
            final int spriteId = ids.get(globalIdx);
            boolean selected = currentIconId != null && currentIconId == spriteId;
            cell.setToolTipText("Sprite " + spriteId);
            styleCell(cell, selected);

            spriteManager.getSpriteAsync(spriteId, 0, img -> {
                if (img == null) return;
                SwingUtilities.invokeLater(() -> {
                    if (("Sprite " + spriteId).equals(cell.getToolTipText()))
                    {
                        cell.setIcon(new ImageIcon(img));
                        cell.revalidate();
                        cell.repaint();
                    }
                });
            });

            final boolean cellSelected = selected;
            cell.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e)
                {
                    onSelect.accept(spriteId);
                    dispose();
                }
                @Override public void mouseEntered(MouseEvent e)
                {
                    if (!cellSelected) hover(cell, true);
                }
                @Override public void mouseExited(MouseEvent e)
                {
                    if (!cellSelected) hover(cell, false);
                }
            });
        }

        if (prevBtn != null) prevBtn.setEnabled(currentPage > 0);
        if (nextBtn != null) nextBtn.setEnabled(currentPage < totalPages - 1);
        if (pageLabel != null)
        {
            pageLabel.setText(String.format("Page %d of %d (%d icons)",
                currentPage + 1, totalPages, ids.size()));
        }

        revalidate();
        repaint();
    }

    private JComponent buildNoneCell(boolean selected)
    {
        JLabel cell = new JLabel("None", SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setForeground(Color.LIGHT_GRAY);
        cell.setPreferredSize(new Dimension(0, 28));
        cell.setToolTipText("No icon");
        styleCell(cell, selected);
        cell.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e)
            {
                onSelect.accept(null);
                dispose();
            }
            @Override public void mouseEntered(MouseEvent e) { if (!selected) hover(cell, true); }
            @Override public void mouseExited(MouseEvent e) { if (!selected) hover(cell, false); }
        });
        return cell;
    }

    private JLabel buildIconCell()
    {
        JLabel cell = new JLabel("", SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setPreferredSize(new Dimension(28, 28));
        styleCell(cell, false);
        return cell;
    }

    private static void styleCell(Component c, boolean selected)
    {
        c.setBackground(selected ? ColorScheme.BRAND_ORANGE.darker() : ColorScheme.DARKER_GRAY_COLOR);
        if (c instanceof JComponent)
        {
            ((JComponent) c).setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected
                    ? ColorScheme.BRAND_ORANGE
                    : ColorScheme.DARK_GRAY_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        }
        c.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    private static void hover(JComponent c, boolean hovering)
    {
        c.setBackground(hovering
            ? ColorScheme.DARKER_GRAY_HOVER_COLOR
            : ColorScheme.DARKER_GRAY_COLOR);
    }

    private void bindEscapeToClose(JComponent root)
    {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        root.getActionMap().put("close", new javax.swing.AbstractAction()
        {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });
    }

    private static List<CategoryView> buildCategoryViews(IconCatalog catalog)
    {
        List<CategoryView> out = new ArrayList<>();
        if (catalog != null)
        {
            for (IconCatalog.Category c : catalog.getCategories())
            {
                if (c.getSpriteIds().isEmpty()) continue;
                out.add(new CategoryView(
                    c.getName() + " (" + c.getSpriteIds().size() + ")",
                    c.getSpriteIds()));
            }
        }
        if (out.isEmpty())
        {
            // Catalog missing or empty; preserve old behavior with one big "All" page.
            out.add(new CategoryView("All (" + LegacyAllIds.IDS.size() + ")", LegacyAllIds.IDS));
        }
        return out;
    }

    private static final class CategoryView
    {
        final String label;
        final List<Integer> ids;
        CategoryView(String label, List<Integer> ids) { this.label = label; this.ids = ids; }
    }

    /** Reflection over SpriteID, sorted ascending. Built lazily only if the JSON catalog
     *  can't be loaded. */
    private static final class LegacyAllIds
    {
        static final List<Integer> IDS = load();

        private static List<Integer> load()
        {
            List<Integer> ids = new ArrayList<>();
            try
            {
                for (Field f : SpriteID.class.getDeclaredFields())
                {
                    int mods = f.getModifiers();
                    if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods)
                        && f.getType() == int.class)
                    {
                        try { ids.add(f.getInt(null)); }
                        catch (IllegalAccessException ignored) {}
                    }
                }
            }
            catch (Throwable t) { ids.clear(); }
            if (ids.isEmpty())
            {
                ids.add(0); ids.add(1); ids.add(2);
            }
            Collections.sort(ids);
            return Collections.unmodifiableList(ids);
        }
    }
}
