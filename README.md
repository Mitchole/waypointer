<p align="center">
  <img src="waypointer-iconpng.png" alt="Waypointer" width="200">
</p>

<h1 align="center">Waypointer</h1>

<p align="center"><em>Save your favourite Old School RuneScape locations and travel to them in one click.</em></p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-BSD_2--Clause-orange.svg" alt="License: BSD-2-Clause"></a>
  <a href="https://adoptium.net/"><img src="https://img.shields.io/badge/Java-11-blue.svg" alt="Java 11"></a>
  <a href="https://runelite.net/"><img src="https://img.shields.io/badge/RuneLite-Plugin-red.svg" alt="RuneLite Plugin"></a>
</p>

> **[ Screenshot ]** The Waypointer panel docked in the RuneLite sidebar, the nearest-landmark bar across the top and two or three categories expanded below. (Will be "panel.png")

Waypointer is a sidebar panel for the places you keep going back to: banks, bosses, farm
patches, teleport spots. Mark a tile, name it, file it under a category. Click Play on any
waypoint and the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin draws the
route to it on your world map and minimap.

It also keeps a one-click bar to the nearest bank, altar, or anvil, and lets you string
waypoints into guided **Routes** for farm runs and agility laps.

> **Waypointer needs the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin.**
> It does the actual pathfinding behind every Play button, the nearest-landmark bar, and
> routes. Install it from the Plugin Hub before you start.

---

## Contents

