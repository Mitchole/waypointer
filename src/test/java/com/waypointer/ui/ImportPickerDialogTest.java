package com.waypointer.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.waypointer.codec.LibraryJsonCodec;
import com.waypointer.codec.WaypointShareCodec;
import com.waypointer.model.Category;
import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.service.WaypointStore;
import java.awt.GraphicsEnvironment;
import java.time.Instant;
import java.util.UUID;
import org.junit.Assume;
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
    private LibraryJsonCodec libraryCodec;

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
        shareCodec = new WaypointShareCodec(gson);
        libraryCodec = new LibraryJsonCodec(gson);
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

    private ImportPickerDialog newDialog(WaypointStore store)
    {
        return new ImportPickerDialog(null, store, shareCodec, libraryCodec, Toasts.NO_OP);
    }

    @Test
    public void codeSourceDecodesAndPopulatesTree()
    {
        Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
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
    public void fileSourceDecodesAndPopulatesTree() throws Exception
    {
        Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
        String json = libraryCodec.encode(oneWaypointLibrary());
        java.io.File tmp = java.io.File.createTempFile("waypointer-import", ".json");
        tmp.deleteOnExit();
        java.nio.file.Files.write(tmp.toPath(),
            json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        Library decoded = d.decodeFileOrNull(tmp);
        assertNotNull(decoded);
        d.populate(decoded);
        assertTrue(d.hasLoadedSource());
    }

    @Test
    public void malformedSourceLeavesTreeEmpty() throws Exception
    {
        Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
        WaypointStore store = new WaypointStore();
        store.bootstrap(new Library());
        ImportPickerDialog d = newDialog(store);

        assertNull(d.decodeCodeOrNull("not a real code"));         // no magic
        assertNull(d.decodeCodeOrNull("WPL1:@@@not-base64@@@"));    // right magic, junk body
        java.io.File junk = java.io.File.createTempFile("waypointer-junk", ".json");
        junk.deleteOnExit();
        java.nio.file.Files.write(junk.toPath(),
            "{not valid json".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertNull(d.decodeFileOrNull(junk));
        assertFalse(d.hasLoadedSource());
    }

    @Test
    public void importBuildsSelectedSubsetAndMergesOnce()
    {
        Assume.assumeFalse("Swing widgets require a display", GraphicsEnvironment.isHeadless());
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
}
