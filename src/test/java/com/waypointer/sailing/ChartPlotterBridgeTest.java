package com.waypointer.sailing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.function.BooleanSupplier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChartPlotterBridgeTest
{
    // Simple name must be exactly "ChartPlotterPlugin" for the availability check.
    public static class ChartPlotterPlugin extends Plugin {}

    @Mock private EventBus eventBus;
    @Mock private ClientThread clientThread;
    @Mock private PluginManager pluginManager;
    private ChartPlotterBridge bridge;

    @Before
    public void setUp()
    {
        bridge = new ChartPlotterBridge(eventBus, clientThread, pluginManager);
        // Run any client-thread supplier inline.
        doAnswer(inv -> { ((BooleanSupplier) inv.getArgument(0)).getAsBoolean(); return null; })
            .when(clientThread).invoke(any(BooleanSupplier.class));
    }

    @Test
    public void availableWhenChartPlotterActive()
    {
        Plugin cp = new ChartPlotterPlugin();
        when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(cp));
        when(pluginManager.isPluginActive(cp)).thenReturn(true);
        assertTrue(bridge.isAvailable());
    }

    @Test
    public void notAvailableWhenAbsent()
    {
        when(pluginManager.getPlugins()).thenReturn(Collections.emptyList());
        assertFalse(bridge.isAvailable());
    }

    @Test
    public void chartPostsPluginMessageWithCoords()
    {
        bridge.chart(2151, 3880);
        verify(eventBus).post(org.mockito.ArgumentMatchers.argThat((PluginMessage m) ->
            "chartplotter".equals(m.getNamespace())
                && "chart".equals(m.getName())
                && Integer.valueOf(2151).equals(m.getData().get("x"))
                && Integer.valueOf(3880).equals(m.getData().get("y"))));
    }

    @Test
    public void clearPostsClearMessage()
    {
        bridge.clear();
        verify(eventBus).post(org.mockito.ArgumentMatchers.argThat((PluginMessage m) ->
            "chartplotter".equals(m.getNamespace()) && "clear".equals(m.getName())));
    }
}
