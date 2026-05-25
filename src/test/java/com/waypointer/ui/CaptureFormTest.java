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

    @Test
    public void cancelHidesFormAndCreatesNoWaypoint()
    {
        form.show(packed);
        form.setNameText("ignored");

        form.clickCancel();

        assertFalse(form.isVisible());
        assertEquals(0, store.getLibrary().getWaypoints().size());
    }

    @Test
    public void newCategorySentinelRevealsInlineSubRow()
    {
        form.show(packed);
        assertFalse(form.isNewCategoryRowVisible());

        form.selectNewCategorySentinel();

        assertTrue(form.isNewCategoryRowVisible());
    }

    @Test
    public void createInlineCategoryAddsCategoryAndSelectsIt()
    {
        form.show(packed);
        form.selectNewCategorySentinel();
        form.setNewCategoryNameText("Bosses");

        form.clickCreateNewCategory();

        assertFalse("sub-row should hide once category is created",
            form.isNewCategoryRowVisible());
        assertEquals("Bosses",
            ((CategoryComboItem) form.selectedCategoryItem()).toString());
    }

    @Test
    public void saveAfterCreateInlineCategoryStoresWaypointInThatCategory()
    {
        form.show(packed);
        form.selectNewCategorySentinel();
        form.setNewCategoryNameText("Bosses");
        form.clickCreateNewCategory();
        form.setNameText("Vorkath");

        form.clickSave();

        com.waypointer.model.Category created = store.getCategoryByName("Bosses");
        assertEquals(created.getId(),
            store.getLibrary().getWaypoints().get(0).getCategoryId());
    }

    @Test
    public void cancelInlineCategoryRevertsComboSelection()
    {
        store.createCategory("Existing");
        form.show(packed);
        form.selectCategoryByName("Existing");
        form.selectNewCategorySentinel();

        form.clickCancelNewCategory();

        assertFalse(form.isNewCategoryRowVisible());
        assertEquals("Existing",
            ((CategoryComboItem) form.selectedCategoryItem()).toString());
    }

    @Test
    public void duplicateCategoryNameShowsInlineErrorAndKeepsFormOpen()
    {
        store.createCategory("Bosses");
        form.show(packed);
        form.selectNewCategorySentinel();
        form.setNewCategoryNameText("Bosses");

        form.clickCreateNewCategory();

        assertTrue(form.isVisible());
        assertTrue(form.isNewCategoryRowVisible());
        assertTrue("inline error should mention duplicate name, got: " + form.getErrorText(),
            form.getErrorText().contains("Bosses"));
        assertEquals("no second category should have been created",
            1L, store.getCategoriesOrdered().stream()
                .filter(c -> "Bosses".equals(c.getName())).count());
    }
}
