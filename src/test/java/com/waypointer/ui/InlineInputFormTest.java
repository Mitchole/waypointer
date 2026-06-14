package com.waypointer.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InlineInputFormTest
{
    @Test
    public void hiddenByDefault()
    {
        InlineInputForm form = new InlineInputForm();
        assertFalse(form.isVisible());
    }

    @Test
    public void openForShowsFormAndDisablesSubmitWhileEmpty()
    {
        InlineInputForm form = new InlineInputForm();
        form.openFor("New route", "Create", text -> null);
        assertTrue(form.isVisible());
        assertFalse("empty field must keep Submit disabled", form.isSubmitEnabled());
    }

    @Test
    public void whitespaceKeepsSubmitDisabled()
    {
        InlineInputForm form = new InlineInputForm();
        form.openFor("New route", "Create", text -> null);
        form.setText("   ");
        assertFalse(form.isSubmitEnabled());
    }

    @Test
    public void nonBlankTextEnablesSubmit()
    {
        InlineInputForm form = new InlineInputForm();
        form.openFor("New route", "Create", text -> null);
        form.setText("My Route");
        assertTrue(form.isSubmitEnabled());
    }

    @Test
    public void clickSubmitWhileEmptyDoesNothing()
    {
        InlineInputForm form = new InlineInputForm();
        boolean[] called = {false};
        form.openFor("New route", "Create", text -> { called[0] = true; return null; });

        form.clickSubmit();   // button is disabled while empty -> no-op

        assertFalse("submit handler must not fire on an empty field", called[0]);
        assertTrue("form stays open when nothing was submitted", form.isVisible());
    }

    @Test
    public void successfulSubmitTrimsTextHidesFormAndInvokesHandler()
    {
        InlineInputForm form = new InlineInputForm();
        String[] seen = new String[1];
        form.openFor("New route", "Create", text -> { seen[0] = text; return null; });
        form.setText("  My Route  ");
        form.clickSubmit();
        assertEquals("My Route", seen[0]);
        assertFalse(form.isVisible());
    }

    @Test
    public void failedSubmitShowsErrorAndKeepsFormOpen()
    {
        InlineInputForm form = new InlineInputForm();
        form.openFor("Import route", "Import", text -> "Not a readable route code (expected RT1:).");
        form.setText("RT1:garbage");
        form.clickSubmit();
        assertTrue(form.isVisible());
        assertTrue(form.getErrorText().contains("RT1:"));
    }

    @Test
    public void cancelHidesFormWithoutInvokingHandler()
    {
        InlineInputForm form = new InlineInputForm();
        boolean[] called = {false};
        form.openFor("New route", "Create", text -> { called[0] = true; return null; });
        form.setText("My Route");
        form.clickCancel();
        assertFalse(form.isVisible());
        assertFalse(called[0]);
    }

    @Test
    public void reopeningClearsPreviousTextAndError()
    {
        InlineInputForm form = new InlineInputForm();
        form.openFor("Import route", "Import", text -> "bad");
        form.setText("oops");
        form.clickSubmit();
        assertTrue(form.getErrorText().contains("bad"));

        form.openFor("New route", "Create", text -> null);
        assertEquals(" ", form.getErrorText());
        assertFalse(form.isSubmitEnabled());
    }
}
