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

    @Test
    public void parse_malformedJson_returnsCanonicalDefault()
    {
        LandmarkSelection s = LandmarkSelection.parse("{not json", new com.google.gson.Gson());
        assertEquals(LandmarkSelection.canonicalDefault().order(), s.order());
    }

    @Test
    public void parse_unknownEnumName_isSkipped()
    {
        String json = "{\"order\":[\"BANK\",\"GHOSTBUSTERS\",\"ALTAR\"],\"selected\":[\"BANK\",\"GHOSTBUSTERS\"]}";
        LandmarkSelection s = LandmarkSelection.parse(json, new com.google.gson.Gson());
        // BANK + ALTAR retained at positions 0,1; remaining 9 types appended in enum order.
        assertEquals(LandmarkType.BANK, s.order().get(0));
        assertEquals(LandmarkType.ALTAR, s.order().get(1));
        assertEquals(LandmarkType.values().length, s.order().size());
        assertTrue(s.isSelected(LandmarkType.BANK));
        // Unknown name in selected is silently dropped.
        assertFalse(s.isSelected(LandmarkType.ALTAR));
    }

    @Test
    public void parse_missingEnumInOrder_isAppendedCanonical()
    {
        // Only BANK and ALTAR listed; remaining 9 must be appended in enum order.
        String json = "{\"order\":[\"BANK\",\"ALTAR\"],\"selected\":[\"BANK\"]}";
        LandmarkSelection s = LandmarkSelection.parse(json, new com.google.gson.Gson());
        assertEquals(LandmarkType.BANK, s.order().get(0));
        assertEquals(LandmarkType.ALTAR, s.order().get(1));
        // Position 2 onward = canonical enum order minus BANK/ALTAR.
        int i = 2;
        for (LandmarkType t : LandmarkType.values())
        {
            if (t == LandmarkType.BANK || t == LandmarkType.ALTAR) continue;
            assertEquals(t, s.order().get(i++));
        }
    }

    @Test
    public void parse_duplicateEnumInOrder_firstWins()
    {
        String json = "{\"order\":[\"BANK\",\"ALTAR\",\"BANK\"],\"selected\":[]}";
        LandmarkSelection s = LandmarkSelection.parse(json, new com.google.gson.Gson());
        // Duplicate BANK is dropped; ALTAR stays at index 1.
        assertEquals(LandmarkType.BANK, s.order().get(0));
        assertEquals(LandmarkType.ALTAR, s.order().get(1));
        assertEquals(LandmarkType.values().length, s.order().size());
    }

    @Test
    public void toJson_roundTripsThroughParse()
    {
        com.google.gson.Gson gson = new com.google.gson.Gson();
        LandmarkSelection original = LandmarkSelection.canonicalDefault();
        String json = original.toJson(gson);
        LandmarkSelection round = LandmarkSelection.parse(json, gson);
        assertEquals(original.order(), round.order());
        for (LandmarkType t : LandmarkType.values())
        {
            assertEquals("selected[" + t + "]", original.isSelected(t), round.isSelected(t));
        }
    }
}
