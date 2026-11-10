package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ScrollBarUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.laf.RuneLiteScrollBarUI;

// Styling helpers for panel + dialogs. Vanilla Swing components default to system LAF which
// clashes with RuneLite's dark theme; each entry applies the RuneLite palette while letting
// components inherit the system LAF font.
final class Styles
{
    private Styles() {}

    private static final int SCROLLBAR_PIN_WIDTH = 7;
    private static final String CP_SCROLLBAR_WIDTH = "JScrollBar.width";
    private static final String CP_SCROLLBAR_BUTTONS = "JScrollBar.showButtons";

    // Destructive-action red used on delete row buttons and the delete link. ERROR_RED is the
    // slightly lighter tint for inline form-validation messages.
    static final Color DELETE_RED = new Color(220, 80, 80);
    static final Color ERROR_RED = new Color(220, 90, 90);

    // Secondary text greys used across empty-states, footer, and no-match copy. MUTED is the
    // primary secondary-text colour; FAINT is the dimmer tier (e.g. the footer tip). Each has a
    // hex twin for the HTML <span style='color:...'> sites that can't take a java.awt.Color.
    static final Color  MUTED_TEXT = new Color(0x9b, 0x9b, 0x9b);
    static final Color  FAINT_TEXT = new Color(0x6e, 0x6e, 0x6e);
    static final String MUTED_HEX  = "#9b9b9b";
    static final String FAINT_HEX  = "#6e6e6e";

    // The card look. Cards are DARKER_GRAY blocks with rounded corners on the DARK_GRAY panel;
    // RAISED is for controls sitting on a card, ROW_HOVER and ACTIVE_TINT for row states.
    static final Color CARD_BG = ColorScheme.DARKER_GRAY_COLOR;
    static final Color RAISED = new Color(52, 52, 52);
    static final Color ROW_HOVER = new Color(58, 58, 58);
    static final Color ROW_LINE = new Color(56, 56, 56);
    static final Color ACTIVE_TINT = new Color(70, 50, 18);
    static final int CARD_ARC = 12;
    static final int CARD_GAP = 6;
    static final int SIDE_PAD = 8;

