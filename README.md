# Waypointer

Save your favourite Old School RuneScape locations and travel to them in one click.

[![License: BSD-2-Clause](https://img.shields.io/badge/License-BSD_2--Clause-orange.svg)](LICENSE)
[![Java 11](https://img.shields.io/badge/Java-11-blue.svg)](https://adoptium.net/)
[![RuneLite Plugin](https://img.shields.io/badge/RuneLite-Plugin-red.svg)](https://runelite.net/)

> **[ Screenshot ]** The Waypointer panel docked in the RuneLite sidebar, showing a couple of categories of saved waypoints.

Waypointer adds a sidebar panel for the places you keep going back to: banks, bosses, farm
patches, teleport spots. Mark a tile, name it, drop it in a category. Click Play on any
waypoint and the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin draws the
route on your world map and minimap.

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
2. Install the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin as well if
   you want one-click travel. Waypointer runs fine without it, but the Play button needs it.
3. Open the Waypointer panel from the RuneLite sidebar (the pin icon).
4. Your library starts empty. Add your own waypoints, or switch to the **Presets** tab at
   the top of the panel to start from a curated set.

> **[ Video ]** Short tour: opening the panel, saving a waypoint, pathing to it.

## Saving a waypoint

There are three ways to capture a tile:

- **Mark current location** captures the tile your character is standing on.
- **Right-click the world map** and choose `Save as Waypoint`.
- **Right-click a tile in the 3D world** with Shift held. This one is off by default; turn
  it on in [Settings](#settings).

Each way opens a small dialog where you set the name and pick a category. When the tile is
inside a known bank, altar, anvil, furnace, fairy ring, dungeon entrance, boss arena, or
other named landmark, the name is filled in for you.

> **[ Screenshot ]** The capture dialog with a name typed and a category chosen.

## Organising your library

- Sort waypoints into **categories** you name yourself. A fixed `Uncategorized` bucket
  holds anything not filed elsewhere.
- Drag the `⠿` handle on a row to reorder a waypoint, inside its category or into another.
- Drag a category header to reorder the categories.
- Give any category or waypoint an **icon** from the in-game sprite picker.
- Hover a row to read the first line of its notes without expanding it.
- Click a row to open an inline editor for its name, category, notes, and tile. The
  `Recapture` action rewrites a waypoint to wherever you are standing now.

> **[ Screenshot ]** A category expanded, with one waypoint's inline editor open.

## Travelling to a waypoint

Click the `Play` button on a waypoint. Waypointer hands the destination to Shortest Path,
which draws the route on the world map and minimap. To cancel an active route, use
**Stop pathing** in the overflow (`⋮`) menu.

> **[ Video ]** Clicking Play on a waypoint and following the drawn route.

## Preset waypoints

New to the plugin, or picking up content you have not mapped yet? Click the **Presets** tab
at the top of the panel.

It shows a browser of curated sets covering Slayer masters, herb and tree patches, bosses,
raids, runecrafting altars, and more. Expand a set and click `+` on a waypoint to add it to
your library straight away. Waypoints you already have are marked, so you will not add a
duplicate.

> **[ Screenshot ]** The preset browser with one set expanded and a few waypoints already added.

## Searching

The search bar at the top of the panel filters by waypoint name, notes, or category name
as you type. Categories with a match expand on their own. Clearing the search puts every
category back to how you had it.

## Sharing

- Copy a share code for a single waypoint (`WP1:`) or your whole library (`WPL1:`).
- Paste a code from someone else through **Import library...** in the overflow menu.

## Settings

| Setting | Default | What it does |
|---|---|---|
| Right-click tiles to save | Off | Adds `Save as Waypoint` to the Shift right-click menu on tiles in the 3D world. |

## Requirements

- RuneLite.
- The [Shortest Path](https://github.com/Skretzo/shortest-path) plugin, for the Play
  button. Capturing, organising, searching, and sharing all work without it.

## License

Released under the [BSD 2-Clause License](LICENSE). Built on the
[RuneLite](https://runelite.net/) plugin framework.

Building from source, the code layout, and how to contribute are covered in
[DEVELOPER.md](DEVELOPER.md).

## Credits

Bank, altar, anvil, furnace, loom, spinning wheel, tanner, spirit tree, bank chest, charter
ship, fairy ring, slayer master, and unique landmark locations are derived from the OSRS Wiki and used under the Creative
Commons Attribution-ShareAlike 4.0 International licence. Source: https://oldschool.runescape.wiki/.