- [Getting started](#getting-started)
- [Saving a waypoint](#saving-a-waypoint)
- [Organising your library](#organising-your-library)
- [Travelling to a waypoint](#travelling-to-a-waypoint)
- [The nearest-landmark bar](#the-nearest-landmark-bar)
- [Routes](#routes)
- [Preset waypoints](#preset-waypoints)
- [Searching](#searching)
- [Sharing](#sharing)
- [Settings](#settings)
- [Requirements](#requirements)
- [License](#license)

---

## Getting started

1. Install the [Shortest Path](https://github.com/Skretzo/shortest-path) plugin from the
   RuneLite Plugin Hub. Waypointer relies on it for all pathfinding.
2. Install Waypointer from the RuneLite Plugin Hub. *(Coming soon. Until then you can build
   it yourself; see [DEVELOPER.md](DEVELOPER.md).)*
3. Open the Waypointer panel from the RuneLite sidebar (the pin icon). The tabs across the top
   are **My Waypoints**, **Presets**, and **Routes**.
4. Your library starts empty. Add your own waypoints, or open the **Presets** tab to start
   from a curated set.

> **[ Video ]** A 20-second tour: open the panel, mark a tile, give it a name and category, click Play, watch the route appear. (will be "tour.mp4"

## Saving a waypoint

Four ways to capture a tile:

- **Mark current location** saves the tile your character is standing on.
- **Right-click the world map** and choose `Save as Waypoint`.
- **Shift right-click a tile in the 3D world** and choose `Save as Waypoint`.
- **Right-click an NPC or object** and choose `Save as Waypoint`. The name and tile at the
  moment you click become the defaults.

The last two are off by default; turn them on in [Settings](#settings).

Each one opens a small dialog where you set the name and pick a category. Stand inside a known
bank, altar, anvil, furnace, fairy ring, dungeon entrance, boss arena, or other named
landmark, and the name is filled in for you.

> **[ Screenshot ]** The capture dialog open over the game, name pre-filled from the landmark under the player, category dropdown showing. (will be "capture.png")

## Organising your library

- Sort waypoints into **categories** you name yourself. A fixed `Uncategorized` bucket holds
  anything you have not filed.
- **Pin** a waypoint to lift it into a section at the very top of the panel, above the
  categories, for the handful you path to constantly.
- Drag the `⠿` handle on a row to reorder a waypoint, or to move it into another category.
- Drag a category header to reorder the categories.
- Give any category or waypoint an **icon** from the in-game sprite picker.
- Hover a row to read the first line of its notes without opening it.
- Click a row for an inline editor over its name, category, notes, and tile. `Recapture`
  rewrites the waypoint to wherever you are standing now.

> **[ Screenshot ]** One category expanded with its inline editor open on a waypoint, showing the name, notes, and category fields. (will be "editor.png")

## Travelling to a waypoint

Click `Play` on a waypoint. Waypointer hands the tile to Shortest Path, which draws the route
on the world map and minimap. While a path is active, a **Pathing to** strip sits at the top of
the panel; clear it with **Stop pathing** in the overflow (`⋮`) menu.

A couple of safety nets, both on by default and both toggleable in [Settings](#settings):

- Rows whose tile is in the Wilderness carry a skull, and Playing one asks you to confirm
  first.
- **Auto-path to death location** starts a route back to where you died the moment it happens,
  so you can run for your gravestone without fishing for the tile.

> **[ Video ]** Clicking Play on a saved bank, then following the highlighted path on the minimap to the door. (will be "waypoint.mp4")

## The nearest-landmark bar

Across the top of the panel is a row of landmark buttons: bank, altar, anvil, and the like.
Click one and Waypointer paths to the nearest landmark of that type from wherever you are
standing, no saved waypoint needed. The `⋮` button on the bar lets you choose which landmark
types show and in what order. Hide the whole bar from [Settings](#settings) if you would
rather not have it.

## Routes

A **Route** is an ordered list of stops you run one after another: a herb farm run, an agility
lap, a wintertodt prep loop. Open the **Routes** tab to build and run them.

A route is made of steps, and there are two kinds:

- **Waypoint steps** path to a tile and advance on their own once you arrive.
- **Manual steps** are a line of text ("climb the ladder", "bank everything") that wait for
  you to press Next.

Build a route by adding steps from your library, marking your current tile as you walk it, or
typing a manual instruction. Turn on **repeat** for a loop that starts over at the end.

Run a route from the playback bar: **Back**, **Next**, **Stop**, and a step counter. Set a
**next-step hotkey** in [Settings](#settings) to advance without leaving the game, and keep the
in-game route box on screen so the current step is always visible. Share a route with anyone
using an `RT1:` code.

> **[ Screenshot ]** The Routes tab mid-run: the playback bar with its step counter, and the in-game route box showing the current step. (will be "route.png")

## Preset waypoints

New to the plugin, or picking up content you have not mapped yet? Open the **Presets** tab.

It holds curated sets covering Slayer masters, herb and tree patches, bosses, raids,
runecrafting altars, and more. Expand a set and click `+` on a waypoint to drop it into your
library. Waypoints you already have are marked, so you will not add the same tile twice.

> **[ Screenshot ]** The preset browser with the Slayer Masters set expanded, a couple of rows showing the already-added tick.

## Searching

The search bar at the top of the panel filters by waypoint name, notes, or category name as
you type. Categories holding a match expand on their own. Clear the search and every category
goes back to how you left it.

## Sharing

- Copy a share code for a single waypoint (`WP1:`), your whole library (`WPL1:`), or a route
  (`RT1:`).
- Paste someone else's code through **Import library...** in the overflow menu.

## Settings

| Setting | Default | What it does |
|---|---|---|
| Right-click tiles to save | Off | Adds `Save as Waypoint` to the Shift right-click menu on tiles in the 3D world. |
| Right-click NPCs and objects to save | Off | Adds `Save as Waypoint` to the right-click menu on NPCs and objects. |
| Show 'nearest landmark' bar | On | Shows the one-click row of nearest-landmark shortcuts at the top of the panel. |
| Show 'Pathing to' banner | On | Shows a status strip at the top of the panel while a path is active. |
| Confirm before pathing into Wilderness | On | Asks before Playing a waypoint whose tile is in the Wilderness. |
| Show wilderness skull on row | On | Marks rows whose destination is in the Wilderness with a skull. |
| Auto-path to death location | Off | Starts a path back to your death tile when you die. Needs Shortest Path. |
| Newest pin at top | On | Orders pinned waypoints with the most recently pinned first. |
| Enable Routes | On | Shows the Routes tab. Off hides the feature. |
| Route: next step hotkey | Unset | Key that advances the current route step while a route runs. |
| Show in-game route box | On | Draws the read-only current-step box in the game window during a route. |

## Requirements

- RuneLite.
- The [Shortest Path](https://github.com/Skretzo/shortest-path) plugin. Waypointer hands every
  destination to it for pathfinding, so install it first. Without it the Play button, the
  nearest-landmark bar, and routes have nothing to draw a path with.

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