    // Fixed vertical space between cards. Left-aligned, unlike a Box strut, so it does not pull
    // a BoxLayout column off its left edge.
    static JPanel gap(int height)
    {
        JPanel p = new JPanel();
        p.setOpaque(false);
        Dimension d = new Dimension(0, height);
        p.setPreferredSize(d);
        p.setMinimumSize(d);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    // Small capital label above a group of cards ("NEAREST", "PINNED").
    static JLabel caption(String text)
    {
        JLabel l = new JLabel(text.toUpperCase(java.util.Locale.ROOT));
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setForeground(FAINT_TEXT);
        l.setBorder(BorderFactory.createEmptyBorder(2, 2, 3, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // A count in a rounded pill, as shown beside a card title.
    static JLabel pill(String text)
    {
        JLabel l = new JLabel(text)
        {
            @Override protected void paintComponent(java.awt.Graphics g)
            {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RAISED);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setForeground(MUTED_TEXT);
        l.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        return l;
    }

    // Client property: corner diameter for a rounded button (default 8). The Mark button sets
    // it to its own height to become a pill.
    static final String CP_ARC = "waypointer.arc";

    // Paints a button as a rounded block of its background colour. Set on the button directly,
    // so it does not depend on which look and feel was live when the panel was built.
    private static final class RoundButtonUI extends javax.swing.plaf.basic.BasicButtonUI
    {
        @Override public void paint(java.awt.Graphics g, javax.swing.JComponent c)
        {
            javax.swing.AbstractButton b = (javax.swing.AbstractButton) c;
            Color fill = b.getBackground();
            if (!b.isEnabled()) fill = blend(fill, ColorScheme.DARK_GRAY_COLOR);
            else if (b.getModel().isPressed()) fill = fill.darker();
            else if (b.getModel().isRollover()) fill = fill.brighter();
            Object arc = b.getClientProperty(CP_ARC);
            int a = arc instanceof Integer ? (Integer) arc : 8;
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), a, a);
            g2.dispose();
            super.paint(g, c);
        }

        private static Color blend(Color a, Color b)
        {
            return new Color((a.getRed() + b.getRed()) / 2, (a.getGreen() + b.getGreen()) / 2,
                (a.getBlue() + b.getBlue()) / 2);
        }
    }

    private static void round(JButton b, Color bg, Color fg, int padV, int padH)
    {
        b.setUI(new RoundButtonUI());
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setRolloverEnabled(true);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setBorder(BorderFactory.createEmptyBorder(padV, padH, padV, padH));
    }

    // Rounded button for use on or between cards.
    static JButton roundButton(String text, boolean primary)
    {
        JButton b = new JButton(text);
        round(b, primary ? ColorScheme.BRAND_ORANGE : RAISED, primary ? Color.BLACK : Color.WHITE, 4, 10);
        b.setFont(primary ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeFont());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Icon-only button with no chrome, for the controls at the end of a row.
    static JButton iconButton(javax.swing.Icon icon, String accessibleName)
    {
        JButton b = new JButton(icon);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.getAccessibleContext().setAccessibleName(accessibleName);
        return b;
    }

    static void secondaryButton(JButton b)
    {
        round(b, RAISED, Color.WHITE, 6, 12);
    }

    static void compactSecondaryButton(JButton b)
    {
        round(b, RAISED, Color.WHITE, 2, 6);
    }

    // Borderless glyph button for row actions: grey at rest, white under the pointer. Keeps
    // rows quiet so the one orange control on screen is the thing that is active.
    static void glyphButton(JButton b)
    {
        b.setForeground(MUTED_TEXT);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setMargin(new java.awt.Insets(0, 0, 0, 0));
        b.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e) { b.setForeground(MUTED_TEXT); }
        });
    }

    // Small text button for inline row actions (dev editors). Same dark chrome as
    // compactSecondaryButton with a caller-chosen foreground so actions can be colour-coded.
    static JButton compactActionButton(String text, Color fg, Runnable onClick)
    {
        JButton b = new JButton(text);
        compactSecondaryButton(b);
        b.setForeground(fg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> onClick.run());
        return b;
    }

    // Clickable text styled as an HTML link: caller-chosen colour, hand cursor, click handler.
    // Used for the inline edit panel's footer actions.
    static JLabel link(String text, Color color, Runnable onClick)
    {
        JLabel l = new JLabel("<html><a href=''>" + text + "</a></html>");
        l.setForeground(color);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
        });
        return l;
    }

    static void primaryButton(JButton b)
    {
        round(b, ColorScheme.BRAND_ORANGE, Color.BLACK, 6, 12);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
    }

    // Tightens a styled button's padding for the panel header.
    static void slim(JButton b)
    {
        b.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    }

    // Paints a rounded card inside `c`, inset from its edges, for the forms that are panels in
    // their own right. The caller keeps `c` non-opaque and pads its border by the same insets.
    static void paintInsetCard(java.awt.Graphics g, Component c, int left, int top, int right, int bottom,
        Color outline)
    {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        int w = c.getWidth() - left - right;
        int h = c.getHeight() - top - bottom;
        g2.setColor(CARD_BG);
        g2.fillRoundRect(left, top, w, h, CARD_ARC, CARD_ARC);
        if (outline != null)
        {
            g2.setColor(outline);
            g2.drawRoundRect(left, top, w - 1, h - 1, CARD_ARC, CARD_ARC);
        }
        g2.dispose();
    }

    static final Color FIELD_LINE = new Color(80, 80, 80);

    static void textField(JTextField f)
    {
        f.setBackground(ColorScheme.DARK_GRAY_COLOR);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_LINE, 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    }

    static void textArea(JTextArea a)
    {
        a.setBackground(ColorScheme.DARK_GRAY_COLOR);
        a.setForeground(Color.WHITE);
        a.setCaretColor(Color.WHITE);
        a.setBorder(BorderFactory.createLineBorder(FIELD_LINE, 1));
    }

    static void combo(JComboBox<?> c)
    {
        c.setBackground(ColorScheme.DARK_GRAY_COLOR);
        c.setForeground(Color.WHITE);
    }

    static void checkBox(JCheckBox c)
    {
        c.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        c.setForeground(Color.WHITE);
        c.setFocusPainted(false);
        c.setFont(FontManager.getRunescapeSmallFont());
    }

    static JLabel fieldLabel(String text)
    {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }

    // A hidden, blank, red label for inline form-validation messages. Callers set the text and
    // flip it visible on a failure, and clear it on success. Used by InlineInputForm and the
    // import dialog so the validation style is defined once.
    static JLabel errorLabel()
    {
        JLabel l = new JLabel(" ");
        l.setForeground(ERROR_RED);
        l.setVisible(false);
        return l;
    }

    // Width-unbounded, height-pinned maximum size. Return this from a component's
    // getMaximumSize() override so BoxLayout(Y_AXIS) stacks it at its preferred height
    // instead of stretching it into leftover column space.
    static Dimension capHeight(Component c)
    {
        return new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height);
    }

    /**
     * A {@link JPanel} whose maximum height is pinned to its preferred height, so a
     * {@code BoxLayout(Y_AXIS)} parent does not stretch it vertically. Used for banners,
     * the footer, and the empty-state block.
     */
    public static JPanel cappedHeightPanel(LayoutManager lm)
    {
        return new JPanel(lm)
        {
            @Override public Dimension getMaximumSize()
            {
                return Styles.capHeight(this);
            }
        };
    }

    static DocumentListener documentListener(Runnable onChange)
    {
        return new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        };
    }

    // Holds a scrolling list at the top of its viewport and reports the sidebar width, so one
    // long label cannot widen the list and push the row buttons past the right edge. The
    // viewport stretches the holder to the real width.
    static JPanel sidebarWidthHolder(Component list)
    {
        JPanel holder = new JPanel(new BorderLayout())
        {
            @Override public Dimension getPreferredSize()
            {
                return new Dimension(net.runelite.client.ui.PluginPanel.PANEL_WIDTH,
                    super.getPreferredSize().height);
            }
        };
        holder.setBackground(ColorScheme.DARK_GRAY_COLOR);
        holder.add(list, BorderLayout.NORTH);
        return holder;
    }

    // Builds a JScrollPane themed to match the dark plugin panel: no border, no horizontal bar,
    // and a thin (7-px) vertical bar with arrow buttons hidden. These panels are constructed
    // during loadCorePlugins() - before ClientUI.init() installs RuneLiteLAF - so the scrollbar's
    // UI delegate would otherwise inherit Metal defaults; the caller stores the bar and passes it
    // to reapplyScrollbarPin(...) from startUp() once the LAF is live.
    static JScrollPane pinnedScrollPane(Component view)
    {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        JScrollBar vBar = scroll.getVerticalScrollBar();
        vBar.putClientProperty(CP_SCROLLBAR_WIDTH, SCROLLBAR_PIN_WIDTH);
        vBar.putClientProperty(CP_SCROLLBAR_BUTTONS, Boolean.FALSE);
        vBar.setPreferredSize(new Dimension(SCROLLBAR_PIN_WIDTH, 0));
        vBar.setUI((ScrollBarUI) RuneLiteScrollBarUI.createUI(vBar));
        return scroll;
    }

    // Re-derives and re-pins a vertical scrollbar's UI delegate after RuneLiteLAF is installed.
    // updateUI() resets the preferred size and client properties, so they are re-applied here.
    // Null-safe so callers can invoke it unconditionally from startUp().
    static void reapplyScrollbarPin(JScrollBar bar)
    {
        if (bar == null) return;
        bar.updateUI();
        bar.setPreferredSize(new Dimension(SCROLLBAR_PIN_WIDTH, 0));
        bar.putClientProperty(CP_SCROLLBAR_WIDTH, SCROLLBAR_PIN_WIDTH);
        bar.putClientProperty(CP_SCROLLBAR_BUTTONS, Boolean.FALSE);
    }

    // Populates `target` as a one-line editable metadata row: name on the WEST, a coordinate/
    // detail string in the CENTER, and Go / Edit / Delete action buttons on the EAST. Used by
    // LandmarkRow and PresetWaypointRow, which keep their typed constructors and delegate here.
    static void editableMetaRow(JPanel target, String name, String detail,
        Runnable onGo, Runnable onEdit, Runnable onDelete)
    {
        target.setLayout(new BorderLayout(6, 0));
        target.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        target.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());

        JLabel detailLabel = new JLabel(detail);
        detailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        detailLabel.setFont(FontManager.getRunescapeSmallFont());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(compactActionButton("Go", ColorScheme.BRAND_ORANGE, onGo));
        right.add(compactActionButton("Edit", Color.WHITE, onEdit));
        right.add(compactActionButton("Delete", DELETE_RED, onDelete));

        target.add(nameLabel, BorderLayout.WEST);
        target.add(detailLabel, BorderLayout.CENTER);
        target.add(right, BorderLayout.EAST);
    }

}
