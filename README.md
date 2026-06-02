<p align="center">
  <img src="waypointer-iconpng.png" alt="Waypointer" width="200">
</p>

# Waypointer

Save your favourite Old School RuneScape locations and travel to them in one click.

[![License: BSD-2-Clause](https://img.shields.io/badge/License-BSD_2--Clause-orange.svg)](LICENSE)
[![Java 11](https://img.shields.io/badge/Java-11-blue.svg)](https://adoptium.net/)
[![RuneLite Plugin](https://img.shields.io/badge/RuneLite-Plugin-red.svg)](https://runelite.net/)

> **[ Screenshot ]** The Waypointer panel docked in the RuneLite sidebar, two or three categories expanded with a handful of saved waypoints.

Waypointer is a sidebar panel for the places you keep going back to: banks, bosses, farm
patches, teleport spots. Mark a tile, name it, file it under a category. Click Play on any
waypoint and the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin draws the
route to it on your world map and minimap.

---

## Contents

- [Getting started](#getting-started)
- [Saving a waypoint](#saving-a-waypoint)
- [Organising your library](#organising-your-library)
- [Travelling to a waypoint](#travelling-to-a-waypoint)
- [Preset waypoints](#preset-waypoints)
- [Searching](#searching)
- [Sharing](#sharing)
- [Settings](#settings)
- [Requirements](#requirements)
- [License](#license)

---

## Getting started

1. Install Waypointer from the RuneLite Plugin Hub. *(Coming soon. Until then you can build
   it yourself; see [DEVELOPER.md](DEVELOPER.md).)*
2. For one-click travel, install the [Shortest Path](https://github.com/Skretzo/shortest-path)
   plugin too. Everything else in Waypointer works without it, but the Play button needs it.
3. Open the Waypointer panel from the RuneLite sidebar (the pin icon).
4. Your library starts empty. Add your own waypoints, or open the **Presets** tab at the top
   of the panel to start from a curated set.

> **[ Video ]** A 20-second tour: open the panel, mark a tile, give it a name and category, click Play, watch the route appear.

## Saving a waypoint

Three ways to capture a tile:

- **Mark current location** saves the tile your character is standing on.
- **Right-click the world map** and choose `Save as Waypoint`.
- **Shift right-click a tile in the 3D world** and choose `Save as Waypoint`. This one is off
  by default; turn it on in [Settings](#settings).

Each one opens a small dialog where you set the name and pick a category. Stand inside a known
bank, altar, anvil, furnace, fairy ring, dungeon entrance, boss arena, or other named
landmark, and the name is filled in for you.

> **[ Screenshot ]** The capture dialog open over the game, name pre-filled from the landmark under the player, category dropdown showing.

## Organising your library

- Sort waypoints into **categories** you name yourself. A fixed `Uncategorized` bucket holds
  anything you have not filed.
- Drag the `⠿` handle on a row to reorder a waypoint, or to move it into another category.
- Drag a category header to reorder the categories.
- Give any category or waypoint an **icon** from the in-game sprite picker.
- Hover a row to read the first line of its notes without opening it.
- Click a row for an inline editor over its name, category, notes, and tile. `Recapture`
  rewrites the waypoint to wherever you are standing now.

> **[ Screenshot ]** One category expanded with its inline editor open on a waypoint, showing the name, notes, and category fields.

## Travelling to a waypoint

Click `Play` on a waypoint. Waypointer hands the tile to Shortest Path, which draws the route
on the world map and minimap. To cancel a route, use **Stop pathing** in the overflow (`⋮`)
menu.

> **[ Video ]** Clicking Play on a saved bank, then following the highlighted path on the minimap to the door.

## Preset waypoints

New to the plugin, or picking up content you have not mapped yet? Open the **Presets** tab at
the top of the panel.

It holds curated sets covering Slayer masters, herb and tree patches, bosses, raids,
runecrafting altars, and more. Expand a set and click `+` on a waypoint to drop it into your
library. Waypoints you already have are marked, so you will not add the same tile twice.

> **[ Screenshot ]** The preset browser with the Slayer Masters set expanded, a couple of rows showing the already-added tick.

## Searching

The search bar at the top of the panel filters by waypoint name, notes, or category name as
you type. Categories holding a match expand on their own. Clear the search and every category
goes back to how you left it.

## Sharing

- Copy a share code for a single waypoint (`WP1:`) or your whole library (`WPL1:`).
- Paste someone else's code through **Import library...** in the overflow menu.

## Settings

| Setting | Default | What it does |
|---|---|---|
| Right-click tiles to save | Off | Adds `Save as Waypoint` to the Shift right-click menu on tiles in the 3D world. |

## Requirements

- RuneLite.
- The [Shortest Path](https://github.com/Skretzo/shortest-path) plugin, for the Play button.
  Capturing, organising, searching, and sharing all work without it.

## License

Released under the [BSD 2-Clause License](LICENSE). Built on the
[RuneLite](https://runelite.net/) plugin framework.

Building from source, the code layout, and how to add preset waypoints or landmark locations
are covered in [DEVELOPER.md](DEVELOPER.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## Credits

Landmark locations (banks, altars, anvils, furnaces, looms, spinning wheels, tanners, spirit
trees, charter ships, fairy rings, slayer masters, and more) are derived from the
[OSRS Wiki](https://oldschool.runescape.wiki/) under the Creative Commons
Attribution-ShareAlike 4.0 International licence.
