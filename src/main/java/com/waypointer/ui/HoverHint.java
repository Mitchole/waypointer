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
 * Immediate-hover hint: a dark-themed borderless {@link JWindow} with a single styled
 * {@link JLabel} that appears the instant the cursor enters an attached component, four
 * pixels above it. The house style for affordance hints across the panel, replacing the
 * native (delayed, light-on-dark) Swing tooltip.
 *
 * <p>Most call sites use the process-shared instance via {@link #shared()} -- one reused
 * window for the whole plugin UI, never more than one visible at a time. The instance API
 * (constructor + {@link #attach} + {@link #dispose}) is retained for unit tests.
 *
 * <p>Headless safety: tests run without a display, so visibility is tracked in
 * {@code visibleIntent} and {@link JWindow#setVisible} calls are skipped in headless
 * environments. Production code reads visibility through the window; tests read it
 * through {@link #visibleIntentForTest()}.
 */
final class HoverHint
{
	private static HoverHint shared;

	/**
	 * Process-shared hint. Lazily created on first use and kept for the JVM session: a single
	 * invisible window is cheap and is reused across every panel rebuild and plugin
	 * enable/disable cycle, so nothing accumulates. {@code attach} only ever adds a listener to
	 * the (freshly built) target component, so re-enabling the plugin re-attaches new components
	 * to the same window without stacking.
	 * Do not call {@link #dispose()} on the shared instance -- {@code disposed} is sticky and would permanently silence all hints for the session.
	 */
	static HoverHint shared()
	{
		if (shared == null) shared = new HoverHint();
		return shared;
	}

	private final JWindow window;
	private final JLabel label;
	private boolean visibleIntent;
	private boolean disposed;

	HoverHint()
	{
		this.label = new JLabel("");
		this.label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		this.label.setFont(FontManager.getRunescapeSmallFont());
		this.label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		this.label.setOpaque(true);
		this.label.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		if (displayAvailable())
		{
			this.window = new JWindow();
			this.window.setFocusableWindowState(false);
			this.window.getContentPane().add(label);
			this.window.pack();
		}
		else
		{
			this.window = null;
		}
	}

	boolean visibleIntentForTest() { return visibleIntent; }
	String labelTextForTest() { return label.getText(); }

	private static boolean displayAvailable()
	{
		return !GraphicsEnvironment.isHeadless();
	}

	/**
	 * Attach this hint to {@code target}. While the cursor is inside the target, the hint shows
	 * {@code textSupplier.get()} four pixels above it, horizontally centred. The supplier is
	 * re-read on every enter so the text follows late updates (e.g. a button changing meaning).
	 *
	 * <p>Attach is single-use per target component: calling it twice on the same target registers
	 * two listeners which would race on every hover. Callers rebuild their components from scratch
	 * on every panel rebuild, so re-attaching is naturally avoided.
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
				visibleIntent = true;
				if (window == null) return;
				window.pack();
				positionAbove(target);
				window.setVisible(true);
			}

			@Override public void mouseExited(MouseEvent e)
			{
				if (disposed) return;
				visibleIntent = false;
				if (window != null) window.setVisible(false);
			}
		});
	}

	private void positionAbove(JComponent target)
	{
		if (window == null) return;
		if (!target.isShowing()) return;
		Point onScreen = target.getLocationOnScreen();
		int gap = 4;
		int x = onScreen.x + (target.getWidth() - window.getWidth()) / 2;
		int y = onScreen.y - window.getHeight() - gap;
		window.setLocation(x, y);
	}

	/**
	 * Releases the hint's {@link JWindow}. Subsequent {@link #attach} calls still register
	 * listeners but those listeners short-circuit because {@code disposed} is sticky. Safe to
	 * call multiple times.
	 */
	void dispose()
	{
		if (disposed) return;
		disposed = true;
		visibleIntent = false;
		if (window != null) window.dispose();
	}
}
