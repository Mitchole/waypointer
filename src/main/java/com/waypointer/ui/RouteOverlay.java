package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.model.route.StepType;
import com.waypointer.service.PlayerPosition;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.util.Bearing;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Read-only in-game step box. The player never clicks it; advancement is via hotkey / sidebar. */
@Singleton
public class RouteOverlay extends OverlayPanel
{
    private final RoutePlaybackEngine engine;
    private final WaypointerConfig config;
    private final PlayerPosition position;

    @Inject
    public RouteOverlay(RoutePlaybackEngine engine, WaypointerConfig config, PlayerPosition position)
    {
        this.engine = engine;
        this.position = position;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showRouteOverlay()) return null;
        Route r = engine.getActiveRoute();
        if (r == null) return null;

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(TitleComponent.builder()
            .text(r.getName())
            .build());

        for (String line : buildLines(r, engine.getCurrentIndex(), engine.getLap(), bearing()))
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left(line)
                .leftColor(line.startsWith("Next:") || line.startsWith("Step ")
                    ? Color.LIGHT_GRAY : Color.WHITE)
                .build());
        }
        return super.render(graphics);
    }

    /** Distance and direction to the current waypoint step while aboard, else null. Render runs on
     *  the client thread, so the client reads are safe. */
    @Nullable
    private String bearing()
    {
        RouteStep step = engine.getCurrentStep();
        if (step == null || step.getType() != StepType.WAYPOINT
            || step.getPackedWorldPoint() == WorldPointPacker.UNDEFINED) return null;
        if (!position.isAboard()) return null;
        WorldPoint here = position.current();
        return here == null ? null : Bearing.describe(here, step.getPackedWorldPoint());
    }

    /**
     * Pure display-line builder (testable without a Graphics2D). Returns: a step counter line,
     * the current step's text, the bearing line when given, and a dim "Next:" preview when a following step exists.
     */
    static List<String> buildLines(Route route, int currentIndex, int lap, @Nullable String bearing)
    {
        List<String> lines = new ArrayList<>();
        int total = route.getSteps().size();
        String counter = "Step " + (currentIndex + 1) + " / " + total;
        if (route.isRepeating()) counter += "  -  Lap " + lap;
        lines.add(counter);

        if (currentIndex >= 0 && currentIndex < total)
        {
            lines.add(route.getSteps().get(currentIndex).boxTextOrLabel());
            if (bearing != null) lines.add(bearing);
        }
        int next = currentIndex + 1;
        if (next < total)
        {
            RouteStep n = route.getSteps().get(next);
            lines.add("Next: " + n.boxTextOrLabel());
        }
        return lines;
    }
}
