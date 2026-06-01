package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import net.runelite.client.ui.ColorScheme;

/**
 * Standalone resizable window hosting the dev-mode editors. The {@link DevPanel} button in the
 * sidebar opens it. The frame is built lazily on first open and hidden (not destroyed) on close,
 * so the editor panels keep their data and event subscriptions between openings. The frame is
 * only torn down on plugin shutdown via {@link #dispose()}.
 */
@Singleton
public class DevToolsWindow
{
    private static final String CARD_LANDMARKS = "landmarks";
    private static final String CARD_PRESETS = "presets";

    private final LandmarkEditorPanel landmarkEditor;
    private final PresetEditorPanel presetEditor;

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel cards;
    private DevSubTabStrip subTabs;

    @Inject
    public DevToolsWindow(LandmarkEditorPanel landmarkEditor, PresetEditorPanel presetEditor)
    {
        this.landmarkEditor = landmarkEditor;
        this.presetEditor = presetEditor;
    }

    /** Shows the window, building it on first use. {@code near} anchors initial placement. */
    public void open(Component near)
    {
        if (frame == null) build();
        if (!frame.isVisible())
        {
            frame.setLocationRelativeTo(near == null ? null : SwingUtilities.getWindowAncestor(near));
        }
        frame.setVisible(true);
        frame.setState(Frame.NORMAL);
        frame.toFront();
        frame.requestFocus();
    }

    private void build()
    {
        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setBackground(ColorScheme.DARK_GRAY_COLOR);
        cards.add(landmarkEditor, CARD_LANDMARKS);
        cards.add(presetEditor, CARD_PRESETS);

        subTabs = new DevSubTabStrip(this::showCard);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.add(subTabs, BorderLayout.NORTH);
        content.add(cards, BorderLayout.CENTER);

        frame = new JFrame("Waypointer - Dev Tools");
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        frame.setContentPane(content);
        frame.setMinimumSize(new Dimension(360, 420));
        frame.setSize(new Dimension(440, 680));
        showCard(DevSubTabStrip.SubTab.LANDMARKS);
    }

    private void showCard(DevSubTabStrip.SubTab t)
    {
        subTabs.setActive(t);
        cardLayout.show(cards, t == DevSubTabStrip.SubTab.LANDMARKS ? CARD_LANDMARKS : CARD_PRESETS);
    }

    public void dispose()
    {
        landmarkEditor.dispose();
        presetEditor.dispose();
        if (frame != null)
        {
            frame.dispose();
            frame = null;
        }
    }
}
