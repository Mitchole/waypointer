package com.waypointer.ui;

import com.google.inject.ImplementedBy;
import com.waypointer.model.Waypoint;
import java.awt.Component;

@ImplementedBy(SwingWildernessConfirmGate.class)
public interface WildernessConfirmGate
{
    Result prompt(Component parent, Waypoint w);

    final class Result
    {
        public final boolean proceed;
        public final boolean dontAskAgain;

        public Result(boolean proceed, boolean dontAskAgain)
        {
            this.proceed = proceed;
            this.dontAskAgain = dontAskAgain;
        }
    }
}
