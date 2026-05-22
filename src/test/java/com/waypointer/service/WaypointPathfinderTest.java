package com.waypointer.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WaypointPathfinderTest
{
    private EventBus eventBus;
    private ClientThread clientThread;
    private Client client;
    private ConfigManager configManager;
    private WaypointPathfinder pathfinder;

    @Before
    public void setUp()
    {
        eventBus = mock(EventBus.class);
        clientThread = mock(ClientThread.class);
        client = mock(Client.class);
        configManager = mock(ConfigManager.class);
        // Run any BooleanSupplier passed to ClientThread.invoke synchronously so we can verify
        // the eventBus.post side-effect within the test.
        doAnswer(invocation -> {
            BooleanSupplier s = invocation.getArgument(0);
            s.getAsBoolean();
            return null;
        }).when(clientThread).invoke(any(BooleanSupplier.class));
        pathfinder = new WaypointPathfinder(eventBus, clientThread, client, configManager);
    }

    @Test
    public void unavailableByDefault()
    {
        assertFalse(pathfinder.isAvailable());
    }

    @Test
    public void availableWhenConfigProbeFindsPersistedKey()
    {
        when(configManager.getConfiguration("shortestpath", "recalculateDistance")).thenReturn("true");
        WaypointPathfinder p = new WaypointPathfinder(eventBus, clientThread, client, configManager);
        assertTrue(p.isAvailable());
    }

    @Test
    public void availableAfterReceivingShortestPathPluginMessage()
    {
        pathfinder.onPluginMessage(new PluginMessage("shortestpath", "transports", new HashMap<>()));
        assertTrue(pathfinder.isAvailable());
    }

    @Test
    public void ignoresPluginMessageFromOtherNamespace()
    {
        pathfinder.onPluginMessage(new PluginMessage("someotherplugin", "hello", new HashMap<>()));
        assertFalse(pathfinder.isAvailable());
    }

    @Test
    public void doesNotSelfDetectOnOwnOutboundPathOrClear()
    {
        // EventBus delivers posts to all subscribers including the poster. Our own outbound
        // `path` / `clear` messages must not be treated as proof of shortest-path's presence.
        pathfinder.onPluginMessage(new PluginMessage("shortestpath", "path", new HashMap<>()));
        assertFalse("path is sent by Waypointer, not by shortest-path", pathfinder.isAvailable());
        pathfinder.onPluginMessage(new PluginMessage("shortestpath", "clear", new HashMap<>()));
        assertFalse("clear is sent by Waypointer, not by shortest-path", pathfinder.isAvailable());
    }

    @Test
    public void availableAfterConfigChangedInShortestPathGroup()
    {
        ConfigChanged e = new ConfigChanged();
        e.setGroup("shortestpath");
        e.setKey("drawTransports");
        pathfinder.onConfigChanged(e);
        assertTrue(pathfinder.isAvailable());
    }

    @Test
    public void ignoresConfigChangedInOtherGroup()
    {
        ConfigChanged e = new ConfigChanged();
        e.setGroup("waypointer");
        e.setKey("anything");
        pathfinder.onConfigChanged(e);
        assertFalse(pathfinder.isAvailable());
    }

    @Test
    public void availabilityIsSticky()
    {
        pathfinder.onPluginMessage(new PluginMessage("shortestpath", "transports", new HashMap<>()));
        // Subsequent unrelated events must not reset the flag.
        ConfigChanged unrelated = new ConfigChanged();
        unrelated.setGroup("waypointer");
        pathfinder.onConfigChanged(unrelated);
        assertTrue(pathfinder.isAvailable());
    }

    @Test
    public void availabilityChangeFiresListenersOnce()
    {
        Runnable listener = mock(Runnable.class);
        pathfinder.subscribe(listener);
        pathfinder.onPluginMessage(new PluginMessage("shortestpath", "transports", new HashMap<>()));
        // Second message should not re-fire: already available.
        pathfinder.onPluginMessage(new PluginMessage("shortestpath", "transports", new HashMap<>()));
        verify(listener, times(1)).run();
    }

    @Test
    public void requestPathPostsExpectedPluginMessageOnClientThread()
    {
        pathfinder.requestPath(12345, "test");

        verify(clientThread).invoke(any(BooleanSupplier.class));
        ArgumentCaptor<PluginMessage> captor = ArgumentCaptor.forClass(PluginMessage.class);
        verify(eventBus).post(captor.capture());
        PluginMessage msg = captor.getValue();
        assertEquals("shortestpath", msg.getNamespace());
        assertEquals("path", msg.getName());
        Map<String, Object> data = msg.getData();
        assertEquals(Integer.valueOf(12345), data.get("target"));
        assertNull("v1 sends no start; shortest-path uses local player", data.get("start"));
    }

    @Test
    public void clearPathPostsClearMessageOnClientThread()
    {
        pathfinder.clearPath();

        verify(clientThread).invoke(any(BooleanSupplier.class));
        ArgumentCaptor<PluginMessage> captor = ArgumentCaptor.forClass(PluginMessage.class);
        verify(eventBus).post(captor.capture());
        assertEquals("clear", captor.getValue().getName());
    }
}
