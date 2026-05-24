package com.waypointer.service;

import java.util.Objects;

public final class LookupHit
{
    public enum Tier { CURATED, POI, SUB_AREA, CITY }

    private final String name;
    private final Tier tier;

    public LookupHit(String name, Tier tier)
    {
        this.name = name;
        this.tier = tier;
    }

    public String getName() { return name; }
    public Tier getTier() { return tier; }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof LookupHit)) return false;
        LookupHit h = (LookupHit) o;
        return Objects.equals(name, h.name) && tier == h.tier;
    }

    @Override
    public int hashCode() { return Objects.hash(name, tier); }

    @Override
    public String toString() { return name + " (" + tier + ")"; }
}
