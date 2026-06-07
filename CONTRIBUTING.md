# Contributing to Waypointer

Most contributions to Waypointer are data, not code. The preset waypoint sets and the
landmark locations that auto-name your captures both live in plain data files, so adding a
herb patch or fixing a bank that sits a tile off does not need any Java.

This guide covers the two common cases first (preset waypoints, then landmark locations), then
the one case that does touch code (a brand-new landmark type), then the house rules for any
pull request.

For build, run, and architecture details, see [DEVELOPER.md](DEVELOPER.md).

## Contents

- [Add or fix preset waypoints](#add-or-fix-preset-waypoints)
- [Add or fix a landmark location](#add-or-fix-a-landmark-location)
- [Add a new landmark type](#add-a-new-landmark-type)
- [House rules](#house-rules)

---

## Add or fix preset waypoints

Preset waypoints are the curated sets behind the **Presets** tab (Slayer masters, herb
patches, bosses, and so on). They live in one file:

```
src/main/resources/com/waypointer/preset-waypoints.json
```

Each entry under `presets` is one set. A set has a `category`, an optional `description`, an
optional category `icon` (a RuneLite sprite id), and a list of `waypoints`:

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

A waypoint needs a `name` and `x` / `y` / `plane` world coordinates. Its `description` is
optional and becomes the waypoint's notes once a player adds it.

To add a waypoint, drop another object into the right set's `waypoints` list. To add a whole
new set, add another object to `presets`. To correct a tile, edit its `x` / `y` / `plane`.

To read a tile's coordinates off the live game, run the client with RuneLite's developer
tools on (`./gradlew run` sets `--developer-mode`) and read the coordinate under your
character, or look the spot up on the OSRS Wiki. Type the numbers into the JSON by hand.

When you are done, run the tests:

```bash
./gradlew test
```

`PresetFileValidityTest` parses the real file on every build. It rejects a blank category, a
blank waypoint name, an `x` or `y` outside `0..32767`, or a `plane` outside `0..3`, so a typo
fails the build instead of shipping.

## Add or fix a landmark location

Landmarks are the named tiles Waypointer matches against when you capture a waypoint, so a
tile inside a bank fills in the bank's name on its own. They also feed the nearest-landmark
bar. The data is a set of tab-separated files, one per type, under:

```
src/main/resources/com/waypointer/landmarks/
```

Each row is a bounding box and a name. The five numbers are space-separated, then a single tab,
then the name:

```
# x1 y1 x2 y2 plane	name
3089 3265 3095 3271 0	Falador Slayer Master
```

`x1 y1` is the south-west corner of the box, `x2 y2` the north-east corner, and `plane` the
floor. Any tile inside the box counts as "at" that landmark. Keep boxes tight: when two boxes
overlap, the smaller one wins, so an over-wide box can swallow a neighbour. Lines starting with
`#` are comments and are left alone.

Edit the `.tsv` directly. Find the right file by type (banks in `banks-bboxes.tsv`, slayer
masters in `slayer-masters-bboxes.tsv`, and so on), add or change a row, and keep the
`space-separated numbers` + `tab` + `name` shape. To read coordinates off the live game, run
the client with RuneLite's developer tools on (`./gradlew run` sets `--developer-mode`) and
read the tile under your character, or use the OSRS Wiki. `BboxIndexTest` and
`LandmarkLookupTest` cover the loader, so a malformed row will not parse.

## Add a new landmark type

This is the one change that needs Java. Say you want a new "Agility course" type that does not
fold into an existing one. Three places line up, and they have to stay in sync:

1. **The data file.** Add `agility-courses-bboxes.tsv` under `landmarks/`, same row format as
   the rest.
2. **The loader.** Register the file in `service/BboxIndex.java` by adding a `ResourceEntry`
   to the `RESOURCES` array, paired with its type.
3. **The enum.** Add a value to `service/LandmarkType.java` with a display name. This is what
   the nearest-landmark bar shows.

Run `./gradlew test` and the bbox tests will confirm the new file loads.

## House rules

- **Run `./gradlew test` before you open a pull request** and keep it green. The data tests
  catch most bad edits on their own.
- **One topic per pull request.** A bank fix and a new preset set are two PRs, not one.
- **Match the surrounding style** in any code you touch: four-space indentation, no wildcard
  imports, Lombok where the neighbouring code already uses it, logging through `@Slf4j`.
- **For a new feature, open an issue first** so we can agree on the shape before you write it.
  Data fixes can go straight to a PR.
- **Source your landmark and preset coordinates** from in-game or the
  [OSRS Wiki](https://oldschool.runescape.wiki/), and note the source in the PR if it is not
  obvious.

Thanks for helping fill out the map.
