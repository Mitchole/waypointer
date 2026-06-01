package com.waypointer;

import com.google.inject.Provides;
import com.waypointer.service.DeathAutoPathfinder;
import com.waypointer.service.LandmarkOverrides;
import com.waypointer.service.PresetOverrides;
import com.waypointer.service.WaypointMenuHandler;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.ProfileLibrarySwitcher;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.ui.AreaPreviewOverlay;
import com.waypointer.ui.Icon;
import com.waypointer.ui.NpcHighlightOverlay;
import com.waypointer.ui.TabHost;
import com.waypointer.ui.WaypointerPanel;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

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
    @Inject private ConfigManager configManager;
    @Inject private ProfileLibrarySwitcher profileLibrarySwitcher;
    @Inject private ScheduledExecutorService scheduler;
    @Inject private EventBus eventBus;
    @Inject private WaypointMenuHandler menuHandler;
    @Inject private WaypointPathfinder pathfinderService;
    @Inject private DeathAutoPathfinder deathAutoPathfinder;
    @Inject private TabHost tabHost;
    @Inject private LandmarkOverrides landmarkOverrides;
    @Inject private PresetOverrides presetOverrides;
    @Inject private AreaPreviewOverlay areaPreviewOverlay;
    @Inject private NpcHighlightOverlay npcHighlightOverlay;
    @Inject private OverlayManager overlayManager;
    @Inject private com.waypointer.service.RouteStore routeStore;
    @Inject private com.waypointer.service.RouteStorePersistence routePersistence;
    @Inject private com.waypointer.service.RoutePlaybackEngine routePlaybackEngine;
    @Inject private com.waypointer.ui.RouteOverlay routeOverlay;
    @Inject private net.runelite.api.Client client;
    @Inject private net.runelite.client.input.KeyManager keyManager;

    private NavigationButton navButton;
    private Thread shutdownHook;
    private net.runelite.client.input.KeyListener routeHotkeyListener;

    @Override
    protected void startUp() throws Exception
    {
        store.enableDebouncedPersistence(persistence, scheduler, Duration.ofMillis(500));
        routeStore.enableDebouncedPersistence(routePersistence, scheduler, Duration.ofMillis(500));
        routeStore.bootstrap(routePersistence.loadOrEmpty());
        routePlaybackEngine.attach(client);
        eventBus.register(routePlaybackEngine);
        overlayManager.add(routeOverlay);

        routeHotkeyListener = new net.runelite.client.input.KeyListener()
        {
            @Override public void keyTyped(java.awt.event.KeyEvent e) {}
            @Override public void keyReleased(java.awt.event.KeyEvent e) {}
            @Override public void keyPressed(java.awt.event.KeyEvent e)
            {
                if (!routePlaybackEngine.isActive()) return;
                if (config.routeAdvanceHotkey().matches(e))
                {
                    routePlaybackEngine.advance();
                    e.consume();
                }
            }
        };
        keyManager.registerKeyListener(routeHotkeyListener);

        profileLibrarySwitcher.initialize(
            profileLibrarySwitcher.resolveStartupKey(configManager.getRSProfileKey()));

        // Flush any pending debounced save if the JVM terminates before shutDown() runs (e.g.
        // a hard client crash or user closing the RuneLite window). Removed in shutDown() so
        // we don't leak a hook reference across plugin enable/disable cycles.
        shutdownHook = new Thread(() -> {
            try { store.flushPendingSave(); }
            catch (Exception e) { log.warn("Shutdown-hook save failed", e); }
        }, "waypointer-shutdown-flush");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        BufferedImage icon = Icon.getSize32();
        navButton = NavigationButton.builder()
            .tooltip("Waypointer")
            .icon(icon)
            .priority(7)
            .panel(tabHost)
            .build();
        clientToolbar.addNavigation(navButton);
        eventBus.register(menuHandler);
        eventBus.register(pathfinderService);
        eventBus.register(deathAutoPathfinder);
        eventBus.register(panel);
        eventBus.register(tabHost);

        // Pick up RuneLiteLAF's scrollbar colors now that the LAF is installed (the panel
        // was constructed earlier, against Metal's UIDefaults).
        tabHost.refreshScrollbarStyling();

        if (config.devModeEnabled()) overlayManager.add(areaPreviewOverlay);
        overlayManager.add(npcHighlightOverlay);

        log.info("Waypointer started: {} waypoints loaded",
            store.getLibrary().getWaypoints().size());
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (routeHotkeyListener != null)
        {
            keyManager.unregisterKeyListener(routeHotkeyListener);
            routeHotkeyListener = null;
        }
        eventBus.unregister(routePlaybackEngine);
        routePlaybackEngine.detach();
        overlayManager.remove(routeOverlay);
        routeStore.disableDebouncedPersistence();
        routeStore.flushPendingSave();

        store.disableDebouncedPersistence();
        if (shutdownHook != null)
        {
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); }
            catch (Exception ignored) {}
            shutdownHook = null;
        }

        eventBus.unregister(menuHandler);
        eventBus.unregister(pathfinderService);
        eventBus.unregister(deathAutoPathfinder);
        eventBus.unregister(panel);
        eventBus.unregister(tabHost);

        clientToolbar.removeNavigation(navButton);
        tabHost.dispose();
        store.flushPendingSave();
        overlayManager.remove(areaPreviewOverlay);
        overlayManager.remove(npcHighlightOverlay);
        landmarkOverrides.flushBlocking();
        presetOverrides.flushBlocking();
        log.info("Waypointer stopped");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (!"waypointer".equals(e.getGroup())) return;
        if ("devModeEnabled".equals(e.getKey()))
        {
            // remove() first so the toggle path is symmetric and self-evidently single-copy,
            // regardless of whether startUp() already added it. (OverlayManager.add is itself
            // idempotent -- it ignores an overlay it already holds -- so this is belt-and-braces.)
            overlayManager.remove(areaPreviewOverlay);
            if (config.devModeEnabled()) overlayManager.add(areaPreviewOverlay);
        }
    }

    @Subscribe
    public void onRuneScapeProfileChanged(net.runelite.client.events.RuneScapeProfileChanged e)
    {
        // getRSProfileKey() is updated before this event is posted and is the same source startUp
        // uses, so reading it here keeps the key format consistent. Marshal the swap (file I/O +
        // panel rebuild) to the EDT.
        final String key = configManager.getRSProfileKey();
        javax.swing.SwingUtilities.invokeLater(() -> profileLibrarySwitcher.switchToProfile(key));
    }

    @Provides
    WaypointerConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(WaypointerConfig.class);
    }
}
