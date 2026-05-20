package com.waypointer.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PasteImportDialogTest
{
    @Test public void wp1IsWaypointSingle()    { assertEquals(PasteImportDialog.CodeKind.WAYPOINT_SINGLE,  PasteImportDialog.sniff("WP1:abc")); }
    @Test public void wpl1IsWaypointLibrary()  { assertEquals(PasteImportDialog.CodeKind.WAYPOINT_LIBRARY, PasteImportDialog.sniff("WPL1:abc")); }
    @Test public void leadingWhitespaceTolerated() { assertEquals(PasteImportDialog.CodeKind.WAYPOINT_SINGLE, PasteImportDialog.sniff("  WP1:abc")); }
    @Test public void rt1IsUnknown()           { assertEquals(PasteImportDialog.CodeKind.UNKNOWN, PasteImportDialog.sniff("RT1:abc")); }
    @Test public void rtl1IsUnknown()          { assertEquals(PasteImportDialog.CodeKind.UNKNOWN, PasteImportDialog.sniff("RTL1:abc")); }
    @Test public void unknownIsUnknown()       { assertEquals(PasteImportDialog.CodeKind.UNKNOWN, PasteImportDialog.sniff("HELLO")); }
    @Test public void nullIsUnknown()          { assertEquals(PasteImportDialog.CodeKind.UNKNOWN, PasteImportDialog.sniff(null)); }
}
