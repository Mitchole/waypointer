package com.waypointer.ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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

    public static LandmarkSelection parse(String json, Gson gson)
    {
        if (json == null || json.isEmpty()) return canonicalDefault();

        JsonObject root;
        try
        {
            root = gson.fromJson(json, JsonObject.class);
        }
        catch (JsonSyntaxException e)
        {
            return canonicalDefault();
        }
        if (root == null) return canonicalDefault();

        List<LandmarkType> order = new ArrayList<>();
        if (root.has("order") && root.get("order").isJsonArray())
        {
            for (JsonElement el : root.getAsJsonArray("order"))
            {
                LandmarkType t = parseTypeOrNull(el.getAsString());
                if (t != null && !order.contains(t)) order.add(t);
            }
        }
        for (LandmarkType t : LandmarkType.values())
        {
            if (!order.contains(t)) order.add(t);
        }

        EnumSet<LandmarkType> selected = EnumSet.noneOf(LandmarkType.class);
        if (root.has("selected") && root.get("selected").isJsonArray())
        {
            for (JsonElement el : root.getAsJsonArray("selected"))
            {
                LandmarkType t = parseTypeOrNull(el.getAsString());
                if (t != null) selected.add(t);
            }
        }

        return new LandmarkSelection(order, selected);
    }

    public String toJson(Gson gson)
    {
        JsonObject root = new JsonObject();
        JsonArray orderArr = new JsonArray();
        for (LandmarkType t : order) orderArr.add(t.name());
        JsonArray selectedArr = new JsonArray();
        for (LandmarkType t : order) // iterate order so selected output is bar-order
        {
            if (selected.contains(t)) selectedArr.add(t.name());
        }
        root.add("order", orderArr);
        root.add("selected", selectedArr);
        return gson.toJson(root);
    }

    public List<LandmarkType> selectedInBarOrder()
    {
        List<LandmarkType> out = new ArrayList<>();
        for (LandmarkType t : order) if (selected.contains(t)) out.add(t);
        return out;
    }

    public LandmarkSelection withSelected(LandmarkType t, boolean include)
    {
        EnumSet<LandmarkType> next = EnumSet.noneOf(LandmarkType.class);
        next.addAll(selected);
        if (include) next.add(t); else next.remove(t);
        return new LandmarkSelection(order, next);
    }

    private static LandmarkType parseTypeOrNull(String name)
    {
        try { return LandmarkType.valueOf(name); }
        catch (IllegalArgumentException e) { return null; }
    }
}
