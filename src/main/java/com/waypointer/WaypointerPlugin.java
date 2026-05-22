package com.waypointer;

import com.google.inject.Provides;
import com.waypointer.service.WaypointMenuHandler;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.ui.PinIcon;
import com.waypointer.ui.WaypointerNavigator;
import com.waypointer.ui.WaypointerPanel;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.MultiplexingPluginPanel;

@Slf4j
@PluginDescriptor(
    name = "Waypointer",
    description = "Save in-game locations and one-click path to them via Shortest Path.",
    tags = {"map", "travel", "pathfinding", "waypoints"}
)
public class WaypointerPlugin extends Plugin
{
    @Inject private WaypointerConfig config;
    @Inject private ClientToolbar clientToolbar;
    @Inject private WaypointerPanel panel;
    @Inject private WaypointStore store;
    @Inject private WaypointStorePersistence persistence;
    @Inject private ScheduledExecutorService scheduler;
    @Inject private EventBus eventBus;
    @Inject private WaypointMenuHandler menuHandler;
    @Inject private WaypointPathfinder pathfinderService;
    @Inject private WaypointerNavigator navigator;

    private NavigationButton navButton;
    private Thread shutdownHook;

    @Override
    protected void startUp() throws Exception
    {
        store.bootstrap(persistence.loadOrEmpty());
        store.enableDebouncedPersistence(persistence, scheduler, Duration.ofMillis(500));

        // Flush any pending debounced save if the JVM terminates before shutDown() runs (e.g.
        // a hard client crash or user closing the RuneLite window). Removed in shutDown() so
        // we don't leak a hook reference across plugin enable/disable cycles.
        shutdownHook = new Thread(() -> {
            try { store.flushPendingSave(); }
            catch (Exception e) { log.warn("Shutdown-hook save failed", e); }
        }, "waypointer-shutdown-flush");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        MultiplexingPluginPanel muxer = new MultiplexingPluginPanel(panel);
        navigator.attach(muxer);

        BufferedImage icon = PinIcon.getSize32();
        navButton = NavigationButton.builder()
            .tooltip("Waypointer")
            .icon(icon)
            .priority(7)
            .panel(muxer)
            .build();
        clientToolbar.addNavigation(navButton);
        eventBus.register(menuHandler);
        eventBus.register(pathfinderService);

        // Pick up RuneLiteLAF's scrollbar colors now that the LAF is installed (the panel
        // was constructed earlier, against Metal's UIDefaults).
        panel.refreshScrollbarStyling();

        log.info("Waypointer started: {} waypoints loaded",
            store.getLibrary().getWaypoints().size());
    }

    @Override
    protected void shutDown() throws Exception
    {
        store.disableDebouncedPersistence();
        if (shutdownHook != null)
        {
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); }
            catch (Exception ignored) {}
            shutdownHook = null;
        }

        eventBus.unregister(menuHandler);
        eventBus.unregister(pathfinderService);

        clientToolbar.removeNavigation(navButton);
        navigator.detach();
        panel.dispose();
        store.flushPendingSave();
        log.info("Waypointer stopped");
    }

    @Provides
    WaypointerConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(WaypointerConfig.class);
    }
}
