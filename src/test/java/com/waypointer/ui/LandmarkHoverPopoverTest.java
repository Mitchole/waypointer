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
}
