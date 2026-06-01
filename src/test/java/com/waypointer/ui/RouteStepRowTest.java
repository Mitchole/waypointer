package com.waypointer.ui;

import static org.junit.Assert.assertNotEquals;
import com.waypointer.model.route.StepType;
import org.junit.Test;

public class RouteStepRowTest
{
    @Test
    public void waypointGlyphDiffersFromManual()
    {
        String wp = RouteStepRow.stepGlyph(StepType.WAYPOINT);
        String man = RouteStepRow.stepGlyph(StepType.MANUAL);
        assertNotEquals(wp, man);
    }
}
