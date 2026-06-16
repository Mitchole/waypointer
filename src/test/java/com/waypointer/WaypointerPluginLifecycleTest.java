package com.waypointer;

import com.waypointer.model.Library;
import com.waypointer.model.route.RouteLibrary;
import com.waypointer.service.DeathAutoPathfinder;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.service.RouteStore;
import com.waypointer.service.RouteStorePersistence;
import com.waypointer.service.WaypointMenuHandler;
import com.waypointer.service.WaypointPathfinder;
import com.waypointer.service.WaypointStore;
import com.waypointer.service.WaypointStorePersistence;
import com.waypointer.ui.NpcHighlightOverlay;
import com.waypointer.ui.RouteOverlay;
import com.waypointer.ui.TabHost;
import com.waypointer.ui.WaypointerPanel;
import java.lang.reflect.Field;
import java.util.HashSet;
import net.runelite.api.Client;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.overlay.OverlayManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WaypointerPluginLifecycleTest
{
    private WaypointerPlugin plugin;
    private WaypointStore store;
    private RouteStore routeStore;
    private EventBus eventBus;
    private KeyManager keyManager;

    @Before
    public void setUp() throws Exception
    {
        plugin = new WaypointerPlugin();
        store = new WaypointStore();     // real, so we can count its listeners
        routeStore = new RouteStore();   // real
        eventBus = mock(EventBus.class);
        keyManager = mock(KeyManager.class);

        WaypointStorePersistence persistence = mock(WaypointStorePersistence.class);
        RouteStorePersistence routePersistence = mock(RouteStorePersistence.class);
        when(persistence.load()).thenReturn(new Library());
        when(routePersistence.load()).thenReturn(new RouteLibrary());

        inject("config", mock(WaypointerConfig.class));
        inject("clientToolbar", mock(ClientToolbar.class));
        inject("panel", mock(WaypointerPanel.class));
        inject("store", store);
        inject("persistence", persistence);
        inject("eventBus", eventBus);
        inject("menuHandler", mock(WaypointMenuHandler.class));
        inject("pathfinderService", mock(WaypointPathfinder.class));
        inject("deathAutoPathfinder", mock(DeathAutoPathfinder.class));
        inject("tabHost", mock(TabHost.class));
        inject("npcHighlightOverlay", mock(NpcHighlightOverlay.class));
        inject("overlayManager", mock(OverlayManager.class));
        inject("routeStore", routeStore);
        inject("routePersistence", routePersistence);
        inject("routePlaybackEngine", mock(RoutePlaybackEngine.class));
        inject("routeOverlay", mock(RouteOverlay.class));
        inject("client", mock(Client.class));
        inject("keyManager", keyManager);
    }

    private void inject(String field, Object value) throws Exception
    {
        Field f = WaypointerPlugin.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(plugin, value);
    }

    @Test
    public void reEnableDoesNotStackSubscriptions() throws Exception
    {
        plugin.startUp();
        assertEquals("one saver after first startUp", 1, store.listenerCountForTest());
        assertEquals(1, routeStore.listenerCountForTest());

        plugin.shutDown();
        assertEquals("saver detached on shutDown", 0, store.listenerCountForTest());
        assertEquals(0, routeStore.listenerCountForTest());

        // Every EventBus registration in startUp must have a matching unregistration in shutDown.
        // Captured before the second startUp, so register() reflects only the first startUp.
        ArgumentCaptor<Object> reg = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> unreg = ArgumentCaptor.forClass(Object.class);
        verify(eventBus, atLeastOnce()).register(reg.capture());
        verify(eventBus, atLeastOnce()).unregister(unreg.capture());
        assertEquals(new HashSet<>(reg.getAllValues()), new HashSet<>(unreg.getAllValues()));

        // The hotkey listener registered in startUp is the same instance removed in shutDown.
        ArgumentCaptor<KeyListener> kReg = ArgumentCaptor.forClass(KeyListener.class);
        ArgumentCaptor<KeyListener> kUnreg = ArgumentCaptor.forClass(KeyListener.class);
        verify(keyManager).registerKeyListener(kReg.capture());
        verify(keyManager).unregisterKeyListener(kUnreg.capture());
        assertEquals(kReg.getValue(), kUnreg.getValue());

        plugin.startUp();
        assertEquals("saver must not stack on re-enable", 1, store.listenerCountForTest());
        assertEquals(1, routeStore.listenerCountForTest());
    }
}
