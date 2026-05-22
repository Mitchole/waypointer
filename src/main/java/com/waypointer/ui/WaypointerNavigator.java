package com.waypointer.ui;

import com.waypointer.preset.PresetCatalog;
import com.waypointer.service.WaypointStore;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.MultiplexingPluginPanel;

/**
 * Sidebar sub-panel navigation. The {@link MultiplexingPluginPanel} is created in
 * {@code WaypointerPlugin.startUp()} and registered here through {@link #attach}. Panels
 * call {@link #openPresetBrowser()} / {@link #closePresetBrowser()} without needing a
 * reference to the muxer or to each other.
 */
@Singleton
public class WaypointerNavigator
{
    private final PresetCatalog presetCatalog;
    private final WaypointStore store;
    private final SpriteManager spriteManager;

    private MultiplexingPluginPanel muxer;
    private PresetBrowserPanel presetBrowser;

    @Inject
    public WaypointerNavigator(PresetCatalog presetCatalog, WaypointStore store,
        SpriteManager spriteManager)
    {
        this.presetCatalog = presetCatalog;
        this.store = store;
        this.spriteManager = spriteManager;
    }

    /** Called once per plugin start-up with the freshly built muxer. */
    public void attach(MultiplexingPluginPanel muxer)
    {
        this.muxer = muxer;
    }

    public void openPresetBrowser()
    {
        if (muxer == null)
        {
            return;
        }
        presetBrowser = new PresetBrowserPanel(
            presetCatalog, store, spriteManager, this::closePresetBrowser);
        muxer.pushState(presetBrowser);
    }

    public void closePresetBrowser()
    {
        if (muxer == null || presetBrowser == null)
        {
            return;
        }
        muxer.popState();
        presetBrowser = null;
    }
}
