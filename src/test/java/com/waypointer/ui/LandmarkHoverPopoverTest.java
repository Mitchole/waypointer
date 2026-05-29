package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.awt.GraphicsEnvironment;
import org.junit.Assume;
import org.junit.Test;

public class LandmarkHoverPopoverTest
{
	@Test
	public void newPopoverIsNotVisibleAndHasEmptyLabel()
	{
		Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
		LandmarkHoverPopover popover = new LandmarkHoverPopover();
		assertFalse("a freshly-constructed popover must not be visible",
			popover.visibleIntentForTest());
		assertEquals("label text starts empty until attach fires",
			"", popover.labelTextForTest());
	}

	@Test
	public void mouseEnterShowsPopoverWithSupplierText()
	{
		Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
		LandmarkHoverPopover popover = new LandmarkHoverPopover();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		popover.attach(btn, () -> "Bank");

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));

		org.junit.Assert.assertTrue("mouseEntered must flip visibleIntent",
			popover.visibleIntentForTest());
		assertEquals("label must reflect the supplier value",
			"Bank", popover.labelTextForTest());
	}

	@Test
	public void mouseExitHidesPopover()
	{
		Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
		LandmarkHoverPopover popover = new LandmarkHoverPopover();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		popover.attach(btn, () -> "Altar");

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_EXITED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));

		assertFalse("mouseExited must flip visibleIntent off",
			popover.visibleIntentForTest());
	}

	@Test
	public void mouseEnterRereadsSupplierEachTime()
	{
		Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
		LandmarkHoverPopover popover = new LandmarkHoverPopover();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		String[] current = {"Bank"};
		popover.attach(btn, () -> current[0]);

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		assertEquals("Bank", popover.labelTextForTest());

		current[0] = "Anvil";
		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_EXITED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		assertEquals("supplier must be re-read on every enter",
			"Anvil", popover.labelTextForTest());
	}

	@Test
	public void disposeIsIdempotent()
	{
		Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
		LandmarkHoverPopover popover = new LandmarkHoverPopover();
		popover.dispose();
		popover.dispose(); // must not throw
	}

	@Test
	public void afterDisposeMouseEnterIsNoOp()
	{
		Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
		LandmarkHoverPopover popover = new LandmarkHoverPopover();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		popover.attach(btn, () -> "Bank");
		popover.dispose();

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));

		assertFalse("after dispose, mouseEntered must not flip visibleIntent",
			popover.visibleIntentForTest());
	}
}
