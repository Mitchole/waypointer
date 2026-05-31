package com.waypointer.service;

import com.waypointer.model.Library;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ProfileLibrarySwitcherTest
{
    private WaypointStore store;
    private WaypointStorePersistence persistence;
    private ConfigManager configManager;
    private ProfileLibrarySwitcher switcher;

    @Before
    public void setUp()
    {
        store = mock(WaypointStore.class);
        persistence = mock(WaypointStorePersistence.class);
        configManager = mock(ConfigManager.class);
        when(persistence.loadOrEmpty()).thenReturn(new Library());
        switcher = new ProfileLibrarySwitcher(store, persistence, configManager);
    }

    @Test
    public void initializeBootstrapsDefaultSlotWithoutRemembering()
    {
        switcher.initialize(null);
        InOrder o = inOrder(persistence, store);
        o.verify(persistence).switchProfile(null);
        o.verify(persistence).seedFromDefault();
        o.verify(store).bootstrap(any(Library.class));
        verify(configManager, never()).setConfiguration(anyString(), anyString(), anyString());
    }

    @Test
    public void initializeSeedsAndRemembersAccount()
    {
        switcher.initialize("main");
        InOrder o = inOrder(persistence, store);
        o.verify(persistence).switchProfile("main");
        o.verify(persistence).seedFromDefault();
        o.verify(store).bootstrap(any(Library.class));
        verify(configManager).setConfiguration(
            ProfileLibrarySwitcher.CONFIG_GROUP, ProfileLibrarySwitcher.LAST_KEY, "main");
    }

    @Test
    public void switchToNullKeyIsIgnored()
    {
        switcher.switchToProfile(null);
        verify(store, never()).flushPendingSave();
        verify(store, never()).bootstrap(any(Library.class));
        verify(persistence, never()).switchProfile(any());
    }

    @Test
    public void switchToActiveKeyIsNoOp()
    {
        when(persistence.getActiveProfileKey()).thenReturn("main");
        switcher.switchToProfile("main");
        verify(store, never()).flushPendingSave();
        verify(store, never()).bootstrap(any(Library.class));
        verify(persistence, never()).switchProfile(any());
    }

    @Test
    public void switchToNewKeyFlushesSwapsSeedsReloadsRemembers()
    {
        when(persistence.getActiveProfileKey()).thenReturn(null);
        switcher.switchToProfile("iron");
        InOrder o = inOrder(store, persistence, configManager);
        o.verify(store).flushPendingSave();
        o.verify(persistence).switchProfile("iron");
        o.verify(persistence).seedFromDefault();
        o.verify(store).bootstrap(any(Library.class));
        o.verify(configManager).setConfiguration(
            ProfileLibrarySwitcher.CONFIG_GROUP, ProfileLibrarySwitcher.LAST_KEY, "iron");
    }

    @Test
    public void resolveStartupKeyPrefersLiveThenStored()
    {
        assertEquals("live", switcher.resolveStartupKey("live"));
        when(configManager.getConfiguration(
            ProfileLibrarySwitcher.CONFIG_GROUP, ProfileLibrarySwitcher.LAST_KEY)).thenReturn("stored");
        assertEquals("stored", switcher.resolveStartupKey(null));
    }

    @Test
    public void switchBetweenTwoAccountsFlushesOutgoing()
    {
        when(persistence.getActiveProfileKey()).thenReturn("main");
        switcher.switchToProfile("iron");
        InOrder o = inOrder(store, persistence, configManager);
        o.verify(store).flushPendingSave();
        o.verify(persistence).switchProfile("iron");
        o.verify(persistence).seedFromDefault();
        o.verify(store).bootstrap(any(Library.class));
        o.verify(configManager).setConfiguration(
            ProfileLibrarySwitcher.CONFIG_GROUP, ProfileLibrarySwitcher.LAST_KEY, "iron");
    }

    @Test
    public void resolveStartupKeyNullWhenNoLiveAndNoStored()
    {
        // getConfiguration is unstubbed, so Mockito returns null -> no prior account.
        assertNull(switcher.resolveStartupKey(null));
    }
}
