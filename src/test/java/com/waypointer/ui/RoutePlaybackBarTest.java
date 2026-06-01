package com.waypointer.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.waypointer.model.route.Route;
import com.waypointer.model.route.RouteStep;
import com.waypointer.service.RoutePlaybackEngine;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RoutePlaybackBarTest
{
    @Mock private RoutePlaybackEngine engine;

    private Route route()
    {
        return new Route(UUID.randomUUID(), "R",
            Arrays.asList(RouteStep.manual("a"), RouteStep.manual("b")), false, Instant.now(), 0);
    }

    @Test
    public void hiddenWhenNoActiveRoute()
    {
        when(engine.isActive()).thenReturn(false);
        RoutePlaybackBar bar = new RoutePlaybackBar(engine);
        bar.refresh();
        assertFalse(bar.isVisible());
    }

    @Test
    public void visibleAndLabelledWhenActive()
    {
        when(engine.isActive()).thenReturn(true);
        when(engine.getActiveRoute()).thenReturn(route());
        when(engine.getCurrentIndex()).thenReturn(0);
        RoutePlaybackBar bar = new RoutePlaybackBar(engine);
        bar.refresh();
        assertTrue(bar.isVisible());
        assertTrue(bar.getLabelTextForTest().contains("Step 1 / 2"));
    }

    @Test
    public void nextButtonCallsAdvance()
    {
        when(engine.isActive()).thenReturn(true);
        when(engine.getActiveRoute()).thenReturn(route());
        RoutePlaybackBar bar = new RoutePlaybackBar(engine);
        bar.refresh();
        bar.clickNextForTest();
        verify(engine).advance();
    }

    @Test
    public void backButtonCallsBack()
    {
        when(engine.isActive()).thenReturn(true);
        when(engine.getActiveRoute()).thenReturn(route());
        RoutePlaybackBar bar = new RoutePlaybackBar(engine);
        bar.refresh();
        bar.clickBackForTest();
        verify(engine).back();
    }

    @Test
    public void stopButtonCallsStop()
    {
        when(engine.isActive()).thenReturn(true);
        when(engine.getActiveRoute()).thenReturn(route());
        RoutePlaybackBar bar = new RoutePlaybackBar(engine);
        bar.refresh();
        bar.clickStopForTest();
        verify(engine).stop();
    }
}
