package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.service.RoutePlaybackEngine;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
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

    @Inject
    public RouteOverlay(RoutePlaybackEngine engine, WaypointerConfig config)
    {
        this.engine = engine;
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

        for (String line : buildLines(r, engine.getCurrentIndex(), engine.getLap()))
        {
            panelComponent.getChildren().add(LineComponent.builder()
                .left(line)
                .leftColor(line.startsWith("Next:") || line.startsWith("Step ")
                    ? Color.LIGHT_GRAY : Color.WHITE)
                .build());
        }
        return super.render(graphics);
    }

    /**
     * Pure display-line builder (testable without a Graphics2D). Returns: a step counter line,
     * the current step's text, and a dim "Next:" preview when a following step exists.
     */
    static List<String> buildLines(Route route, int currentIndex, int lap)
    {
        List<String> lines = new ArrayList<>();
        int total = route.getSteps().size();
        String counter = "Step " + (currentIndex + 1) + " / " + total;
        if (route.isRepeating()) counter += "  -  Lap " + lap;
        lines.add(counter);

        if (currentIndex >= 0 && currentIndex < total)
        {
            lines.add(route.getSteps().get(currentIndex).getLabel());
        }
        int next = currentIndex + 1;
        if (next < total)
        {
            RouteStep n = route.getSteps().get(next);
            lines.add("Next: " + n.getLabel());
        }
        return lines;
    }
}
