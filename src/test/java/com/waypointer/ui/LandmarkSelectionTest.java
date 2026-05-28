package com.waypointer.ui;

import com.waypointer.service.LandmarkType;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class LandmarkSelectionTest
{
    @Test
    public void canonicalDefault_orderContainsAllElevenTypes()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        assertEquals(LandmarkType.values().length, s.order().size());
    }

    @Test
    public void canonicalDefault_orderStartsWithDefaults()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        List<LandmarkType> expectedHead = Arrays.asList(
            LandmarkType.BANK, LandmarkType.ALTAR,
            LandmarkType.SPIRIT_TREE, LandmarkType.FAIRY_RING);
        assertEquals(expectedHead, s.order().subList(0, 4));
    }

    @Test
    public void canonicalDefault_selectedIsTheFourDefaults()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        assertTrue(s.isSelected(LandmarkType.BANK));
        assertTrue(s.isSelected(LandmarkType.ALTAR));
        assertTrue(s.isSelected(LandmarkType.SPIRIT_TREE));
        assertTrue(s.isSelected(LandmarkType.FAIRY_RING));
        assertFalse(s.isSelected(LandmarkType.SLAYER_MASTER));
        assertFalse(s.isSelected(LandmarkType.ANVIL));
    }
}
