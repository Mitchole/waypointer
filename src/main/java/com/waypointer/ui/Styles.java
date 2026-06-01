package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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

    private static final String CLIENT_PROP_ACTIVE = "waypointer.playActive";
    private static final String CLIENT_PROP_HOVER_ATTACHED = "waypointer.playHoverAttached";

    private static final int SCROLLBAR_PIN_WIDTH = 7;
    private static final String CP_SCROLLBAR_WIDTH = "JScrollBar.width";
    private static final String CP_SCROLLBAR_BUTTONS = "JScrollBar.showButtons";

    // Destructive-action red used on delete row buttons and the delete link. ERROR_RED is the
    // slightly lighter tint for inline form-validation messages.
    static final Color DELETE_RED = new Color(220, 80, 80);
    static final Color ERROR_RED = new Color(220, 90, 90);

    static void secondaryButton(JButton b)
    {
        b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    static void compactSecondaryButton(JButton b)
    {
        b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
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
        b.setBackground(ColorScheme.BRAND_ORANGE);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        b.setFont(b.getFont().deriveFont(Font.BOLD));
    }

    // Per-row Play button. Dark by default, brand-orange on hover, locked orange
    // when active. Active flag is mirrored to a client property so the hover
    // adapter can check it without closure capture.
    static void playIconButton(JButton b, boolean active)
    {
        b.putClientProperty(CLIENT_PROP_ACTIVE, active);

        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        Dimension size = new Dimension(36, 32);
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(true);

        applyPlayIconResting(b, active);

        // Attach the hover adapter once. Re-invocations on the same button would
        // otherwise stack listeners.
        if (b.getClientProperty(CLIENT_PROP_HOVER_ATTACHED) == null)
        {
            b.addMouseListener(new MouseAdapter()
            {
                @Override public void mouseEntered(MouseEvent e)
                {
                    if (Boolean.TRUE.equals(b.getClientProperty(CLIENT_PROP_ACTIVE))) return;
                    applyPlayIconHover(b);
                }
                @Override public void mouseExited(MouseEvent e)
                {
                    if (Boolean.TRUE.equals(b.getClientProperty(CLIENT_PROP_ACTIVE))) return;
                    applyPlayIconResting(b, false);
                }
            });
            b.putClientProperty(CLIENT_PROP_HOVER_ATTACHED, Boolean.TRUE);
        }
    }

    private static void applyPlayIconResting(JButton b, boolean active)
    {
        if (active)
        {
            b.setBackground(ColorScheme.BRAND_ORANGE);
            b.setForeground(Color.BLACK);
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }
        else
        {
            b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            b.setForeground(ColorScheme.BRAND_ORANGE);
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }
    }

    private static void applyPlayIconHover(JButton b)
    {
        b.setBackground(ColorScheme.BRAND_ORANGE);
        b.setForeground(Color.BLACK);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    static void textField(JTextField f)
    {
        f.setBackground(ColorScheme.DARK_GRAY_COLOR);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    }

    static void textArea(JTextArea a)
    {
        a.setBackground(ColorScheme.DARK_GRAY_COLOR);
        a.setForeground(Color.WHITE);
        a.setCaretColor(Color.WHITE);
        a.setBorder(BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1));
    }

    static void combo(JComboBox<?> c)
    {
        c.setBackground(ColorScheme.DARK_GRAY_COLOR);
        c.setForeground(Color.WHITE);
    }

    static JLabel fieldLabel(String text)
    {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        return l;
    }

    // Width-unbounded, height-pinned maximum size. Return this from a component's
    // getMaximumSize() override so BoxLayout(Y_AXIS) stacks it at its preferred height
    // instead of stretching it into leftover column space.
    static Dimension capHeight(Component c)
    {
        return new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height);
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

    // Returns s with characters that are illegal in filenames on Windows / macOS removed
    // (/ \ : * ? " < > |), plus leading/trailing dots and whitespace trimmed. Falls back to
    // "untitled" when the input is null, empty, or reduces to nothing.
    static String sanitizeFilenameSegment(String s)
    {
        if (s == null) return "untitled";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            switch (ch)
            {
                case '/': case '\\': case ':': case '*': case '?':
                case '"': case '<': case '>': case '|':
                    continue;
                default:
                    b.append(ch);
            }
        }
        // Strip outer whitespace and dots, alternating until stable. A single pass misses
        // inputs like ". .name. ." where dots and spaces interleave at the boundary.
        String result = b.toString();
        String prev;
        do
        {
            prev = result;
            result = result.trim();
            int start = 0, end = result.length();
            while (start < end && result.charAt(start) == '.') start++;
            while (end > start && result.charAt(end - 1) == '.') end--;
            result = result.substring(start, end);
        } while (!result.equals(prev));
        return result.isEmpty() ? "untitled" : result;
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

    // Escapes s for embedding in HTML. Handles <, >, &, ", and '. Returns "" when s is null.
    static String escapeHtml(String s)
    {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            switch (ch)
            {
                case '<':  b.append("&lt;");   break;
                case '>':  b.append("&gt;");   break;
                case '&':  b.append("&amp;");  break;
                case '"':  b.append("&quot;"); break;
                case '\'': b.append("&#39;");  break;
                default:   b.append(ch);
            }
        }
        return b.toString();
    }
}
