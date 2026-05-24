package com.waypointer.service;

/**
 * Categories of landmarks the bundled bbox dataset exposes for "nearest of type" queries.
 * One value per typed TSV under {@code /com/waypointer/landmarks/}; the untyped
 * {@code landmarks-bboxes.tsv} (unique landmarks) is intentionally not represented.
 * Bank chests fold into {@link #BANK}.
 */
public enum LandmarkType
{
    BANK("Bank"),
    ALTAR("Altar"),
    ANVIL("Anvil"),
    FURNACE("Furnace"),
    LOOM("Loom"),
    SPINNING_WHEEL("Spinning wheel"),
    TANNER("Tanner"),
    SPIRIT_TREE("Spirit tree"),
    CHARTER_SHIP("Charter ship"),
    FAIRY_RING("Fairy ring"),
    SLAYER_MASTER("Slayer master");

    private final String displayName;

    LandmarkType(String displayName)
    {
        this.displayName = displayName;
    }

    public String displayName()
    {
        return displayName;
    }
}
