# Future Ideas

Features deferred past v1. Not commitments. Each entry roughs out what it is, why it might
be worth doing, and how big the work looks.

---

## Bulk row actions

**What:** multi-select rows (Ctrl-click / Shift-click), then bulk move-to-category, bulk
delete, bulk export.

**Why interesting:** once a user has 100+ waypoints across 16 categories, they'll want to
reorganize. Single-row drag-and-drop is fine for tweaks; bulk moves need a different verb.

**Scope:** medium. Adds selection state to `WaypointerPanel`, a checkbox column on rows when
in "select mode", and a contextual action bar at the bottom of the panel ("3 selected · Move
to ▼ · Delete"). Probably gated behind a "Select" mode toggle so the default click behavior
(expand-inline) stays unchanged.

---

## Per-category sort modes

**What:** the right-click menu on a category header gets a `Sort by ▶` submenu: `Manual`,
`Name (A-Z)`, `Date added (newest first)`. Manual is the current behavior.

**Why interesting:** for categories like "Slayer Masters" or "Notable POIs" where there's no
natural manual order, users will rearrange forever or just leave it messy. A built-in name
sort fixes both. Manual stays the default so it doesn't surprise anyone.

**Scope:** small. Persist the chosen mode per category. Sort applies in
`getWaypointsInCategory` after the existing manual sortOrder fetch.

---

## Auto-filtered "Nearby" view

**What:** when the search bar is empty, render a synthetic "Nearby" pseudo-section at the very
top of the panel showing the N closest waypoints to the player's current tile (default N=5).
Excludes the currently-pathing target so it doesn't recommend "the place you're already going."
Updates as the player moves; once per `GameTick` is plenty.

**Why interesting:** the panel is sorted by user-managed categories, which is great for
deliberate navigation but bad for the common case of "what saved spot is nearest right now?"
Especially useful when freshly logging in away from your usual hub, or when a slayer task
drops you somewhere unfamiliar. The answer is one glance away instead of a category hunt.

**Settings:**
- Toggle: Show "Nearby" section (on / off, default on).
- Slider: How many waypoints to show (3-10, default 5).
- Toggle: Restrict to same plane (on / off, default on). Otherwise dungeons surface above
  overworld waypoints when you're underground, which is rarely what you want.

**Scope:** small. Pure derived view; no persistence change. A `Nearby` helper service computes
the K nearest waypoints from the player's `WorldPoint` on each `GameTick` if the panel is
visible (and bails out cheaply when the cache is fresh). The panel's `rebuild()` reads the
helper's current list and prepends a `CategorySection` flagged as synthetic (no drag-drop,
no rename / delete options).

