package com.waypointer.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class LandmarkHoverPopoverTest
{
	@Test
	public void newPopoverIsNotVisibleAndHasEmptyLabel()
	{
		LandmarkHoverPopover popover = new LandmarkHoverPopover();
		assertFalse("a freshly-constructed popover must not be visible",
			popover.visibleIntentForTest());
		assertEquals("label text starts empty until attach fires",
			"", popover.labelTextForTest());
	}
}
