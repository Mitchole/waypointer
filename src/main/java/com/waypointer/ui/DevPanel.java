package com.waypointer.ui;

import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

// Dev-tab content: a single button that opens the standalone dev-tools window. The editors
// themselves live in DevToolsWindow so they get a roomy resizable window instead of the sidebar.
@Singleton
public class DevPanel
{
    private final JPanel root = new JPanel(new BorderLayout());
    private final DevToolsWindow window;

    @Inject
    public DevPanel(DevToolsWindow window)
    {
        this.window = window;

        root.setBackground(ColorScheme.DARK_GRAY_COLOR);
        root.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JButton open = new JButton("Open dev tools");
        Styles.secondaryButton(open);
        open.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        open.addActionListener(e -> window.open(root));

        JLabel hint = new JLabel("<html><div style='color:" + Styles.MUTED_HEX + ";'>Opens a separate window to "
            + "edit and navigate landmark and preset data.</div></html>");
        hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        hint.setFont(FontManager.getRunescapeSmallFont());
        hint.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        inner.add(open);
        inner.add(hint);
        root.add(inner, BorderLayout.NORTH);
    }

    public JPanel getRoot() { return root; }

    public void dispose() { window.dispose(); }
}