**Edge cases:**
- Player not logged in → hide the section entirely (don't render "Nearby" with stale tiles).
- Filtering active → hide; the search bar becomes the user's primary surface.
- Very small libraries (< N waypoints) → just show all of them rather than padding.

---

## Recordable routes

**What:** a Route is an ordered sequence of steps the plugin guides you through, one step
at a time, advancing as it detects each step is done. Steps cover more than tiles: being
near a location, having an interface like the bank open, obtaining items, using or
equipping an item, casting a teleport. Routes can be built by hand, but the headline is
**recording**. Turn on record mode, do the run once, and the plugin captures the steps.

The motivating case is a farm run. Recorded once, the route reads:

1. Be near the bank.
2. Bank interface open.
3. Withdraw a teleport item and a sapling.
4. Use the teleport item.
5. Be near the farming patch.

Recorded item steps start specific (the exact sapling withdrawn) but can be **loosened**
to a class of item: any sapling, any allotment seed, any herb seed. A run recorded with
oak saplings then works for any tree run.

Routes can also **loop**. Flag a route as repeating and it restarts from the first step on
finishing the last, with a lap counter. This suits continuous minigames like the Vale
Totem, where the player cycles one short sequence.

While a route runs, an **in-game status panel** drawn in the game window (not the sidebar)
shows the current step and a counter, e.g. `Step 3/5: withdraw a sapling`.

**Why interesting:** routes are the repetitive multi-location chores: farm runs, agility
laps, runecrafting runs, minigame cycles. Saved waypoints answer "where is X". Routes
answer "walk me through this whole thing again". Hand-authoring a fifteen-step run is a
chore; doing the run once with record on is not.

**Not automation.** The plugin records and guides. It never performs actions for the
player. Each step is detected from game events; the player still clicks everything. This
is the design, and a hard line for Plugin Hub acceptance.

**Scope:** large, the biggest item in this file. Sensible build order:

1. Manual routes only. An ordered list of existing waypoints with sequential auto-path on
   arrival, reusing `WaypointPathfinder`'s arrival detection. Shippable on its own.
2. Recording. A recorder service subscribes to game events (movement, widget loads,
   item-container and equipment changes, animations) and turns them into a step list,
   plus an editor to trim noise and loosen steps.
3. Rich step types with the item-class table (saplings, seeds, herbs and the rest),
   looping routes, and the in-game status overlay.

New `route` package (`Route`, `RouteStep` and its subtypes), a recorder service, a
guidance overlay, and route UI shown through the `MultiplexingPluginPanel` the preset
browser already uses. Routes persist alongside the library or in their own file.

**Edge cases:**
- Player does steps out of order or skips one → track the furthest completed step rather
  than demanding strict order, and let a later step's detector pull the route forward.
- A step cannot complete (no sapling in the bank) → allow a manual skip.
- Steps the plugin cannot detect reliably → fall back to a manual "done" button on the
  status panel.
- A route recorded with one teleport setup, run by a different account → loosened
  item-class steps absorb most of it; tile steps after a teleport stay valid.

---

## Death marker

**What:** on death, the plugin auto-creates a waypoint at the death tile and shows the
gravestone countdown next to it. An optional setting paths to it immediately via
shortest-path.

**Why interesting:** recovering a gravestone is a race against its timer, and the moment
of death is the worst time to be fumbling with Mark current location. An automatic marker,
and optionally an automatic path, turns the recovery into one glance.

**Scope:** small-medium. Detect the local player's death and the death location (see
RuneLite's death-location handling; the former Death Indicator plugin tracked this).
Create the waypoint in a dedicated auto-managed slot rather than a user category, with an
expiry tied to the gravestone duration so it is removed once the grave is gone.

**Settings:**
- Toggle: drop a death marker (on / off, default on).
- Toggle: path to the death marker automatically (on / off, default off). Requires
  shortest-path.
- Toggle: keep the marker after the gravestone expires (off by default).

**Edge cases:**
- Safe deaths with no gravestone → still mark, or skip; pick one.
- Dying again before recovering the first grave → replace the marker; the old grave is
  already gone.
- Auto-path with shortest-path unavailable → fall back to just the marker.

---

## "Nearest" landmark actions

**What:** one-click "Path to nearest bank", and the same for altars, anvils and the other
landmark types, without needing a saved waypoint. Surfaced as buttons or overflow-menu
items.

**Why interesting:** the most common navigation need is a generic one ("I need a bank"),
and it shouldn't require having saved that bank first. It is useful the moment the plugin
is installed, before the user has built any library.

**Scope:** small for the mechanism. The bundled wiki-sourced bbox dataset (banks, altars,
anvils, furnaces, looms, spinning wheels, tanners, spirit trees, bank chests, charter ships,
fairy rings, slayer masters, unique landmarks) gives the per-type coverage this needs.
`BboxIndex` currently merges every TSV into one plane-keyed list and only exposes a
containment lookup; for "nearest of type X" it would need to retain the source category per
entry (or load and query each file separately) and add a distance-based query. From there,
pick the nearest entry of the requested type by straight-line distance from the player, then
hand it to `WaypointPathfinder` (shortest-path computes the real route from there).

**Edge cases:**
- Nearest by straight-line distance can pick a candidate that is slow to actually reach.
  Acceptable as a first cut, since shortest-path still routes properly once chosen.
- Members-only banks for an F2P account, instanced areas, plane differences.

---

## Right-click NPCs and objects to save

**What:** holding Shift, right-clicking an NPC or a game object adds a "Save as Waypoint"
entry that captures that entity's tile and names the waypoint after it. Config-gated and
off by default, like the existing tile capture.

**Why interesting:** plenty of destinations are really "that NPC" or "that object": a
bank booth, an altar, a quest giver. Capturing the entity is more natural than the tile
under it, and the entity's name is a free, accurate default.

**Scope:** small-medium. Extends `WaypointMenuHandler` with NPC and object branches in
`onMenuEntryAdded`, gated on Shift held plus a new config toggle (a sibling to
`tileRightClickEnabled`). The tile comes from the NPC's or object's world location, the
name from the menu target.

**Settings:**
- Toggle: right-click NPCs and objects to save (on / off, default off).

**Edge cases:**
- NPCs move; the captured tile is a snapshot at capture time.
- Multi-tile objects → use the clicked tile or the south-west tile.

---

## Pinned waypoints

**What:** a pin toggle that floats chosen waypoints into a synthetic "Pinned" section at
the top of the panel, regardless of their category.

**Why interesting:** a handful of waypoints get used constantly: home, the main bank, the
current grind. Hunting for them inside categories every time is friction a pin removes.

**Scope:** small. An optional `pinned` flag on `Waypoint` (a new optional field; Gson
tolerates its absence in old files, so no migration needed). The panel renders a synthetic
Pinned section first, similar to the planned Nearby section. The waypoint still lives in
its real category; pinning is just an extra view. The pin control sits on the row or in
the inline editor.

**Edge cases:**
- Pinned section hidden when empty.
- Synthetic section, so no drag-drop or rename / delete on it, matching how Nearby behaves.

---

## Per-category colour

**What:** an optional colour per category, alongside the existing optional icon. Drives a
coloured accent on the category header and its rows.

**Why interesting:** icons help, but a colour is faster to scan down a long list, and
colour-coding by purpose (danger, skilling, bossing) is an easy mental model.

**Scope:** small. An optional colour field on `Category`, stored as a packed RGB int. A
colour choice added to the category menu next to "Set icon...", constrained to a small
palette that reads well against the dark theme rather than a free colour wheel. Applied in
`CategorySection`.

**Edge cases:**
- Contrast against the RuneLite dark panel; a fixed palette avoids unreadable picks.

---

## Wilderness pathing confirmation

**What:** pathing to a waypoint inside the Wilderness shows a confirm dialog first. A
setting disables it.

**Why interesting:** accidentally walking into the Wilderness can cost the player their
gear. A speed bump on Play for known-dangerous destinations is cheap insurance.

**Scope:** small. The Wilderness bounds are a known region; check the waypoint's tile
against them in the PLAY branch of `WaypointerPanel.handleRowAction`. Optionally tag
wilderness waypoints with a skull glyph in the row so it's clear before clicking.

**Settings:**
- Toggle: confirm before pathing into the Wilderness (on / off, default on).

---

## Undo for destructive actions

**What:** a transient Undo affordance after a destructive action: delete waypoint, delete
category, reset library.

**Why interesting:** deletes commit straight after their confirm dialog. A misclick, or
picking the wrong option in the category-delete dialog, loses data. The `.bak` file guards
against file corruption, not against a delete the user regrets a second later.

**Scope:** small-medium. Hold the last destructive operation's removed objects in memory
and show an Undo control (a small bar in the panel, or a reused banner slot). Undo
re-inserts them. Single-level undo is enough; clear the buffer on the next mutation.

**Edge cases:**
- Undo after the debounced save already persisted the delete → fine, undo is just another
  mutation that re-saves.
- Undoing a category delete restores its waypoints and their `categoryId`.
- The buffer does not survive a client restart, which is acceptable.

---

## Selective and per-category sharing

**What:** an export picker. When exporting to a file or a share code, choose which
categories (or individual waypoints) to include, with an option to carry the category
definitions or flatten everything into Uncategorized. A per-category share code falls out
of this for free: it is just an export with one category selected.

**Why interesting:** sharing today is all-or-one, the whole library (`WPL1:`) or a single
waypoint (`WP1:`). People want to share a themed set, "my boss waypoints", without dumping
their entire library or copying waypoints one at a time.

**Scope:** medium. No new code format needed: a selected subset is just a `Library` fed to
the existing `encodeLibrary` and file export. The work is the picker dialog (categories
with checkboxes) and building the filtered `Library`. The carry-category option decides
whether exported waypoints keep their category or land in Uncategorized; `importMerge`
already rebinds categories by name on the way back in.

**Edge cases:**
- A subset that references the Uncategorized sentinel.
- Exporting an empty selection → disable the confirm.

**Affected entry points:** `LibraryFileIo`, `OverflowMenu`, and the export paths that
currently call `WaypointShareCodec.encodeLibrary`.

---

## Per-profile libraries

**What:** key the waypoint library per RuneLite profile, so different accounts or profiles
keep separate waypoint sets instead of sharing one global library.

**Why interesting:** an ironman and a main want different waypoints. RuneLite already
segments config by profile, so a global library breaks that expectation.

**Scope:** medium. The library lives at `~/.runelite/waypointer/library.json` today.
Persist one per profile instead (`library-<profileKey>.json`, or a per-profile subfolder),
resolving the path from the active profile. On a profile switch, reload the matching
library, which means `WaypointStore.bootstrap` runs again, the panel rebuilds, and the
debounced-save target path updates. An existing global `library.json` migrates to the
default profile on first run.

**Open question:** which profile concept to key on. RuneLite has the local config profile
and the per-account RS profile; confirm which is the right granularity before building
this.

**Edge cases:**
- A profile switch mid-session has to swap the in-memory library and cancel any pending
  save against the old path.
- Sharing waypoints between profiles stays out of scope; share codes already cover it.

---

## Panel UX polish

A cluster of small panel improvements, grouped because none is big enough for its own
entry:

- **Count badge on collapsed categories.** A collapsed category header should show its
  waypoint count, the way `PresetSection` already does. Today you have to expand a
  category to see whether it holds anything. Tiny.
- **Expand / collapse all.** One control in the toolbar row or overflow menu to fold or
  unfold every category at once. Tiny.
- **Inline distance per row.** When logged in, show each waypoint's tile distance from the
  player, refreshed on `GameTick`. Reuses the distance calc the Nearby section already
  needs. Small.
- **Keyboard navigation.** Arrow keys move a selection highlight through rows, Enter
  paths, Space expands the inline editor. Nice to have, not a priority.
- **Search syntax.** Prefixes that scope a search, e.g. `cat:` to match category names
  only, plus coordinate search. Nice to have, not a priority.

---

## Plugin Hub submission

Once we're ready to make it real:

- Verify the hub's verifier passes (`grep -rE "WidgetInfo|WidgetID|new OkHttpClient|new Gson\(\)"`
  on src/main/java; already clean).
- Make the github repo public.
- Open a PR to `runelite/plugin-hub` with a manifest pointing at the repo + commit hash.
- Replace the placeholder icon with a final design (we have an Icons8 logo with attribution
  in the README; verify the license fits Plugin Hub's redistribution rules).
