package com.waypointer.ui;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

@Singleton
public class PresetEditorPanel extends JPanel
{
    @Inject public PresetEditorPanel() { setBackground(ColorScheme.DARK_GRAY_COLOR); }
    public void dispose() {}
}
