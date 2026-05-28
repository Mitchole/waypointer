# Developing Waypointer

How to build, test, and work on the Waypointer RuneLite plugin. For what the plugin does
from a player's side, see [README.md](README.md).

## Prerequisites

- **Java 11** (Eclipse Temurin recommended). The build targets Java 11 bytecode.
- An IDE that understands Lombok. IntelliJ IDEA works out of the box; Eclipse needs the
  Lombok plugin. Lombok is wired in as a compile-only annotation processor, so no setup is
  needed beyond the IDE plugin.

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
  service/                WaypointStore and the services around it
  ui/                     the sidebar panel, the preset browser, dialogs, styling helpers
  util/                   Listeners (a small pub/sub helper)

src/main/resources/com/waypointer/
  preset-waypoints.json   the bundled preset catalog (see below)
  icon-categories.json    groupings for the icon picker
  landmarks/, transports/ tile-to-landmark data used for capture-time naming
```

## How it fits together

- **State.** `WaypointStore` owns the in-memory `Library` and is the single entry point for
  every mutation. A change notifies registered listeners, which trigger a debounced save
  and a panel rebuild.
- **Persistence.** The library is written to `~/.runelite/waypointer/library.json`, with a
  `library.json.bak` companion for atomic-rename and backup recovery. Saves are debounced
  500 ms after the last change and flushed on plugin shutdown.
- **UI.** `TabHost` is the `@Singleton PluginPanel` registered with RuneLite. It hosts a
  `TabStrip` and the `ActivePathBanner` at the top, plus a `CardLayout` body that holds
  `WaypointerPanel` and `PresetBrowserPanel` as `@Singleton` cards. Clicking a tab swaps
  which card is visible; both cards stay live underneath so listener subscriptions and
  scroll position survive.
- **Pathing.** `WaypointPathfinder` talks to the Shortest Path plugin over `PluginMessage`.
  Shortest Path is optional. When it is not installed, the Play button is disabled and the
  rest of the panel still works.
- **Threading.** Panel code runs on the Swing EDT. Anything that reads the RuneLite
  `Client` has to hop to the client thread with `ClientThread.invoke`, then back to the EDT
  with `SwingUtilities.invokeLater`.

## Testing

Tests live under `src/test/java` and use JUnit 4 with Mockito 3. Run `./gradlew test`
before opening a pull request and keep it passing.

## The preset catalog

The bundled preset sets live in `src/main/resources/com/waypointer/preset-waypoints.json`.
The format is plain JSON so anyone can add or correct waymarks with a pull request, no code
change required:

```json
{
  "presets": [
    {
      "category": "Herb Patches",
      "description": "Allotment herb patches for farm runs.",
      "icon": 1468,
      "waypoints": [
        {
          "name": "Ardougne",
          "description": "Behind the monastery",
          "x": 2670,
          "y": 3375,
          "plane": 0
        }
      ]
    }
  ]
}
```

- `category` is the name of the category a waypoint lands in when a player adds it.
  `description` on a preset is optional.
- `icon` is an optional RuneLite sprite id for the category.
- A waypoint has a `name`, an optional `description` (it becomes the waypoint's notes), and
  `x` / `y` / `plane` world coordinates.

`PresetFileValidityTest` parses the real file on every build and checks the coordinates are
in range, so a broken edit fails `./gradlew test`.

## Plugin Hub compliance

The plugin is built to pass the Plugin Hub verifier:

- No banned APIs (`WidgetInfo`, `WidgetID`, `Client.getVar*`, `new OkHttpClient()`,
  `new Gson()`, `net.runelite.client.account`, JNI).
- Java 11 bytecode (`options.release.set(11)`).
- All filesystem I/O scoped to `~/.runelite/waypointer/`.
- Logging through `@Slf4j`. No `System.out.println`, no `printStackTrace`.

A handful of framework deprecations are tolerated and suppressed at class level. If you add
a new suppression, leave a comment saying why.

## Contributing

- Run `./gradlew test` and make sure it passes.
- Match the existing style: four-space indentation, no wildcard imports, Lombok where the
  surrounding code already uses it, logging via `@Slf4j`.
- For a new feature, sketch the idea in an issue before writing code.
- To add or fix preset waypoints, edit `preset-waypoints.json` directly.

## License

[BSD 2-Clause](LICENSE).
