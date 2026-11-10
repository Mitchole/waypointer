package com.waypointer.model.route;

import com.waypointer.model.WorldPointPacker;
import java.util.List;

/**
 * Straight-line distances along a route, in tiles. Only waypoint steps with a defined tile
 * count; manual steps are skipped. Plane is ignored.
 */
public final class RouteDistance
{
    private RouteDistance()
    {
    }

    private static boolean isStop(RouteStep s)
    {
        return s.getType() == StepType.WAYPOINT
            && s.getPackedWorldPoint() != WorldPointPacker.UNDEFINED;
    }

    /** Number of waypoint steps that have a tile. */
    public static int stops(Route route)
    {
        int n = 0;
        for (RouteStep s : route.getSteps()) if (isStop(s)) n++;
        return n;
    }

    /**
     * One entry per step: the distance from the previous waypoint step, or -1 for manual steps
     * and for the first waypoint step.
     */
    public static int[] legs(Route route)
    {
        List<RouteStep> steps = route.getSteps();
        int[] out = new int[steps.size()];
        int previous = WorldPointPacker.UNDEFINED;
        boolean havePrevious = false;
        for (int i = 0; i < out.length; i++)
        {
            RouteStep s = steps.get(i);
            if (!isStop(s))
            {
                out[i] = -1;
                continue;
            }
            int packed = s.getPackedWorldPoint();
            out[i] = havePrevious ? WorldPointPacker.chebyshev(previous, packed) : -1;
            previous = packed;
            havePrevious = true;
        }
        return out;
    }

    /** Sum of all legs, plus the closing leg back to the first stop when the route repeats. */
    public static int total(Route route)
    {
        int sum = 0;
        for (int leg : legs(route)) if (leg > 0) sum += leg;
        if (route.isRepeating())
        {
            int first = WorldPointPacker.UNDEFINED;
            int last = WorldPointPacker.UNDEFINED;
            int count = 0;
            for (RouteStep s : route.getSteps())
            {
                if (!isStop(s)) continue;
                if (count == 0) first = s.getPackedWorldPoint();
                last = s.getPackedWorldPoint();
                count++;
            }
            if (count >= 2) sum += WorldPointPacker.chebyshev(last, first);
        }
        return sum;
    }
}
