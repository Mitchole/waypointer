package com.waypointer.ui;

import com.waypointer.model.route.Route;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** One route row: name + subtitle, with Play / Edit / overflow actions. */
final class RouteRow extends JPanel
{
    RouteRow(Route route, Runnable onPlay, Runnable onEdit, Runnable onOverflow)
    {
        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel text = new JPanel(new BorderLayout());
        text.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JLabel name = new JLabel(route.getName());
        name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        name.setFont(FontManager.getRunescapeBoldFont());
        JLabel sub = new JLabel(subtitle(route));
        sub.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        sub.setFont(FontManager.getRunescapeSmallFont());
        text.add(name, BorderLayout.NORTH);
        text.add(sub, BorderLayout.SOUTH);
        add(text, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JButton play = new JButton("▶");
        JButton edit = new JButton("✎");
        JButton more = new JButton("⋮");
        Styles.compactSecondaryButton(play);
        Styles.compactSecondaryButton(edit);
        Styles.compactSecondaryButton(more);
        play.getAccessibleContext().setAccessibleName("Play route " + route.getName());
        edit.getAccessibleContext().setAccessibleName("Edit route " + route.getName());
        more.getAccessibleContext().setAccessibleName("More actions for " + route.getName());
        play.addActionListener(e -> onPlay.run());
        edit.addActionListener(e -> onEdit.run());
        more.addActionListener(e -> onOverflow.run());
        actions.add(play);
        actions.add(edit);
        actions.add(more);
        add(actions, BorderLayout.EAST);
    }

    static String subtitle(Route r)
    {
        int n = r.getSteps().size();
        String s = n + (n == 1 ? " step" : " steps");
        return r.isRepeating() ? s + "  -  repeating" : s;
    }

    @Override
    public Dimension getMaximumSize() { return Styles.capHeight(this); }
}
