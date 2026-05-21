package com.waypointer.ui;

import com.waypointer.codec.WaypointShareCodec;
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
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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

    private final JTextField nameField;
    private final JTextArea notesArea;
    private boolean nameDirty = false;
    private boolean notesDirty = false;

    public InlineEditPanel(Waypoint w, WaypointStore store, WaypointCapture capture,
        WaypointShareCodec codec, SpriteManager spriteManager, IconCatalog iconCatalog)
    {
        this.store = store;
        this.waypointId = w.getId();
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 24, 4, 4));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 2, 2, 2);
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
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
        JComboBox<CategoryItem> cats = new JComboBox<>();
        cats.setBackground(ColorScheme.DARK_GRAY_COLOR);
        cats.setForeground(Color.WHITE);
        for (Category c : store.getCategoriesOrdered()) cats.addItem(new CategoryItem(c));
        for (int i = 0; i < cats.getItemCount(); i++)
            if (cats.getItemAt(i).id().equals(w.getCategoryId())) cats.setSelectedIndex(i);
        cats.addActionListener(e -> {
            CategoryItem sel = (CategoryItem) cats.getSelectedItem();
            if (sel != null) store.moveWaypointToCategory(waypointId, sel.id());
        });
        g.gridx = 1; g.weightx = 1; add(cats, g);

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
        int p = w.getPackedWorldPoint();
        JLabel coords = new JLabel(String.format("(%d, %d, %d)",
            WorldPointPacker.getX(p), WorldPointPacker.getY(p), WorldPointPacker.getPlane(p)));
        coords.setForeground(Color.LIGHT_GRAY);
        g.gridx = 1; g.weightx = 1; add(coords, g);

        // Footer: thin divider followed by a single FlowLayout row of links separated by ·
        g.gridx = 0; g.gridy++; g.gridwidth = 2; g.weightx = 1;
        add(makeDivider(), g);

        g.gridx = 0; g.gridy++; g.gridwidth = 2; g.weightx = 1;
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        footer.setOpaque(false);
        JLabel recapture = makeLink("Recapture", () -> {
            // Flush any in-flight name/notes edits before swapping the tile under us.
            flushPending();
            capture.readCurrentLocation(packed -> {
                if (packed != WorldPointPacker.UNDEFINED)
                {
                    store.updateWaypointPoint(waypointId, packed);
                }
            });
        });
        JLabel setIcon = makeLink("Set icon", () -> {
            Waypoint cur = store.getWaypointById(waypointId);
            if (cur == null) return;
            Window owner = SwingUtilities.getWindowAncestor(this);
            new IconPickerDialog(owner, spriteManager, iconCatalog, cur.getIconId(), iconId -> {
                store.updateWaypointIcon(waypointId, iconId);
            }).setVisible(true);
        });
        JLabel copy = makeLink("Copy share code", () -> {
            // Flush pending edits so the share code reflects what the user just typed.
            flushPending();
            Waypoint cur = store.getWaypointById(waypointId);
            if (cur == null) return;
            Category cat = store.getCategoryById(cur.getCategoryId());
            if (cat == null) return;
            String code = codec.encodeSingle(cur, cat);
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(code), null);
            JOptionPane.showMessageDialog(this, "Share code copied to clipboard.",
                "Waypointer", JOptionPane.INFORMATION_MESSAGE);
        });
        footer.add(recapture);
        footer.add(makeSeparator());
        footer.add(setIcon);
        footer.add(makeSeparator());
        footer.add(copy);
        footer.add(makeSeparator());
        JLabel delete = makeLink("Delete", () -> {
            flushPending();
            WaypointerPanel.confirmAndDelete(this, store, store.getWaypointById(waypointId));
        });
        delete.setForeground(new Color(220, 80, 80));
        footer.add(delete);
        add(footer, g);
    }

    // Commits in-flight name/notes edits. Called on focus loss, on detach, and before any
    // action that reads the waypoint's persisted value (recapture, share-code copy).
    public void flushPending()
    {
        flushName();
        flushNotes();
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
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
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

    private static JLabel makeLink(String text, Runnable onClick)
    {
        JLabel l = new JLabel("<html><a href=''>" + text + "</a></html>");
        l.setForeground(ColorScheme.BRAND_ORANGE);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return l;
    }

    public static final class CategoryItem
    {
        private final Category c;
        public CategoryItem(Category c) { this.c = c; }
        public UUID id() { return c.getId(); }
        @Override public String toString() { return c.getName(); }
    }
}
