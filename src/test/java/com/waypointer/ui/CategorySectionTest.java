package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CategorySectionTest
{
    private static CategorySection.Actions noopActions()
    {
        return new CategorySection.Actions(() -> {}, () -> {}, () -> {}, () -> {}, m -> {}, () -> {});
    }

    @Test
    public void specBuildsSectionForItsCategory()
    {
        UUID id = UUID.randomUUID();
        Category cat = new Category(id, "Bosses", 1, false, null, false);
        CategorySection section = CategorySection.spec(cat, Collections.<Waypoint>emptyList())
            .actions(noopActions())
            .build();
        assertNotNull(section);
        assertEquals(id, section.getCategoryId());
    }
}
