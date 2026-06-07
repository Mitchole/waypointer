package com.waypointer;

import com.google.inject.Provides;
import com.waypointer.service.DeathAutoPathfinder;
import com.waypointer.service.WaypointMenuHandler;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.ui.Icon;
import com.waypointer.ui.NpcHighlightOverlay;
import com.waypointer.ui.TabHost;
import com.waypointer.ui.WaypointerPanel;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
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
    @Inject private EventBus eventBus;
    @Inject private WaypointMenuHandler menuHandler;
    @Inject private WaypointPathfinder pathfinderService;
    @Inject private DeathAutoPathfinder deathAutoPathfinder;
    @Inject private TabHost tabHost;
    @Inject private NpcHighlightOverlay npcHighlightOverlay;
    @Inject private OverlayManager overlayManager;
    @Inject private com.waypointer.service.RouteStore routeStore;
    @Inject private com.waypointer.service.RouteStorePersistence routePersistence;
    @Inject private com.waypointer.service.RoutePlaybackEngine routePlaybackEngine;
    @Inject private com.waypointer.ui.RouteOverlay routeOverlay;
    @Inject private net.runelite.api.Client client;
    @Inject private net.runelite.client.input.KeyManager keyManager;

    private NavigationButton navButton;
    private net.runelite.client.input.KeyListener routeHotkeyListener;

    @Override
    protected void startUp() throws Exception
    {
        // Load from config first so the panel (subscribed at construction) renders the saved data,
        // then attach the write-through saver so the bootstrap itself does not re-save.
        store.bootstrap(persistence.load());
        store.enablePersistence(() -> persistence.save(store.getLibrary()));
        routeStore.bootstrap(routePersistence.load());
        routeStore.enablePersistence(() -> routePersistence.save(routeStore.getLibrary()));
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

        BufferedImage icon = Icon.getSize32();
        navButton = NavigationButton.builder()
            .tooltip("Waypointer")
            .icon(icon)
            .priority(7)
            .panel(tabHost)
            .build();
        tabHost.setNavButton(navButton);
        clientToolbar.addNavigation(navButton);
        eventBus.register(menuHandler);
        eventBus.register(pathfinderService);
        eventBus.register(deathAutoPathfinder);
        eventBus.register(panel);
        eventBus.register(tabHost);

        // Pick up RuneLiteLAF's scrollbar colors now that the LAF is installed (the panel
        // was constructed earlier, against Metal's UIDefaults).
        tabHost.refreshScrollbarStyling();

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
        routeStore.disablePersistence();
        store.disablePersistence();

        eventBus.unregister(menuHandler);
        eventBus.unregister(pathfinderService);
        eventBus.unregister(deathAutoPathfinder);
        eventBus.unregister(panel);
        eventBus.unregister(tabHost);

        clientToolbar.removeNavigation(navButton);
        tabHost.dispose();
        overlayManager.remove(npcHighlightOverlay);
        log.info("Waypointer stopped");
    }

    @Subscribe
    public void onRuneScapeProfileChanged(net.runelite.client.events.RuneScapeProfileChanged e)
    {
        // ConfigManager has already switched the active RS profile by the time this fires, so a
        // fresh load() reads the newly-logged-in account's library. The write-through saver stays
        // attached and targets the now-current profile. Marshal the swap to the EDT.
        javax.swing.SwingUtilities.invokeLater(() -> store.bootstrap(persistence.load()));
    }

    @Provides
    WaypointerConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(WaypointerConfig.class);
    }
}
