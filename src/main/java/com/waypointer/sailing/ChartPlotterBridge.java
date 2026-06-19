package com.waypointer.sailing;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/** Bridge to the Chart Plotter plugin's inbound PluginMessage API (namespace "chartplotter",
 *  name "chart" data {x,y}; name "clear"). Availability is detected by class simple name via
 *  PluginManager (works across plugin-hub classloaders; not java.lang.reflect). */
@Slf4j
@Singleton
public class ChartPlotterBridge
{
    private static final String NAMESPACE = "chartplotter";
    private static final String MSG_CHART = "chart";
    private static final String MSG_CLEAR = "clear";
    private static final String PLUGIN_CLASS = "ChartPlotterPlugin";

    private final EventBus eventBus;
    private final ClientThread clientThread;
    private final PluginManager pluginManager;

    @Inject
    public ChartPlotterBridge(EventBus eventBus, ClientThread clientThread, PluginManager pluginManager)
    {
        this.eventBus = eventBus;
        this.clientThread = clientThread;
        this.pluginManager = pluginManager;
    }

    public boolean isAvailable()
    {
        for (Plugin p : pluginManager.getPlugins())
        {
            if (PLUGIN_CLASS.equals(p.getClass().getSimpleName()) && pluginManager.isPluginActive(p))
            {
                return true;
            }
        }
        return false;
    }

    public void chart(int x, int y)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        // Chart Plotter's handler reads client state, so post from the client thread.
        clientThread.invoke(() -> {
            eventBus.post(new PluginMessage(NAMESPACE, MSG_CHART, data));
            return true;
        });
    }

    public void clear()
    {
        clientThread.invoke(() -> {
            eventBus.post(new PluginMessage(NAMESPACE, MSG_CLEAR, new HashMap<>()));
            return true;
        });
    }
}
