package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

// Dev-tab root. Sub-tab strip plus CardLayout body swapping landmark/preset editors.
@Singleton
public class DevPanel
{
    private static final String CARD_LANDMARKS = "landmarks";
    private static final String CARD_PRESETS = "presets";

    private final JPanel root = new JPanel(new BorderLayout());
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final DevSubTabStrip subTabs;
    private final LandmarkEditorPanel landmarkEditor;
    private final PresetEditorPanel presetEditor;

    @Inject
    public DevPanel(LandmarkEditorPanel landmarkEditor, PresetEditorPanel presetEditor)
    {
        this.landmarkEditor = landmarkEditor;
        this.presetEditor = presetEditor;
        this.subTabs = new DevSubTabStrip(this::onSubTabSelected);

        root.setBackground(ColorScheme.DARK_GRAY_COLOR);
        root.add(subTabs, BorderLayout.NORTH);

        cards.setBackground(ColorScheme.DARK_GRAY_COLOR);
        cards.add(landmarkEditor, CARD_LANDMARKS);
        cards.add(presetEditor, CARD_PRESETS);
        root.add(cards, BorderLayout.CENTER);
    }

    public JPanel getRoot() { return root; }

    public void dispose()
    {
        landmarkEditor.dispose();
        presetEditor.dispose();
    }

    private void onSubTabSelected(DevSubTabStrip.SubTab t)
    {
        subTabs.setActive(t);
        cardLayout.show(cards, t == DevSubTabStrip.SubTab.LANDMARKS ? CARD_LANDMARKS : CARD_PRESETS);
    }
}
