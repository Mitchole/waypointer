package com.waypointer.ui;

import com.waypointer.service.LandmarkType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Ordered selection of landmark types for the in-panel landmark bar. Immutable; mutators
 * return new instances. {@link #order()} is the full 11-type picker-list order; the bar
 * shows the types for which {@link #isSelected(LandmarkType)} returns true, in {@link #order()} sequence.
 */
public final class LandmarkSelection
{
    /** Default selected set: BANK, ALTAR, SPIRIT_TREE, FAIRY_RING. */
    static final List<LandmarkType> DEFAULTS = Collections.unmodifiableList(Arrays.asList(
        LandmarkType.BANK,
        LandmarkType.ALTAR,
        LandmarkType.SPIRIT_TREE,
        LandmarkType.FAIRY_RING));

    private final List<LandmarkType> order;
    private final Set<LandmarkType> selected;

    private LandmarkSelection(List<LandmarkType> order, EnumSet<LandmarkType> selected)
    {
        this.order = Collections.unmodifiableList(new ArrayList<>(order));
        EnumSet<LandmarkType> copy = EnumSet.noneOf(LandmarkType.class);
        copy.addAll(selected);
        this.selected = Collections.unmodifiableSet(copy);
    }

    public static LandmarkSelection canonicalDefault()
    {
        List<LandmarkType> order = new ArrayList<>(DEFAULTS);
        for (LandmarkType t : LandmarkType.values())
        {
            if (!order.contains(t)) order.add(t);
        }
        EnumSet<LandmarkType> selected = EnumSet.copyOf(DEFAULTS);
        return new LandmarkSelection(order, selected);
    }

    public List<LandmarkType> order() { return order; }

    public boolean isSelected(LandmarkType t) { return selected.contains(t); }
}
