package com.waypointer.ui;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
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

	// Guard used by attach/positionAbove (added in later tasks): JWindow.setVisible and
	// Component.getLocationOnScreen both throw HeadlessException when no display exists.
	private static boolean displayAvailable()
	{
		return !GraphicsEnvironment.isHeadless();
	}

	/**
	 * Attach this popover to {@code target}. While the cursor is inside the target,
	 * the popover shows the value of {@code textSupplier.get()} four pixels above the
	 * target, horizontally centred. The supplier is re-read on every enter so the
	 * text follows late-arriving updates (e.g. a rebuild changing a button's meaning).
	 *
	 * <p>Attach is a no-op after {@link #dispose()}.
	 */
	void attach(JComponent target, Supplier<String> textSupplier)
	{
		target.addMouseListener(new MouseAdapter()
		{
			@Override public void mouseEntered(MouseEvent e)
			{
				if (disposed) return;
				String text = textSupplier.get();
				label.setText(text == null ? "" : text);
				window.pack();
				positionAbove(target);
				visibleIntent = true;
				if (displayAvailable()) window.setVisible(true);
			}

			@Override public void mouseExited(MouseEvent e)
			{
				visibleIntent = false;
				if (displayAvailable()) window.setVisible(false);
			}
		});
	}

	private void positionAbove(JComponent target)
	{
		if (!displayAvailable()) return;
		if (!target.isShowing()) return;
		Point onScreen = target.getLocationOnScreen();
		int gap = 4;
		int x = onScreen.x + (target.getWidth() - window.getWidth()) / 2;
		int y = onScreen.y - window.getHeight() - gap;
		window.setLocation(x, y);
	}
}
