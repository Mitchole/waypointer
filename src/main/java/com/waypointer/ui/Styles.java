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
