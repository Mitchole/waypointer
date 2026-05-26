package com.waypointer.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaceholderTextFieldTest
{
    @Test
    public void placeholderDoesNotPolluteText()
    {
        PlaceholderTextField f = new PlaceholderTextField("Search waypoints...");
        // Filter logic reads getText(); the placeholder is paint-only and must not appear here.
        assertEquals("", f.getText());
    }

    @Test
    public void placeholderRetrievable()
    {
        PlaceholderTextField f = new PlaceholderTextField("hint");
        assertEquals("hint", f.getPlaceholder());
    }

    @Test
    public void nullPlaceholderTreatedAsEmpty()
    {
        PlaceholderTextField f = new PlaceholderTextField(null);
        assertEquals("", f.getPlaceholder());
    }

    @Test
    public void userTypedTextWins()
    {
        PlaceholderTextField f = new PlaceholderTextField("hint");
        f.setText("typed");
        assertEquals("typed", f.getText());
    }
}
