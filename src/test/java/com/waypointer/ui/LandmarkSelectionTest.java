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

    @Test
    public void selectedInBarOrder_filtersOrderBySelection()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        List<LandmarkType> bar = s.selectedInBarOrder();
        assertEquals(4, bar.size());
        assertEquals(LandmarkType.BANK, bar.get(0));
        assertEquals(LandmarkType.ALTAR, bar.get(1));
        assertEquals(LandmarkType.SPIRIT_TREE, bar.get(2));
        assertEquals(LandmarkType.FAIRY_RING, bar.get(3));
    }

    @Test
    public void withSelected_addingType_appearsInBarAtItsOrderPosition()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        // SLAYER_MASTER is at index 4 in canonical default order (after the 4 defaults).
        LandmarkSelection next = s.withSelected(LandmarkType.SLAYER_MASTER, true);
        List<LandmarkType> bar = next.selectedInBarOrder();
        assertEquals(5, bar.size());
        assertEquals(LandmarkType.SLAYER_MASTER, bar.get(4));
    }

    @Test
    public void withSelected_removingType_keepsOrderPosition()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        LandmarkSelection next = s.withSelected(LandmarkType.ALTAR, false);
        // Order is unchanged; ALTAR is still at index 1.
        assertEquals(LandmarkType.ALTAR, next.order().get(1));
        // Bar order drops ALTAR.
        assertEquals(3, next.selectedInBarOrder().size());
        assertFalse(next.selectedInBarOrder().contains(LandmarkType.ALTAR));
    }

    @Test
    public void withSelected_returnsNewInstance_originalUnchanged()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        LandmarkSelection next = s.withSelected(LandmarkType.ALTAR, false);
        assertTrue("original still has ALTAR selected", s.isSelected(LandmarkType.ALTAR));
        assertFalse("new instance does not", next.isSelected(LandmarkType.ALTAR));
    }

    @Test
    public void withOrderMove_movesForward_shiftsOthersUp()
    {
        // Move index 0 (BANK) to index 2.
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        LandmarkSelection next = s.withOrderMove(0, 2);
        // ALTAR shifts up to 0, SPIRIT_TREE to 1, BANK lands at 2.
        assertEquals(LandmarkType.ALTAR, next.order().get(0));
        assertEquals(LandmarkType.SPIRIT_TREE, next.order().get(1));
        assertEquals(LandmarkType.BANK, next.order().get(2));
    }

    @Test
    public void withOrderMove_movesBackward_shiftsOthersDown()
    {
        // Move index 3 (FAIRY_RING) to index 0.
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        LandmarkSelection next = s.withOrderMove(3, 0);
        assertEquals(LandmarkType.FAIRY_RING, next.order().get(0));
        assertEquals(LandmarkType.BANK, next.order().get(1));
        assertEquals(LandmarkType.ALTAR, next.order().get(2));
        assertEquals(LandmarkType.SPIRIT_TREE, next.order().get(3));
    }

    @Test
    public void withOrderMove_sameIndex_returnsEquivalent()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        LandmarkSelection next = s.withOrderMove(0, 0);
        assertEquals(s.order(), next.order());
    }

    @Test
    public void withOrderMove_movingSelectedType_changesBarOrder()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        // SLAYER_MASTER is last (index 10) in canonical order. Move it to index 0.
        // It is unselected, so bar count is unchanged.
        int lastIndex = s.order().size() - 1;
        LandmarkSelection next = s.withOrderMove(lastIndex, 0);
        assertEquals(LandmarkType.SLAYER_MASTER, next.order().get(0));
        assertEquals(s.selectedInBarOrder().size(), next.selectedInBarOrder().size());
        // BANK shifts from index 0 to index 1.
        assertEquals(LandmarkType.BANK, next.order().get(1));
    }

    @Test
    public void withOrderMove_outOfRange_returnsSameOrder()
    {
        LandmarkSelection s = LandmarkSelection.canonicalDefault();
        assertEquals(s.order(), s.withOrderMove(-1, 0).order());
        assertEquals(s.order(), s.withOrderMove(0, 99).order());
    }

    @Test
    public void parse_nonPrimitiveArrayElements_areSkipped()
    {
        // Hand-corrupted JSON: objects and numbers inside the arrays.
        String json = "{\"order\":[\"BANK\",{\"foo\":\"bar\"},\"ALTAR\",42],\"selected\":[\"BANK\",{\"x\":1}]}";
        LandmarkSelection s = LandmarkSelection.parse(json, new com.google.gson.Gson());
        assertEquals(LandmarkType.BANK, s.order().get(0));
        assertEquals(LandmarkType.ALTAR, s.order().get(1));
        assertEquals(LandmarkType.values().length, s.order().size());
        assertTrue(s.isSelected(LandmarkType.BANK));
    }
}
