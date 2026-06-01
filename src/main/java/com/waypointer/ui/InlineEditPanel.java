package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.IconCatalog;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointStore;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

// Form shown when a WaypointRow is expanded. Name and notes edits are deferred: the user
// types into local Swing fields, and on focus loss (or panel removal) the pending text is
// committed. This avoids the keystroke -> store-update -> panel-rebuild -> field-recreated
// -> cursor-lost loop the older synchronous variant had.
public class InlineEditPanel extends JPanel
{
    private final WaypointStore store;
    private final UUID waypointId;
    private final Toasts toasts;

    private final JTextField nameField;
    private final JTextArea notesArea;
    private final JComboBox<CategoryComboItem> categoryCombo;
    private boolean nameDirty = false;
    private boolean notesDirty = false;
    private boolean categoryDirty = false;

    public InlineEditPanel(Waypoint w, WaypointStore store, WaypointCapture capture,
        SpriteManager spriteManager, IconCatalog iconCatalog, Toasts toasts,
        Runnable onClose, Runnable onShowOnMap)
    {
        this.store = store;
        this.waypointId = w.getId();
        this.toasts = toasts;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 24, 4, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 2, 2, 2);

        // Top-right close affordance. Spans both columns so the X anchors to the panel edge.
        if (onClose != null)
        {
            g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.weightx = 1;
            g.anchor = GridBagConstraints.EAST; g.fill = GridBagConstraints.NONE;
            JLabel closeBtn = new JLabel("✕"); // U+2715 multiplication X
            closeBtn.setForeground(Color.LIGHT_GRAY);
            closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeBtn.setToolTipText("Close");
            closeBtn.getAccessibleContext().setAccessibleName("Close editor");
            closeBtn.addMouseListener(new MouseAdapter()
            {
                @Override public void mouseClicked(MouseEvent e)
                {
                    flushPending();
                    onClose.run();
                }
            });
            add(closeBtn, g);
            g.gridwidth = 1; g.anchor = GridBagConstraints.CENTER; g.fill = GridBagConstraints.HORIZONTAL;
        }

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        add(Styles.fieldLabel("Name"), g);
        nameField = new JTextField(w.getName());
        Styles.textField(nameField);
        nameField.getDocument().addDocumentListener(Styles.documentListener(() -> nameDirty = true));
        nameField.addFocusListener(new FocusAdapter()
        {
            @Override public void focusLost(FocusEvent e) { flushName(); }
        });
        g.gridx = 1; g.weightx = 1; add(nameField, g);

        g.gridx = 0; g.gridy++; g.weightx = 0; add(Styles.fieldLabel("Category"), g);
        categoryCombo = new JComboBox<>();
        Styles.combo(categoryCombo);
        for (Category c : store.getCategoriesOrdered()) categoryCombo.addItem(new CategoryComboItem(c));
        for (int i = 0; i < categoryCombo.getItemCount(); i++)
            if (categoryCombo.getItemAt(i).id().equals(w.getCategoryId())) categoryCombo.setSelectedIndex(i);
        // Deferred commit: track changes here, apply on focus loss / detach. An immediate
        // moveWaypointToCategory on every action would fire for any stray click or scroll-wheel
        // tick over the combo, with no way to undo without manually moving the row back.
        categoryCombo.addActionListener(e -> categoryDirty = true);
        categoryCombo.addFocusListener(new FocusAdapter()
        {
            @Override public void focusLost(FocusEvent e) { flushCategory(); }
        });
        g.gridx = 1; g.weightx = 1; add(categoryCombo, g);

        g.gridx = 0; g.gridy++; g.weightx = 0; add(Styles.fieldLabel("Notes"), g);
        notesArea = new JTextArea(w.getNotes(), 3, 12);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        Styles.textArea(notesArea);
        notesArea.getDocument().addDocumentListener(Styles.documentListener(() -> notesDirty = true));
        notesArea.addFocusListener(new FocusAdapter()
        {
            @Override public void focusLost(FocusEvent e) { flushNotes(); }
        });
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(null);
        notesScroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        // Pin the notes pane to ~3 visible rows. The JTextArea(rows=3) hint flows up through
        // getPreferredScrollableViewportSize, but inside the outer body's JScrollPane it ends
        // up collapsing to a single visible row; setting an explicit preferred + minimum
        // height keeps the field at its intended size regardless of the surrounding chain.
        int rowH = notesArea.getFontMetrics(notesArea.getFont()).getHeight();
        Dimension notesPref = new Dimension(0, rowH * 3 + 6);
        notesScroll.setPreferredSize(notesPref);
        notesScroll.setMinimumSize(notesPref);
        g.gridx = 1; g.weightx = 1; add(notesScroll, g);

