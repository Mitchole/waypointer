/*
 * Copyright (c) 2025, nucleon <https://github.com/nucleon>
 * Copyright (c) 2025, Cooper Morris <https://github.com/coopermor>
 * Copyright (c) 2025, AtchRBX <https://github.com/AtchRBX>
 * All rights reserved. Lifted from "Dude, Where's My Boat?" under BSD 2-Clause.
 * THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 */
package com.waypointer.sailing;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/** Sailing docks. The id equals the SAILING_BOAT_n_PORT varbit value and SailingDock
 *  DBTable COL_DOCK_ID. navigationLocation is a walkable tile at the dock (land-leg target). */
@Getter
public enum SailingDock
{
    PORT_SARIM(0, "Port Sarim", 1, new WorldPoint(3050, 3192, 0), 7115),
    THE_PANDEMONIUM(1, "The Pandemonium", 1, new WorldPoint(3069, 2986, 0), 7116),
    LANDS_END(2, "Land's End", 5, new WorldPoint(1506, 3402, 0), 7117),
    MUSA_POINT(3, "Musa Point", 10, new WorldPoint(2960, 3147, 0), 7118),
    HOSIDIUS(4, "Hosidius", 5, new WorldPoint(1726, 3452, 0), 7119),
    RIMMINGTON(5, "Rimmington", 18, new WorldPoint(2905, 3226, 0), 7120),
    CATHERBY(6, "Catherby", 20, new WorldPoint(2796, 3412, 0), 7121),
    PORT_PISCARILLIUS(7, "Port Piscarillius", 15, new WorldPoint(1845, 3687, 0), 7122),
    BRIMHAVEN(8, "Brimhaven", 25, new WorldPoint(2757, 3229, 0), 7123),
    ARDOUGNE(9, "Ardougne", 28, new WorldPoint(2671, 3265, 0), 7124),
    PORT_KHAZARD(10, "Port Khazard", 30, new WorldPoint(2685, 3161, 0), 7125),
    WITCHAVEN(11, "Witchaven", 34, new WorldPoint(2746, 3304, 0), 7126),
    ENTRANA(12, "Entrana", 36, new WorldPoint(2878, 3335, 0), 7127),
    CIVITAS_ILLA_FORTIS(13, "Civitas illa Fortis", 38, new WorldPoint(1774, 3141, 0), 7128),
    CORSAIR_COVE(14, "Corsair Cove", 40, new WorldPoint(2579, 2843, 0), 7129),
    CAIRN_ISLE(15, "Cairn Isle", 42, new WorldPoint(2749, 2951, 0), 7130),
    SUNSET_COAST(16, "Sunset Coast", 44, new WorldPoint(1511, 2975, 0), 7131),
    THE_SUMMER_SHORE(17, "The Summer Shore", 45, new WorldPoint(3174, 2367, 0), 7132),
    ALDARIN(18, "Aldarin", 46, new WorldPoint(1452, 2970, 0), 7133),
    RUINS_OF_UNKAH(19, "Ruins of Unkah", 48, new WorldPoint(3143, 2824, 0), 7134),
    VOID_KNIGHTS_OUTPOST(20, "Void Knights' Outpost", 50, new WorldPoint(2651, 2678, 0), 7135),
    PORT_ROBERTS(21, "Port Roberts", 50, new WorldPoint(1860, 3306, 0), 7136),
    RED_ROCK(22, "Red Rock", 52, new WorldPoint(2752, 2496, 0), 7137),
    RELLEKKA(23, "Rellekka", 62, new WorldPoint(2630, 3705, 0), 7138),
    ETCETERIA(24, "Etceteria", 65, new WorldPoint(2611, 3840, 0), 7139),
    PORT_TYRAS(25, "Port Tyras", 66, new WorldPoint(2144, 3120, 0), 7140),
    DEEPFIN_POINT(26, "Deepfin Point", 67, new WorldPoint(1923, 2758, 0), 7141),
    JATIZSO(27, "Jatizso", 68, new WorldPoint(2412, 3780, 0), 7142),
    NETIZNOT(28, "Netiznot", 68, new WorldPoint(2308, 3783, 0), 7143),
    PRIFDDINAS(29, "Prifddinas", 70, new WorldPoint(2158, 3324, 0), 7144),
    PISCATORIS(30, "Piscatoris", 75, new WorldPoint(2303, 3690, 0), 7145),
    LUNAR_ISLE(31, "Lunar Isle", 76, new WorldPoint(2151, 3880, 0), 7146),
    ISLE_OF_SOULS(32, "Isle of Souls", 55, new WorldPoint(2282, 2823, 0), 7147),
    WATERBIRTH_ISLAND(33, "Waterbirth Island", 74, new WorldPoint(2543, 3765, 0), 7148),
    WEISS(34, "Weiss", 80, new WorldPoint(2860, 3972, 0), 7149),
    DOGNOSE_ISLAND(35, "Dognose Island", 40, new WorldPoint(3061, 2639, 0), 7150),
    REMOTE_ISLAND(36, "Remote Island", 45, new WorldPoint(2971, 2603, 0), 7151),
    THE_LITTLE_PEARL(37, "The Little Pearl", 45, new WorldPoint(3354, 2216, 0), 7152),
    THE_ONYX_CREST(38, "The Onyx Crest", 47, new WorldPoint(2997, 2288, 0), 7153),
    LAST_LIGHT(39, "Last Light", 52, new WorldPoint(2850, 2331, 0), 7154),
    CHARRED_ISLAND(40, "Charred Island", 60, new WorldPoint(2660, 2395, 0), 7155),
    VATRACHOS_ISLAND(41, "Vatrachos Island", 46, new WorldPoint(1872, 2985, 0), 7156),
    ANGLERS_RETREAT(42, "Anglers' Retreat", 51, new WorldPoint(2467, 2721, 0), 7157),
    MINOTAURS_REST(43, "Minotaurs' Rest", 54, new WorldPoint(1958, 3117, 0), 7158),
    ISLE_OF_BONES(44, "Isle of Bones", 56, new WorldPoint(2532, 2531, 0), 7159),
    TEAR_OF_THE_SOUL(45, "Tear of the Soul", 61, new WorldPoint(2318, 2774, 0), 7160),
    WINTUMBER_ISLAND(46, "Wintumber Island", 63, new WorldPoint(2058, 2606, 0), 7161),
    THE_CROWN_JEWEL(47, "The Crown Jewel", 64, new WorldPoint(1765, 2659, 0), 7162),
    RAINBOWS_END(48, "Rainbow's End", 69, new WorldPoint(2344, 2270, 0), 7163),
    SUNBLEAK_ISLAND(49, "Sunbleak Island", 72, new WorldPoint(2189, 2327, 0), 7164),
    SHIMMERING_ATOLL(50, "Shimmering Atoll", 49, new WorldPoint(1557, 2771, 0), 7165),
    LAGUNA_AURORAE(51, "Laguna Aurorae", 58, new WorldPoint(1202, 2733, 0), 7166),
    CHINCHOMPA_ISLAND(52, "Chinchompa Island", 42, new WorldPoint(1892, 3429, 0), 7167),
    LLEDRITH_ISLAND(53, "Lledrith Island", 66, new WorldPoint(2097, 3188, 0), 7168),
    YNYSDAIL(54, "Ynysdail", 73, new WorldPoint(2222, 3466, 0), 7169),
    BUCCANEERS_HAVEN(55, "Buccaneers' Haven", 76, new WorldPoint(2080, 3690, 0), 7170),
    DRUMSTICK_ISLE(56, "Drumstick Isle", 79, new WorldPoint(2150, 3530, 0), 7171),
    BRITTLE_ISLE(57, "Brittle Isle", 81, new WorldPoint(1954, 4056, 0), 7172),
    GRIMSTONE(58, "Grimstone", 87, new WorldPoint(2927, 4056, 0), 7173),
    ;

    private final int id;
    private final String dockName;
    private final int sailingLevel;
    private final WorldPoint navigationLocation;
    private final int spriteId;

    SailingDock(int id, String dockName, int sailingLevel, WorldPoint navigationLocation, int spriteId)
    {
        this.id = id;
        this.dockName = dockName;
        this.sailingLevel = sailingLevel;
        this.navigationLocation = navigationLocation;
        this.spriteId = spriteId;
    }

    public static SailingDock forId(int id)
    {
        for (SailingDock d : values())
        {
            if (d.id == id) return d;
        }
        return null;
    }
}
