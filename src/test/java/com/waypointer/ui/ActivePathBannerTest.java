package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointPathfinder;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActivePathBannerTest
{
    private WaypointPathfinder pathfinder;
    private WaypointerConfig config;
    private ActivePathBanner banner;

    @Before
    public void setUp()
    {
        pathfinder = mock(WaypointPathfinder.class);
        config = mock(WaypointerConfig.class);
        when(config.showPathingBanner()).thenReturn(true);
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.UNDEFINED);
        banner = new ActivePathBanner(pathfinder, config);
    }

    @Test
    public void hiddenWhenNoActiveTarget()
    {
        banner.refresh();
        assertFalse(banner.isVisible());
    }

    @Test
    public void visibleAndLabelledWhenPathActive()
    {
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.pack(3162, 3486, 0));
        when(pathfinder.getActiveName()).thenReturn("Vorkath");
        banner.refresh();
        assertTrue(banner.isVisible());
        assertTrue("label should mention the destination, got: " + banner.getLabelText(),
            banner.getLabelText().contains("Vorkath"));
    }

    @Test
    public void hiddenWhenSettingDisabledEvenWithActiveTarget()
    {
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.pack(3162, 3486, 0));
        when(pathfinder.getActiveName()).thenReturn("Vorkath");
        when(config.showPathingBanner()).thenReturn(false);
        banner.refresh();
        assertFalse(banner.isVisible());
    }

    @Test
    public void hidesAfterClearedTarget()
    {
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.pack(3162, 3486, 0));
        when(pathfinder.getActiveName()).thenReturn("Vorkath");
        banner.refresh();
        assertTrue(banner.isVisible());

        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.UNDEFINED);
        when(pathfinder.getActiveName()).thenReturn(null);
        banner.refresh();
        assertFalse(banner.isVisible());
    }

    @Test
    public void stopButtonClearsPath()
    {
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.pack(3162, 3486, 0));
        when(pathfinder.getActiveName()).thenReturn("Vorkath");
        banner.refresh();
        banner.clickStopForTest();
        verify(pathfinder).clearPath();
    }

    @Test
    public void escapesHtmlInWaypointName()
    {
        when(pathfinder.getActiveTarget()).thenReturn(WorldPointPacker.pack(3162, 3486, 0));
        when(pathfinder.getActiveName()).thenReturn("<b>cheeky</b>");
        banner.refresh();
        String html = banner.getLabelText();
        assertFalse("raw <b> should not appear in label HTML, got: " + html,
            html.contains("<b>cheeky</b>"));
        assertTrue("escaped marker should appear, got: " + html, html.contains("&lt;b&gt;"));
    }
}
