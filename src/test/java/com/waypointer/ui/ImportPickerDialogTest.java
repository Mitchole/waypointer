package com.waypointer.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.codec.DecodeReport;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.ReadableImport;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.service.WaypointStore;
import com.waypointer.testsupport.Headless;
import java.time.Instant;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImportPickerDialogTest
{
    private WaypointShareCodec shareCodec;

    @Before
    public void setUp()
    {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonSerializer<Instant>) (src, t, c) ->
                    new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class,
                (com.google.gson.JsonDeserializer<Instant>) (e, t, c) ->
                    Instant.parse(e.getAsString()))
            .create();
        LibraryJsonCodec libraryCodec = new LibraryJsonCodec(gson);
        shareCodec = new WaypointShareCodec(gson, libraryCodec);
    }

    private static Library oneWaypointLibrary()
    {
        Library lib = new Library();
        UUID cat = UUID.randomUUID();
        lib.getCategories().add(new Category(cat, "Banks", 0, false, null, false));
        lib.getWaypoints().add(new Waypoint(UUID.randomUUID(), "GE", 9, cat, null, "",
            Instant.parse("2026-05-02T00:00:00Z"), 0, false, null, false));
        return lib;
    }

    private ImportPickerDialog newDialog(WaypointStore store, Toasts toasts)
    {
        return new ImportPickerDialog(null, store, shareCodec, new ReadableImport(new Gson()), toasts);
    }

    private ImportPickerDialog newDialog(WaypointStore store)
    {
        return newDialog(store, Toasts.NO_OP);
    }

    @Test
    public void codeSourceDecodesAndPopulatesTree()
    {
        Headless.assumeDisplay();
        String code = shareCodec.encodeLibrary(oneWaypointLibrary());

        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        Library decoded = d.decodeCodeOrNull(code);
        assertNotNull(decoded);
        d.populate(decoded);
        assertTrue(d.hasLoadedSource());
    }

    @Test
    public void malformedSourceLeavesTreeEmpty()
    {
        Headless.assumeDisplay();
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        assertNull(d.decodeCodeOrNull("not a real code"));         // no magic
        assertNull(d.decodeCodeOrNull("WPL1:@@@not-base64@@@"));    // right magic, junk body
        assertFalse(d.hasLoadedSource());
    }

    @Test
    public void importBuildsSelectedSubsetAndMergesOnce()
    {
        Headless.assumeDisplay();
        String code = shareCodec.encodeLibrary(oneWaypointLibrary());

        WaypointStore store = mock(WaypointStore.class);
        when(store.importMerge(any())).thenReturn(new WaypointStore.ImportResult());
        ImportPickerDialog d = newDialog(store);

        d.populate(d.decodeCodeOrNull(code));
        d.importSelected();

        ArgumentCaptor<Library> captor = ArgumentCaptor.forClass(Library.class);
        verify(store, times(1)).importMerge(captor.capture());
        Library captured = captor.getValue();
        assertEquals(1, captured.getWaypoints().size());
        assertEquals("GE", captured.getWaypoints().get(0).getName());
        assertEquals(1, captured.getCategories().size());
        assertEquals("Banks", captured.getCategories().get(0).getName());
        assertEquals(captured.getCategories().get(0).getId(),
            captured.getWaypoints().get(0).getCategoryId());
    }

    @Test
    public void loadNoticeOnlyWhenEntriesDropped()
    {
        DecodeReport noDrops = new DecodeReport(new Library(), 0, 0);
        assertNull(ImportPickerDialog.loadNoticeOrNull(noDrops));

        DecodeReport withDrops = new DecodeReport(oneWaypointLibrary(), 2, 1);
        String notice = ImportPickerDialog.loadNoticeOrNull(withDrops);
        assertNotNull(notice);
        assertTrue(notice.contains("3 invalid"));
    }

    @Test
    public void importSummaryOmitsSkippedWhenZero()
    {
        WaypointStore.ImportResult none = new WaypointStore.ImportResult();
        none.waypointsAdded = 2;
        none.categoriesAdded = 1;
        assertEquals("Imported 2 waypoints, 1 categories.", ImportPickerDialog.importSummary(none));

        WaypointStore.ImportResult some = new WaypointStore.ImportResult();
        some.waypointsAdded = 1;
        some.waypointsSkipped = 3;
        assertTrue(ImportPickerDialog.importSummary(some).contains("Skipped 3 already in your library"));
    }

    @Test
    public void badCodeShowsInlineErrorNotModal()
    {
        Headless.assumeDisplay();
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        d.loadCode("WP1:not a real code");

        assertTrue("error mentions the expected prefixes, got: " + d.getErrorText(),
            d.getErrorText().contains("WP1:"));
        assertTrue(d.isErrorVisible());
        assertFalse(d.hasLoadedSource());
    }

    @Test
    public void validCodeAfterBadOneClearsErrorAndPopulates()
    {
        Headless.assumeDisplay();
        String code = shareCodec.encodeLibrary(oneWaypointLibrary());
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        d.loadCode("garbage");   // shows the inline error
        d.loadCode(code);        // valid -> clears it and loads the tree

        assertFalse(d.isErrorVisible());
        assertTrue(d.hasLoadedSource());
    }

    @Test
    public void readableListLoadsIntoPicker()
    {
        Headless.assumeDisplay();
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        d.loadCode("# Sea\nWreck, 2301, 3690\n");

        assertFalse(d.isErrorVisible());
        assertTrue(d.hasLoadedSource());
    }

    @Test
    public void malformedListShowsLineError()
    {
        Headless.assumeDisplay();
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        d.loadCode("A 1 2\nJust a name");

        assertEquals("Line 2: expected a name followed by x, y.", d.getErrorText());
        assertTrue(d.isErrorVisible());
        assertFalse(d.hasLoadedSource());
    }

    @Test
    public void alreadyPresentCountShown()
    {
        Headless.assumeDisplay();
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        UUID sea = store.createCategory("Sea").getId();
        store.createWaypoint(com.waypointer.model.WorldPointPacker.pack(10, 20, 0), "Wreck", sea);
        java.util.List<String> shown = new java.util.ArrayList<>();
        Toasts capture = new Toasts()
        {
            @Override public void show(String text) { shown.add(text); }
            @Override public void show(String text, String label, Runnable onClick) { shown.add(text); }
        };
        ImportPickerDialog d = newDialog(store, capture);

        d.loadCode("# Sea\nWreck 10 20\nNew 1 2");

        assertEquals(java.util.Collections.singletonList("1 already in your library, skipped."), shown);
        assertEquals("1 already in your library, skipped.", ImportPickerDialog.alreadyPresentNoticeOrNull(1));
        assertNull(ImportPickerDialog.alreadyPresentNoticeOrNull(0));
    }

    @Test
    public void mistypedShareCodeShowsShareCodeError()
    {
        Headless.assumeDisplay();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 40; i++) token.append("H4sIAAAAAAAA");
        for (String input : new String[] {"P1:abc", "wp1:abc", token.toString()})
        {
            WaypointStore store = new WaypointStore();
            store.bootstrap(new Library());
            ImportPickerDialog d = newDialog(store);

            d.loadCode(input);

            assertTrue(input, d.getErrorText().contains("WP1:"));
            assertFalse(d.hasLoadedSource());
        }
    }

    @Test
    public void malformedCompactJsonShowsListError()
    {
        Headless.assumeDisplay();
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        d.loadCode("[{\"name\":\"A\",\"x\":1}]");

        assertEquals("Entry 1: needs a name, x and y.", d.getErrorText());
    }

    @Test
    public void multiWordGarbageShowsListError()
    {
        Headless.assumeDisplay();
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        d.loadCode("not a real code");

        assertEquals("Line 1: expected a name followed by x, y.", d.getErrorText());
    }
}
