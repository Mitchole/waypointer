package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.waypointer.testsupport.Headless;
import org.junit.Test;

public class HoverHintTest
{
	@Test
	public void newHintIsNotVisibleAndHasEmptyLabel()
	{
		Headless.assumeDisplay();
		HoverHint hint = new HoverHint();
		assertFalse("a freshly-constructed hint must not be visible",
			hint.visibleIntentForTest());
		assertEquals("label text starts empty until attach fires",
			"", hint.labelTextForTest());
	}

	@Test
	public void mouseEnterShowsHintWithSupplierText()
	{
		Headless.assumeDisplay();
		HoverHint hint = new HoverHint();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		hint.attach(btn, () -> "Bank");

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));

		org.junit.Assert.assertTrue("mouseEntered must flip visibleIntent",
			hint.visibleIntentForTest());
		assertEquals("label must reflect the supplier value",
			"Bank", hint.labelTextForTest());
	}

	@Test
	public void mouseExitHidesHint()
	{
		Headless.assumeDisplay();
		HoverHint hint = new HoverHint();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		hint.attach(btn, () -> "Altar");

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_EXITED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));

		assertFalse("mouseExited must flip visibleIntent off",
			hint.visibleIntentForTest());
	}

	@Test
	public void mouseEnterRereadsSupplierEachTime()
	{
		Headless.assumeDisplay();
		HoverHint hint = new HoverHint();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		String[] current = {"Bank"};
		hint.attach(btn, () -> current[0]);

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		assertEquals("Bank", hint.labelTextForTest());

		current[0] = "Anvil";
		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_EXITED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		assertEquals("supplier must be re-read on every enter",
			"Anvil", hint.labelTextForTest());
	}

	@Test
	public void disposeIsIdempotent()
	{
		Headless.assumeDisplay();
		HoverHint hint = new HoverHint();
		hint.dispose();
		assertFalse("dispose must leave visibleIntent false",
			hint.visibleIntentForTest());
		hint.dispose();
		assertFalse("visibleIntent must still be false after second dispose",
			hint.visibleIntentForTest());
	}

	@Test
	public void afterDisposeMouseEnterIsNoOp()
	{
		Headless.assumeDisplay();
		HoverHint hint = new HoverHint();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		hint.attach(btn, () -> "Bank");
		hint.dispose();

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));

		assertFalse("after dispose, mouseEntered must not flip visibleIntent",
			hint.visibleIntentForTest());
		assertEquals("after dispose, label text must be unchanged",
			"", hint.labelTextForTest());
	}

	@Test
	public void disposeWhileShowingHidesTheWindow()
	{
		Headless.assumeDisplay();
		HoverHint hint = new HoverHint();
		javax.swing.JButton btn = new javax.swing.JButton();
		btn.setSize(34, 34);
		hint.attach(btn, () -> "Bank");

		btn.dispatchEvent(new java.awt.event.MouseEvent(btn,
			java.awt.event.MouseEvent.MOUSE_ENTERED,
			System.currentTimeMillis(), 0, 0, 0, 0, false));
		org.junit.Assert.assertTrue("hint should be visible before dispose",
			hint.visibleIntentForTest());

		hint.dispose();
		assertFalse("dispose called while showing must drop visibleIntent",
			hint.visibleIntentForTest());
	}

	@Test
	public void sharedReturnsAStableInstance()
	{
		Headless.assumeDisplay();
		HoverHint a = HoverHint.shared();
		HoverHint b = HoverHint.shared();
		org.junit.Assert.assertNotNull("shared() must never return null", a);
		org.junit.Assert.assertSame("shared() must return the same instance", a, b);
	}
}
