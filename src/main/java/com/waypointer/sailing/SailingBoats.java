/*
 * Copyright (c) 2025, AtchRBX <https://github.com/AtchRBX>
 * Copyright (c) 2025, Logical-sh <https://github.com/Logical-sh>
 * All rights reserved. Mirroring technique from "Dude, Where's My Boat?" under BSD 2-Clause.
 * THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND.
 */
package com.waypointer.sailing;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarbitID;

/** Reads the player's 5 sailing boat slots from varbits. Boat names are composed from the
 *  SailingBoatNameOptions DBTable (prefix + descriptor + noun), mirroring "Dude, Where's My
 *  Boat?" (BSD, ArchRBX/Logical-sh). Call on the client thread. */
@Singleton
public class SailingBoats
{
    private static final int[] OWNED = {
        VarbitID.SAILING_BOAT_1_OWNED, VarbitID.SAILING_BOAT_2_OWNED, VarbitID.SAILING_BOAT_3_OWNED,
        VarbitID.SAILING_BOAT_4_OWNED, VarbitID.SAILING_BOAT_5_OWNED };
    private static final int[] PORT = {
        VarbitID.SAILING_BOAT_1_PORT, VarbitID.SAILING_BOAT_2_PORT, VarbitID.SAILING_BOAT_3_PORT,
        VarbitID.SAILING_BOAT_4_PORT, VarbitID.SAILING_BOAT_5_PORT };
    private static final int[] NAME1 = {
        VarbitID.SAILING_BOAT_1_NAME_1, VarbitID.SAILING_BOAT_2_NAME_1, VarbitID.SAILING_BOAT_3_NAME_1,
        VarbitID.SAILING_BOAT_4_NAME_1, VarbitID.SAILING_BOAT_5_NAME_1 };
    private static final int[] NAME2 = {
        VarbitID.SAILING_BOAT_1_NAME_2, VarbitID.SAILING_BOAT_2_NAME_2, VarbitID.SAILING_BOAT_3_NAME_2,
        VarbitID.SAILING_BOAT_4_NAME_2, VarbitID.SAILING_BOAT_5_NAME_2 };
    private static final int[] NAME3 = {
        VarbitID.SAILING_BOAT_1_NAME_3, VarbitID.SAILING_BOAT_2_NAME_3, VarbitID.SAILING_BOAT_3_NAME_3,
        VarbitID.SAILING_BOAT_4_NAME_3, VarbitID.SAILING_BOAT_5_NAME_3 };

    private final Client client;

    @Inject
    public SailingBoats(Client client)
    {
        this.client = client;
    }

    public boolean isBoarded()
    {
        return client.getVarbitValue(VarbitID.SAILING_BOARDED_BOAT) == 1;
    }

    public List<BoatInfo> mooredBoats()
    {
        List<BoatInfo> out = new ArrayList<>();
        for (int slot = 1; slot <= 5; slot++)
        {
            BoatInfo b = boat(slot);
            if (b != null) out.add(b);
        }
        return out;
    }

    public BoatInfo boat(int slot)
    {
        if (slot < 1 || slot > 5 || !isOwned(slot)) return null;
        SailingDock dock = SailingDock.forId(client.getVarbitValue(PORT[slot - 1]));
        if (dock == null) return null; // bottled / capsized / at sea / unknown
        return new BoatInfo(slot, boatName(slot), dock);
    }

    private boolean isOwned(int slot)
    {
        int i = slot - 1;
        // The OWNED varbit is unreliable; treat the slot as occupied if any signal is set.
        return client.getVarbitValue(OWNED[i]) > 0
            || client.getVarbitValue(PORT[i]) > 0
            || client.getVarbitValue(NAME2[i]) > 0;
    }

    public String boatName(int slot)
    {
        int i = slot - 1;
        int r1 = client.getVarbitValue(NAME1[i]);
        int r2 = client.getVarbitValue(NAME2[i]);
        int r3 = client.getVarbitValue(NAME3[i]);
        if (r1 + r2 + r3 == 0) return "Boat " + slot;
        StringBuilder sb = new StringBuilder();
        appendOption(sb, DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_PREFIX_OPTIONS, r1 - 1);
        appendOption(sb, DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_DESCRIPTOR_OPTIONS, r2 - 1);
        appendOption(sb, DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_NOUN_OPTIONS, r3 - 1);
        String s = sb.toString().trim();
        return s.isEmpty() ? "Boat " + slot : s;
    }

    private void appendOption(StringBuilder sb, int row, int index)
    {
        if (index <= 0) return;
        Object[] opts = client.getDBTableField(row, DBTableID.SailingBoatNameOptions.COL_OPTION, 0);
        if (opts == null || index >= opts.length) return;
        Object o = opts[index];
        if (o instanceof String && !((String) o).isEmpty())
        {
            if (sb.length() > 0) sb.append(' ');
            sb.append((String) o);
        }
    }

    public static final class BoatInfo
    {
        public final int slot;
        public final String name;
        public final SailingDock dock;

        public BoatInfo(int slot, String name, SailingDock dock)
        {
            this.slot = slot;
            this.name = name;
            this.dock = dock;
        }
    }
}
