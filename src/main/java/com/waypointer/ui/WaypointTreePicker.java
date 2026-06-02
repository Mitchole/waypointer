package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Renders a {@link WaypointPickerModel} as a collapsible, dark-themed checkbox tree, reusing the
 * panel's section idiom instead of a {@code JTree} (which would fight the Metal LAF described in
 * CLAUDE.md). Category rows carry a tri-state box, an expand caret, the name, and a count;
 * waypoint leaves carry an indented box and a name. Toggling any box updates the model and fires
 * {@code onChange} so the host dialog can re-gate its confirm buttons.
 */
final class WaypointTreePicker extends JPanel
{
    private final WaypointPickerModel model;
    private final Runnable onChange;
    private final List<CategoryView> categoryViews = new ArrayList<>();

    WaypointTreePicker(WaypointPickerModel model, Runnable onChange)
    {
        this.model = model;
        this.onChange = onChange == null ? () -> {} : onChange;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        for (Category c : model.getOrderedCategories())
        {
            CategoryView cv = new CategoryView(c);
            categoryViews.add(cv);
            add(cv);
        }
        add(Box.createVerticalGlue());
    }

    /** Re-reads every box from the model. Called by the host dialog after Select all / none. */
    void refreshAll()
    {
        for (CategoryView cv : categoryViews) cv.refresh();
    }

    private void fireChanged()
    {
        onChange.run();
    }

    private final class CategoryView extends JPanel
    {
        private final Category category;
        private final TriStateBox headerBox = new TriStateBox();
        private final JPanel body = new JPanel();
        private final List<LeafView> leaves = new ArrayList<>();
        private final JLabel caret = new JLabel("▶"); // black right-pointing triangle
        private boolean collapsed = true;

        CategoryView(Category category)
        {
            this.category = category;
            setLayout(new BorderLayout());
            setBackground(ColorScheme.DARK_GRAY_COLOR);
            setAlignmentX(LEFT_ALIGNMENT);

            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            header.setAlignmentX(LEFT_ALIGNMENT);

            headerBox.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e)
                {
                    boolean check = model.categoryState(category.getId())
                        != WaypointPickerModel.Tri.CHECKED;
                    model.setCategoryChecked(category.getId(), check);
                    refresh();
                    fireChanged();
                }
            });

            caret.setForeground(Color.LIGHT_GRAY);
            caret.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter toggle = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { toggleCollapsed(); }
            };
            caret.addMouseListener(toggle);

            JLabel name = new JLabel(category.getName());
            name.setForeground(Color.WHITE);
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            name.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            name.addMouseListener(toggle);

            JLabel count = new JLabel("(" + model.waypointsOf(category.getId()).size() + ")");
            count.setForeground(Color.LIGHT_GRAY);
            count.setFont(FontManager.getRunescapeSmallFont());

            header.add(headerBox);
            header.add(caret);
            header.add(name);
            header.add(count);
            add(header, BorderLayout.NORTH);

            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(ColorScheme.DARK_GRAY_COLOR);
            body.setAlignmentX(LEFT_ALIGNMENT);
            for (Waypoint w : model.waypointsOf(category.getId()))
            {
                LeafView lv = new LeafView(w, this);
                leaves.add(lv);
                body.add(lv);
            }
            body.setVisible(false);
            add(body, BorderLayout.CENTER);

            refresh();
        }

        private void toggleCollapsed()
        {
            collapsed = !collapsed;
            caret.setText(collapsed ? "▶" : "▼"); // right / down triangle
            body.setVisible(!collapsed);
            revalidate();
            repaint();
        }

        void refresh()
        {
            switch (model.categoryState(category.getId()))
            {
                case CHECKED:  headerBox.setState(TriStateBox.State.CHECKED); break;
                case PARTIAL:  headerBox.setState(TriStateBox.State.PARTIAL); break;
                default:       headerBox.setState(TriStateBox.State.UNCHECKED); break;
            }
            for (LeafView lv : leaves) lv.refresh();
        }

        @Override public Dimension getMaximumSize()
        {
            return Styles.capHeight(this);
        }
    }

    private final class LeafView extends JPanel
    {
        private final Waypoint waypoint;
        private final TriStateBox box = new TriStateBox();
        private final CategoryView owner;

        LeafView(Waypoint waypoint, CategoryView owner)
        {
            this.waypoint = waypoint;
            this.owner = owner;
            setLayout(new FlowLayout(FlowLayout.LEFT, 4, 1));
            setBackground(ColorScheme.DARK_GRAY_COLOR);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            MouseAdapter toggle = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { toggleChecked(); }
            };
            box.addMouseListener(toggle);
            addMouseListener(toggle);

            JLabel name = new JLabel(waypoint.getName());
            name.setForeground(Color.WHITE);
            name.addMouseListener(toggle);
            add(box);
            add(name);
        }

        private void toggleChecked()
        {
            model.setWaypointChecked(waypoint.getId(), !model.isWaypointChecked(waypoint.getId()));
            owner.refresh();
            fireChanged();
        }

        void refresh()
        {
            box.setState(model.isWaypointChecked(waypoint.getId())
                ? TriStateBox.State.CHECKED : TriStateBox.State.UNCHECKED);
        }

        @Override public Dimension getMaximumSize()
        {
            return Styles.capHeight(this);
        }
    }

}
