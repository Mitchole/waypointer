package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointCapture;
import com.waypointer.service.WaypointStore;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CaptureFormTest
{
    private WaypointStore store;
    private WaypointCapture capture;
    private CaptureForm form;
    private int packed;

    @Before
    public void setUp()
    {
        store = new WaypointStore();
        store.bootstrap(new Library());
        capture = mock(WaypointCapture.class);
        packed = WorldPointPacker.pack(3222, 3218, 0);
        when(capture.defaultName(packed)).thenReturn("Lumbridge");
        form = new CaptureForm(store, capture);
    }

    @Test
    public void hiddenByDefault()
    {
        assertFalse(form.isVisible());
    }

    @Test
    public void showPopulatesDefaultNameAndBecomesVisible()
    {
        form.show(packed);
        assertTrue(form.isVisible());
        assertEquals("Lumbridge", form.getNameText());
    }

    @Test
    public void dismissFlipsVisibilityOff()
    {
        form.show(packed);
        form.dismiss();
        assertFalse(form.isVisible());
    }

    @Test
    public void saveCreatesWaypointWithTypedNameInSelectedCategory()
    {
        store.createCategory("Bosses");
        form.show(packed);
        form.setNameText("Vorkath");
        form.selectCategoryByName("Bosses");

        form.clickSave();

        assertFalse(form.isVisible());
        assertEquals(1, store.getLibrary().getWaypoints().size());
        com.waypointer.model.Waypoint w = store.getLibrary().getWaypoints().get(0);
        assertEquals("Vorkath", w.getName());
        assertEquals(packed, w.getPackedWorldPoint());
        assertEquals(store.getCategoryByName("Bosses").getId(), w.getCategoryId());
    }

    @Test
    public void saveWithBlankNameFallsBackToDefault()
    {
        form.show(packed);
        form.setNameText("   ");

        form.clickSave();

        assertEquals("Lumbridge", store.getLibrary().getWaypoints().get(0).getName());
    }
}
