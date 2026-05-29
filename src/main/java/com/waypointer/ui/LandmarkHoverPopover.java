package com.waypointer.ui;

import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Immediate-hover popover used by {@link NearestLandmarkBar}. Owns one borderless
 * {@link JWindow} with a single styled {@link JLabel}. The window is reused across
 * every attached target — only one popover is ever visible at a time.
 *
 * <p>Headless safety: tests run without a display, so visibility is tracked in
 * {@code visibleIntent} and {@link JWindow#setVisible} calls are skipped in headless
 * environments. Production code reads visibility through the window; tests read it
 * through {@link #visibleIntentForTest()}.
 */
final class LandmarkHoverPopover
{
	private final JWindow window;
	private final JLabel label;
	private boolean visibleIntent;
	private boolean disposed;

	LandmarkHoverPopover()
	{
		this.window = new JWindow();
		this.window.setFocusableWindowState(false);
		this.label = new JLabel("");
		this.label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		this.label.setFont(FontManager.getRunescapeSmallFont());
		this.label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		this.label.setOpaque(true);
		this.label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		this.window.getContentPane().add(label);
		this.window.pack();
	}

	boolean visibleIntentForTest() { return visibleIntent; }
	String labelTextForTest() { return label.getText(); }

	private static boolean displayAvailable()
	{
		return !GraphicsEnvironment.isHeadless();
	}
}
