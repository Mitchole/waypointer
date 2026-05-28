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

    @Test
    public void parse_emptyString_returnsCanonicalDefault()
    {
        LandmarkSelection s = LandmarkSelection.parse("", new com.google.gson.Gson());
        assertEquals(LandmarkSelection.canonicalDefault().order(), s.order());
        assertTrue(s.isSelected(LandmarkType.BANK));
        assertFalse(s.isSelected(LandmarkType.ANVIL));
    }

    @Test
    public void parse_null_returnsCanonicalDefault()
    {
        LandmarkSelection s = LandmarkSelection.parse(null, new com.google.gson.Gson());
        assertEquals(LandmarkSelection.canonicalDefault().order(), s.order());
    }

    @Test
    public void parse_validJson_preservesOrderAndSelection()
    {
        String json = "{\"order\":[\"ANVIL\",\"BANK\",\"ALTAR\",\"FURNACE\",\"LOOM\",\"SPINNING_WHEEL\",\"TANNER\",\"SPIRIT_TREE\",\"CHARTER_SHIP\",\"FAIRY_RING\",\"SLAYER_MASTER\"],"
            + "\"selected\":[\"ANVIL\",\"BANK\"]}";
        LandmarkSelection s = LandmarkSelection.parse(json, new com.google.gson.Gson());
        assertEquals(LandmarkType.ANVIL, s.order().get(0));
        assertEquals(LandmarkType.BANK, s.order().get(1));
        assertTrue(s.isSelected(LandmarkType.ANVIL));
        assertTrue(s.isSelected(LandmarkType.BANK));
        assertFalse(s.isSelected(LandmarkType.ALTAR));
    }
}
