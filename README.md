# Waypointer

A RuneLite plugin for saving in-game locations and one-click pathing.

[![License: BSD-2-Clause](https://img.shields.io/badge/License-BSD_2--Clause-orange.svg)](LICENSE)
[![Java 11](https://img.shields.io/badge/Java-11-blue.svg)](https://adoptium.net/)
[![RuneLite Plugin](https://img.shields.io/badge/RuneLite-Plugin-red.svg)](https://runelite.net/)

**Waypoints** - your personal atlas. Mark a tile, give it a name and category, then click Play
to have [Shortest Path](https://github.com/Skretzo/shortest-path) draw the route on the world
map and minimap.

---

## Table of contents

- [Waypoints](#waypoints)
- [Shared features](#shared-features)
- [Configuration](#configuration)
- [Installation](#installation)
- [Building from source](#building-from-source)
- [Architecture](#architecture)
- [Plugin Hub compliance](#plugin-hub-compliance)
- [Contributing](#contributing)
- [License](#license)
- [Attribution](#attribution)

---

## Waypoints

### Capture
- **Mark current location** button captures your character's tile.
- **Right-click `Save as Waypoint`** on the world map.
- **Right-click tiles** with Shift held - off by default; toggle in plugin config.
- All capture flows open a small modal where you set the name and category. Names default to
  the matched landmark (e.g. `Grand Exchange (3162, 3486)`) when the tile is a known bank,
  altar, anvil, or apothecary; plain coords otherwise. Pick an existing category or create a
  new one inline.

### Organize
- User-managed categories with a fixed `Uncategorized` bucket.
- Drag a row's `⠿` handle to reorder within or across categories.
- Drag a category header to reorder categories themselves.
- Each category and each waypoint can carry an icon, picked from a paginated grid of in-game sprites.
- Hover a row to preview the first line of its notes - no need to expand the row first.

### Edit
- Click a row's body to reveal an inline edit panel with name, category, notes, and tile.
- Edits to name and notes are saved on field focus loss (or panel collapse), so the cursor
  stays put while you type.
- `Recapture` rewrites the waypoint to your current tile; `Set icon` opens the icon picker;
  `Copy share code` produces a `WP1:` code for that single waypoint.

### Path
- `▶` Play icon on each row posts a `PluginMessage` to Shortest Path, which draws the route
  on the world map and minimap automatically.
- A small banner appears at the top of the panel while a path is active:
  `→ Pathing to <waypoint>` with a `Stop` button. Auto-clears when you arrive within ~3 tiles
  of the target on the same plane, swap to a different waypoint, or Shortest Path becomes
  unavailable.

### Bundled defaults
Ships with 15 starter categories you can opt into via the first-run prompt or
`Import defaults` in the overflow menu. They land empty - fillable over time:

> Cities & Hubs · Standalone Banks · Herb Patches · Tree Patches · Fruit Tree Patches ·
> Runecrafting Altars · Slayer Masters · Slayer Dungeons · Open World Bosses ·
> God Wars Dungeon Bosses · Wildy Bosses · Instance Bosses · Raids · Skilling Hotspots ·
> Notable POI's

Bundled categories always sort below your own custom categories so your stuff stays at the top.

---

## Shared features

### Find
Live search bar at the top of the panel filters by name, notes, or category name
(case-insensitive substring). Categories with at least one match auto-expand; zero-match
categories disappear. Clearing the search snaps every category back to its previous collapse
state.

### Share codes
| Magic | What it carries |
|---|---|
| `WP1:` | A single waypoint |
| `WPL1:` | The whole waypoint library |

Copy to clipboard or paste-import via the overflow menu's `Import library...` modal.

### Persistence
Library is stored at `~/.runelite/waypointer/library.json` with a `library.json.bak` for
atomic rename + backup recovery. Saves are debounced 500 ms after the last mutation and
flushed on plugin shutdown.

---

## Configuration

| Setting | Default | Effect |
|---|---|---|
| Right-click tiles to save | Off | Adds `Save as Waypoint` to Shift right-click on 3D tiles. |

---

## Installation

### From the Plugin Hub
*Coming soon*

### From a sideloaded jar
Build the shadow jar (see below) and drop `build/libs/waypointer-*-all.jar` into
`~/.runelite/sideloaded-plugins/`. Restart RuneLite.

---

## Building from source

```bash
./gradlew shadowJar       # build/libs/waypointer-1.0.0-SNAPSHOT-all.jar
./gradlew run             # launches RuneLite in dev mode (-ea, --developer-mode --debug)
./gradlew test            # runs the JUnit + Mockito test suite
```

Requires **Java 11** (Eclipse Temurin recommended). Lombok is wired in as a compile-only
annotation processor; modern IDEs handle this automatically.

---

## Architecture

```
src/main/java/com/waypointer/
├── WaypointerPlugin.java   Plugin entry point - startUp/shutDown, event subscriptions, hotkeys, overlays
├── WaypointerConfig.java   User-facing config (group "waypointer")
├── codec/                  JSON <-> model serialization, share codes, schema migration
├── model/                  Library, Category, Waypoint
├── service/                WaypointStore, WaypointPathfinder, ...
├── ui/                     WaypointerPanel + dialogs (capture, icon picker, paste import)
└── util/                   ItemContainers helper, Listeners (lightweight pub/sub)
```

---

## Plugin Hub compliance

- No banned APIs (`WidgetInfo`, `WidgetID`, `Client.getVar*`, `new OkHttpClient()`,
  `new Gson()`, `net.runelite.client.account`, JNI).
- Java 11 bytecode (`options.release.set(11)`).
- All filesystem I/O scoped to `~/.runelite/waypointer/`.
- No `System.out.println` or `e.printStackTrace`; logging via `@Slf4j`.

---

## Contributing

Issues and PRs welcome.

1. Run `./gradlew test` and ensure all tests pass.
2. Match the existing code style - Lombok-friendly, four-space indent, no wildcard imports,
   logging via `@Slf4j`.
3. For new features, sketch the design in an issue first. The bar for v1 is "demonstrably
   useful and doesn't regress existing flows."

---

## License

[BSD 2-Clause](LICENSE).

---

## Attribution

- Plugin icon: [Waypoint Map](https://icons8.com/icon/YWTGLb9p2sC7/waypoint-map) by [Icons8](https://icons8.com).
- Bank, altar, anvil, and apothecary location data adapted from [Shortest Path](https://github.com/Skretzo/shortest-path) under BSD 2-Clause. Thanks to Skretzo for the maintained dataset.
- Built on the [RuneLite](https://runelite.net/) plugin framework.
