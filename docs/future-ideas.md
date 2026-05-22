# Future Ideas

Features deferred past v1. Not commitments. Each entry roughs out what it is, why it might
be worth doing, and how big the work looks.


---

## "Pathing to..." banner

**What:** a small banner at the top of the panel showing which waypoint shortest-path is
currently routing to, with a `Stop` button. Shows up only when a path is active.

**Example:** `→ Pathing to Vorkath  [✕ Stop]`

**Why interesting:** today the user clicks Play and shortest-path silently draws a path on
the world map and minimap. The Waypointer panel itself gives no indication of which waypoint
is active or how to cancel without diving into the overflow menu. This is the one missing
feedback loop in the post-capture UX.

**Scope:** small-medium. Subscribe to shortest-path's `transports` `PluginMessage` (it
already posts these when a path is active; see `ShortestPathPlugin.postPluginMessages`).
Keep the most recent target packed-int in `WaypointPathfinder`, look up the matching
waypoint, render at the top of the panel body. Clear when shortest-path posts an empty path
or we send our own `clear` message.

**Edge cases:**
- Path destinations that don't match any saved waypoint (e.g. user used shortest-path's own
  right-click `Set Target` independently). Banner should hide or show "Pathing to (3162, 3486)".
- Shortest-path being unavailable mid-session. Listen for the existing availability flip and
  clear the banner.

---

## Smarter capture default names

**What:** when capturing a new waypoint, default the name to a region-aware string instead of
`(3162, 3486)`. So Mark current location at the GE produces `Grand Exchange`
rather than the raw coords.

**Why interesting:** raw coords are unscannable. Users almost always rename, so the default
is wasted. A region-name prefix turns "did I really visit this place?" into "yes, that's the
one" without typing anything.

**Scope:** small. Two paths to investigate:
1. RuneLite's bundled `Region` enum / `WorldArea` lookups: fast and offline, but coverage may
   be incomplete (covers major cities, may miss smaller landmarks).
2. The OSRS Wiki's location data, cached locally on first use: best coverage but adds an
   HTTP dependency and a one-time download.

Start with (1); fall back to `(x, y)` when no region match. (2) becomes worth revisiting if
the region table feels too sparse.

**Affected entry points:**
- `WaypointCapture.captureFromPackedPoint` (used by world-map right-click and Mark current
  location).
- `CaptureDialog`'s default name (currently builds `(x, y)` via `WorldPointPacker`; would
  pull from the same helper).


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

## Plugin Hub submission

Once we're ready to make it real:

- Verify the hub's verifier passes (`grep -rE "WidgetInfo|WidgetID|new OkHttpClient|new Gson\(\)"`
  on src/main/java; already clean).
- Make the github repo public.
- Open a PR to `runelite/plugin-hub` with a manifest pointing at the repo + commit hash.
- Replace the placeholder icon with a final design (we have an Icons8 logo with attribution
  in the README; verify the license fits Plugin Hub's redistribution rules).
