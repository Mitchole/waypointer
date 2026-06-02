# Developing Waypointer

This is the build-and-architecture reference for the Waypointer RuneLite plugin. For what the
plugin does from a player's side, see [README.md](README.md). To add preset waypoints or
landmark locations, see [CONTRIBUTING.md](CONTRIBUTING.md), most of which needs no Java.

## Prerequisites

- **Java 11** (Eclipse Temurin recommended). The build targets Java 11 bytecode.
- An IDE that understands Lombok. IntelliJ IDEA works out of the box; Eclipse needs the
  Lombok plugin. Lombok is wired in as a compile-only annotation processor, so there is
  nothing to set up beyond the IDE plugin.

## Building and running

Run these from the project root:

```bash
./gradlew shadowJar    # builds build/libs/waypointer-<version>-all.jar
./gradlew run          # launches RuneLite with the plugin loaded, in dev mode
./gradlew test         # runs the JUnit + Mockito test suite
```

The `<version>` in the jar name comes from `build.gradle`.

`./gradlew run` uses `com.waypointer.WaypointerPluginTest` as its entry point. That class
loads Waypointer as a built-in plugin and starts RuneLite with `--developer-mode --debug`
and assertions on.

To try a build inside a normal RuneLite install, copy the shadow jar into
`~/.runelite/sideloaded-plugins/` and restart the client.

## Code layout

```
src/main/java/com/waypointer/
  WaypointerPlugin.java   plugin entry point: lifecycle, event subscriptions, nav button
  WaypointerConfig.java   user-facing config (config group "waypointer")
  codec/                  JSON serialization, schema migration, WP1/WPL1 share codes
  model/                  Library, Category, Waypoint, WorldPointPacker
  preset/                 bundled preset catalog (Preset, PresetCatalog, PresetImport)
  service/                WaypointStore, the bbox/landmark index, and the rest of the services
  ui/                     the sidebar panel, the preset browser, the dev tools, dialogs, styling
  util/                   Listeners (a small pub/sub helper)

src/main/resources/com/waypointer/
  preset-waypoints.json   the bundled preset catalog
  icon-categories.json    groupings for the icon picker
  landmarks/, transports/ tile-to-landmark data used for capture-time naming
```

Editing the data under `resources/` is covered in [CONTRIBUTING.md](CONTRIBUTING.md).

## How it fits together

- **State.** `WaypointStore` owns the in-memory `Library` and is the single entry point for
  every mutation. Each change notifies its listeners, which is what triggers the debounced save
  and the panel rebuild.
- **Persistence.** Saves land in `~/.runelite/waypointer/library.json`, with a
  `library.json.bak` companion for atomic-rename and backup recovery. A write is debounced
  500 ms after the last change, and flushed on plugin shutdown.
- **UI.** The registered `@Singleton PluginPanel` is `TabHost`. It carries a `TabStrip` and the
  `ActivePathBanner` up top, over a `CardLayout` body holding `WaypointerPanel` and
  `PresetBrowserPanel` as cards. Switching tabs swaps the visible card; both stay live
  underneath, so listener subscriptions and scroll position survive a switch.
- **Pathing.** Routes go out to the Shortest Path plugin over `PluginMessage` through
  `WaypointPathfinder`. Shortest Path is optional: with it absent, the Play button is disabled
  and nothing else changes.
- **Threading.** Panel code runs on the Swing EDT. Reading the RuneLite `Client` means hopping
  to the client thread with `ClientThread.invoke`, then back to the EDT with
  `SwingUtilities.invokeLater`.

## Testing

Tests live under `src/test/java` and use JUnit 4 with Mockito 3. Run `./gradlew test` before
opening a pull request and keep it passing. The data files have their own coverage:
`PresetFileValidityTest` validates the preset catalog, and the bbox tests load the landmark
TSVs, so a bad data edit fails the build.

## Plugin Hub compliance

The plugin is built to pass the Plugin Hub verifier:

- No banned APIs (`WidgetInfo`, `WidgetID`, `Client.getVar*`, `new OkHttpClient()`,
  `new Gson()`, `net.runelite.client.account`, JNI).
- Java 11 bytecode (`options.release.set(11)`).
- All filesystem I/O scoped to `~/.runelite/waypointer/`.
- Logging through `@Slf4j`. No `System.out.println`, no `printStackTrace`.

A handful of framework deprecations are tolerated and suppressed at class level. If you add a
new suppression, leave a comment saying why.

Production code always uses the injected `Gson`. Where a test needs to build a catalog or
override on its own, the `forTesting(...)` factories take a `Gson`, so the construction lives
in the test sources that the hub verifier does not scan. Never reintroduce a `new Gson()` in
`src/main`.

## License

[BSD 2-Clause](LICENSE).
