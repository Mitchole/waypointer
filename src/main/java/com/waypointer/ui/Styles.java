package com.waypointer.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;

// Styling helpers for panel + dialogs. Vanilla Swing components default to system LAF which
// clashes with RuneLite's dark theme; each entry applies the RuneLite palette while letting
// components inherit the system LAF font.
final class Styles
{
    private Styles() {}

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
