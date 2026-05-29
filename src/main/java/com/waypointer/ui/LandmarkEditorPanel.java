package com.waypointer.ui;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

@Singleton
public class LandmarkEditorPanel extends JPanel
{
	@Inject public LandmarkEditorPanel() { setBackground(ColorScheme.DARK_GRAY_COLOR); }
	public void dispose() {}
}
