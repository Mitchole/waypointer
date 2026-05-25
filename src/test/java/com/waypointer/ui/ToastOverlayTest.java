package com.waypointer.ui;

import javax.swing.JLabel;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ToastOverlayTest
{
    @Test
    public void wrapsContentAndStartsWithCardHidden()
    {
        JLabel content = new JLabel("inner");
        ToastOverlay overlay = new ToastOverlay(content);

        // The wrapped content is a direct child of the overlay (in some layer).
        boolean found = false;
        for (java.awt.Component c : overlay.getComponents())
        {
            if (c == content) found = true;
        }
        assertTrue("expected content to be a child of the overlay", found);

        // No card is visible until show() is called.
        assertFalse("toast card should be hidden initially", overlay.cardIsVisibleForTest());
    }

    @Test
    public void showMakesCardVisibleAndSetsText()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300); // give it a non-zero bound so layout runs

        overlay.show("Added Varrock");

        assertTrue("card should be visible after show()", overlay.cardIsVisibleForTest());
        assertTrue("expected label to mention the message, got: " + overlay.cardLabelTextForTest(),
            overlay.cardLabelTextForTest().contains("Added Varrock"));
    }

    @Test
    public void showEscapesHtmlInMessage()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300);

        overlay.show("<b>cheeky</b>");

        String rendered = overlay.cardLabelTextForTest();
        assertFalse("raw <b> should not appear in label HTML, got: " + rendered,
            rendered.contains("<b>cheeky</b>"));
        assertTrue("escaped marker should appear, got: " + rendered, rendered.contains("&lt;b&gt;"));
    }

    @Test
    public void showSchedulesAutoHideAtDefaultDuration()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300);

        overlay.show("hi");

        javax.swing.Timer t = overlay.autoHideTimerForTest();
        org.junit.Assert.assertNotNull("expected timer to exist after show()", t);
        org.junit.Assert.assertTrue("expected timer to be running", t.isRunning());
        org.junit.Assert.assertEquals(2500, t.getDelay());
    }

    @Test
    public void showWithActionRendersActionLabel()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300);

        overlay.show("Deleted Catherby", "Undo", () -> {});

        JLabel action = overlay.actionLabelForTest();
        org.junit.Assert.assertNotNull("expected action label to be rendered", action);
        org.junit.Assert.assertTrue("expected action label text 'Undo', got: " + action.getText(),
            action.getText().contains("Undo"));
        org.junit.Assert.assertTrue("expected action label visible", action.isVisible());
    }

    @Test
    public void clickingActionInvokesRunnableAndHidesCard()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300);
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        overlay.show("Deleted Catherby", "Undo", calls::incrementAndGet);

        // Simulate a mouse click on the action label by dispatching the canonical click event.
        JLabel action = overlay.actionLabelForTest();
        java.awt.event.MouseEvent click = new java.awt.event.MouseEvent(
            action, java.awt.event.MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
            0, 0, 0, 1, false);
        for (java.awt.event.MouseListener l : action.getMouseListeners()) l.mouseClicked(click);

        org.junit.Assert.assertEquals("runnable should fire exactly once", 1, calls.get());
        assertFalse("card should hide after action click", overlay.cardIsVisibleForTest());
    }

    @Test
    public void showWithActionUsesLongerDuration()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300);

        overlay.show("Deleted Catherby", "Undo", () -> {});

        org.junit.Assert.assertEquals(6000, overlay.autoHideTimerForTest().getDelay());
    }

    @Test
    public void rapidShowReusesSameEntryAndDoesNotIncrementAnimationCount()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300);

        overlay.show("first");
        int afterFirst = overlay.entryAnimationCountForTest();
        overlay.show("second");
        int afterSecond = overlay.entryAnimationCountForTest();

        org.junit.Assert.assertEquals(
            "second show() should reuse visible card, entry animation count must not increment",
            afterFirst, afterSecond);
        org.junit.Assert.assertTrue("expected second show() text to be reflected",
            overlay.cardLabelTextForTest().contains("second"));
    }

    @Test
    public void firstShowStartsAnimationOffScreenAndSettlesAtRestingY()
    {
        ToastOverlay overlay = new ToastOverlay(new JLabel("inner"));
        overlay.setSize(200, 300);

        overlay.show("hello");

        // Right after show(), the card should be at the off-screen start position.
        int startY = overlay.cardCurrentYForTest();
        org.junit.Assert.assertEquals("card should start at the bottom edge", 300, startY);

        // Finish the animation synchronously.
        overlay.completeEntryAnimationForTest();
        int settledY = overlay.cardCurrentYForTest();
        org.junit.Assert.assertTrue("card must settle above the bottom edge, got y=" + settledY,
            settledY < 300);
    }
}