        g.gridx = 0; g.gridy++; g.weightx = 0; add(Styles.fieldLabel("Tile"), g);
        JLabel showOnMap = Styles.link("Show on world map", ColorScheme.BRAND_ORANGE,
            onShowOnMap == null ? () -> {} : onShowOnMap);
        g.gridx = 1; g.weightx = 1; add(showOnMap, g);

        // Footer: thin divider followed by a single FlowLayout row of links separated by ·
        g.gridx = 0; g.gridy++; g.gridwidth = 2; g.weightx = 1;
        add(makeDivider(), g);

        g.gridx = 0; g.gridy++; g.gridwidth = 2; g.weightx = 1;
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        footer.setOpaque(false);
        JLabel recapture = Styles.link("Recapture", ColorScheme.BRAND_ORANGE, () -> {
            // Flush any in-flight name/notes edits before swapping the tile under us.
            flushPending();
            capture.readCurrentLocation(packed -> {
                if (packed == WorldPointPacker.UNDEFINED) return;
                Waypoint cur = store.getWaypointById(waypointId);
                if (cur == null) return;
                String name = cur.getName();
                store.updateWaypointPoint(waypointId, packed);
                toasts.show("Recaptured '" + name + "'", "Undo", store::undoLast);
            });
        });
        JLabel setIcon = Styles.link("Set icon", ColorScheme.BRAND_ORANGE, () -> {
            Waypoint cur = store.getWaypointById(waypointId);
            if (cur == null) return;
            Window owner = SwingUtilities.getWindowAncestor(this);
            new IconPickerDialog(owner, spriteManager, iconCatalog, cur.getIconId(), iconId -> {
                store.updateWaypointIcon(waypointId, iconId);
            }).setVisible(true);
        });
        footer.add(recapture);
        footer.add(makeSeparator());
        footer.add(setIcon);
        footer.add(makeSeparator());
        JLabel delete = Styles.link("Delete", Styles.DELETE_RED, () -> {
            flushPending();
            WaypointerPanel.softDeleteWithUndo(store, store.getWaypointById(waypointId), toasts);
        });
        footer.add(delete);
        add(footer, g);
    }

    // Commits in-flight edits. Called on focus loss, on detach, and before recapture so the
    // swap doesn't drop the user's pending text.
    public void flushPending()
    {
        flushName();
        flushNotes();
        flushCategory();
    }

    private void flushName()
    {
        if (!nameDirty) return;
        nameDirty = false;
        store.renameWaypoint(waypointId, nameField.getText());
    }

    private void flushNotes()
    {
        if (!notesDirty) return;
        notesDirty = false;
        store.updateWaypointNotes(waypointId, notesArea.getText());
    }

    private void flushCategory()
    {
        if (!categoryDirty) return;
        categoryDirty = false;
        CategoryComboItem sel = (CategoryComboItem) categoryCombo.getSelectedItem();
        if (sel == null) return;
        Waypoint cur = store.getWaypointById(waypointId);
        if (cur == null || sel.id().equals(cur.getCategoryId())) return;
        store.moveWaypointToCategory(waypointId, sel.id());
    }

    // Safety net for parent rebuilds that fire before focus loss.
    @Override
    public void removeNotify()
    {
        flushPending();
        super.removeNotify();
    }

    // Cap vertical extent at preferred height so BoxLayout(Y_AXIS) in the parent stacks tight
    // instead of stretching us into leftover space.
    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }

    private static JPanel makeDivider()
    {
        JPanel d = new JPanel();
        d.setBackground(ColorScheme.LIGHT_GRAY_COLOR);
        Dimension dim = new Dimension(0, 1);
        d.setPreferredSize(dim);
        d.setMinimumSize(dim);
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return d;
    }

    private static JLabel makeSeparator()
    {
        JLabel sep = new JLabel("·"); // middle dot
        sep.setForeground(Color.LIGHT_GRAY);
        return sep;
    }

}
