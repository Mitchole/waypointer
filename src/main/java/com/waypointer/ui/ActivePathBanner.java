package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.Navigator;
import com.waypointer.util.Text;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The trip card at the top of the tab host. Shows where the active path leads plus a Stop
 * button, and hides itself when nothing is being pathed to. Pure projection of
 * {@link Navigator} state and the {@code showPathingBanner} config toggle.
 */
final class ActivePathBanner extends JPanel
{
    private final Navigator navigator;
    private final WaypointerConfig config;
    private final JLabel label = new JLabel();
    private final JButton stop = Styles.roundButton("", true);

    ActivePathBanner(Navigator navigator, WaypointerConfig config)
    {
        this.navigator = navigator;
        this.config = config;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(Styles.CARD_GAP, Styles.SIDE_PAD, 0, Styles.SIDE_PAD));
        setAlignmentX(LEFT_ALIGNMENT);

        CardPanel card = new CardPanel(new BorderLayout(6, 0));
        card.setBackground(Styles.ACTIVE_TINT);
        card.setOutline(ColorScheme.BRAND_ORANGE);
        card.setBorder(BorderFactory.createEmptyBorder(5, 10, 6, 8));

        JLabel caption = new JLabel("HEADING TO");
        caption.setFont(FontManager.getRunescapeSmallFont());
        caption.setForeground(ColorScheme.BRAND_ORANGE);
        label.setForeground(Color.WHITE);
        label.setFont(FontManager.getRunescapeBoldFont());
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(caption, BorderLayout.NORTH);
        text.add(label, BorderLayout.CENTER);
        card.add(text, BorderLayout.CENTER);

        stop.setIcon(Glyphs.of(Glyphs.Shape.STOP, Color.BLACK));
        stop.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        stop.getAccessibleContext().setAccessibleName("Stop pathing");
        HoverHint.shared().attach(stop, () -> "Stop");
        stop.addActionListener(e -> navigator.stop());
        JPanel east = new JPanel(new java.awt.GridBagLayout());
        east.setOpaque(false);
        east.add(stop);
        card.add(east, BorderLayout.EAST);
        add(card, BorderLayout.CENTER);

        setVisible(false);
    }

    /** Pull the latest target/name from the navigator, refresh the label, toggle visibility. */
    void refresh()
    {
        int target = navigator.activeTarget();
        boolean active = target != WorldPointPacker.UNDEFINED;
        boolean enabled = config.showPathingBanner();
        boolean visible = active && enabled;
        if (visible)
        {
            label.setText("<html>" + Text.escapeHtml(navigator.activeName()) + "</html>");
        }
        setVisible(visible);
    }

    // Test seam: tests read the rendered label text without poking at private fields.
    String getLabelText()
    {
        return label.getText();
    }

    // Test seam: programmatically activate the Stop button so tests can verify clearPath().
    void clickStopForTest()
    {
        stop.doClick();
    }

    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }
}
