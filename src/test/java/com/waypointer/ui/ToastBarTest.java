package com.waypointer.ui;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ToastBarTest
{
    @Test
    public void hiddenAfterConstruction()
    {
        ToastBar bar = new ToastBar();
        assertFalse(bar.isVisible());
    }

    @Test
    public void showMakesVisibleAndSetsMessage()
    {
        ToastBar bar = new ToastBar();
        bar.show("Added Varrock");
        assertTrue(bar.isVisible());
        assertTrue("expected label to mention the waypoint, got: " + bar.getMessageText(),
            bar.getMessageText().contains("Added Varrock"));
    }

    @Test
    public void showEscapesHtmlInMessage()
    {
        ToastBar bar = new ToastBar();
        bar.show("<b>cheeky</b>");
        String html = bar.getMessageText();
        assertFalse("raw <b> should not appear in label HTML, got: " + html,
            html.contains("<b>cheeky</b>"));
        assertTrue("escaped marker should appear, got: " + html, html.contains("&lt;b&gt;"));
    }
}
