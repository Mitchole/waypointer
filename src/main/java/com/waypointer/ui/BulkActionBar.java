package com.waypointer.ui;

import com.waypointer.model.Category;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Bottom action bar shown while the panel is in bulk select mode. Reads "N selected" on the left
 * and offers Move to / Delete / Export on the right. The three action controls are disabled while
 * the selection is empty. Stateless beyond the count + enabled flags; all behaviour is delegated
 * to the callbacks supplied by {@link WaypointerPanel}.
 */
final class BulkActionBar extends JPanel
{
    private final JLabel countLabel = new JLabel("0 selected");
    private final JButton moveBtn = new JButton("Move to ▾"); // down triangle
    private final JButton deleteBtn = new JButton("Delete");
    private final JButton exportBtn = new JButton("Export");

    BulkActionBar(Supplier<List<Category>> categorySupplier, Consumer<UUID> onMove,
        Runnable onDelete, Runnable onExport)
    {
        setLayout(new BorderLayout(4, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        countLabel.setForeground(Color.WHITE);
        countLabel.setFont(FontManager.getRunescapeSmallFont());
        add(countLabel, BorderLayout.WEST);

        Styles.secondaryButton(moveBtn);
        Styles.secondaryButton(deleteBtn);
        Styles.secondaryButton(exportBtn);

        moveBtn.addActionListener(e -> {
            JPopupMenu menu = new JPopupMenu();
            for (Category c : categorySupplier.get())
            {
                JMenuItem item = new JMenuItem(c.getName());
                UUID target = c.getId();
                item.addActionListener(ev -> onMove.accept(target));
                menu.add(item);
            }
            menu.show(moveBtn, 0, -menu.getPreferredSize().height);
        });
        deleteBtn.addActionListener(e -> onDelete.run());
        exportBtn.addActionListener(e -> onExport.run());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        buttons.add(moveBtn);
        buttons.add(deleteBtn);
        buttons.add(exportBtn);
        add(buttons, BorderLayout.EAST);

        setActionsEnabled(false);
    }

    void setCount(int n)
    {
        countLabel.setText(n + " selected");
    }

    void setActionsEnabled(boolean enabled)
    {
        moveBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
        exportBtn.setEnabled(enabled);
    }

    // Cap height to preferred so the SOUTH dock stays a thin fixed-height strip.
    @Override public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }
}
