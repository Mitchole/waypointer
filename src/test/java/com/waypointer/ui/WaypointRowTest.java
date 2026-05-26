package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import java.time.Instant;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.*;

/** Tests for {@link WaypointRow}'s pure helpers. The row itself is Swing-driven and untested. */
public class WaypointRowTest
{
    private static Waypoint wp(String name, String notes)
    {
        return new Waypoint(UUID.randomUUID(), name, 0, UUID.randomUUID(), null,
            notes == null ? "" : notes, Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false);
    }

    @Test
    public void emptyNotesReturnNamePlain()
    {
        assertEquals("Vorkath", WaypointRow.buildHoverTooltip(wp("Vorkath", "")));
        assertEquals("Vorkath", WaypointRow.buildHoverTooltip(wp("Vorkath", null)));
    }

    @Test
    public void notesPreviewWrapsNameAndFirstLineInHtml()
    {
        String tip = WaypointRow.buildHoverTooltip(wp("Vorkath", "Drops dragonbones"));
        assertTrue("expected HTML tooltip with name + notes preview, got: " + tip,
            tip.startsWith("<html>"));
        assertTrue(tip.contains("Vorkath"));
        assertTrue(tip.contains("Drops dragonbones"));
    }

    @Test
    public void multilineNotesShowOnlyFirstLine()
    {
        String tip = WaypointRow.buildHoverTooltip(
            wp("Yew", "100k/hr at 90 wc\nbring axe and energy pots\nbank at varrock"));
        assertTrue(tip.contains("100k/hr at 90 wc"));
        assertFalse("only first line of notes belongs in the preview",
            tip.contains("bring axe"));
    }

    @Test
    public void whitespaceOnlyFirstLineFallsBackToBareName()
    {
        // notes starts with newlines / blank line: preview is empty, so show name only.
        String tip = WaypointRow.buildHoverTooltip(wp("Yew", "   \n actual content"));
        assertEquals("Yew", tip);
    }

    @Test
    public void htmlInUserFieldsIsEscaped()
    {
        String tip = WaypointRow.buildHoverTooltip(
            wp("<script>alert('x')</script>", "more <evil> stuff"));
        assertFalse("name must be HTML-escaped", tip.contains("<script>"));
        assertTrue(tip.contains("&lt;script&gt;"));
        assertTrue(tip.contains("&lt;evil&gt;"));
    }
}
