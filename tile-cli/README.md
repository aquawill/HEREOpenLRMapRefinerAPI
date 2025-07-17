# tile-cli

A simple cross-platform CLI tool written in Java for converting HERE tile coordinates, quadkeys, and WGS84 lat/lon.  
Built for debugging HERE OCM/MapTile data or integrating with tile-based map pipelines.

---

## Commands

### 1. `from-wgs84-to-tile-coords <lat> <lng> <level>`

Convert WGS84 coordinates to:
- HERE quadkey
- tile row & column
- inner tile X/Y (25-bit world coordinate offset)

**Example**:

```bash
java -jar tile-cli.jar from-wgs84-to-tile-coords 25.033 121.5654 14
````
```json
{
  "quadkey": "389114714",
  "tile_row": 5235,
  "tile_column": 13724,
  "tile_inner_x": 1180,
  "tile_inner_y": 572
}
```
---

### 2. `from-tile-coords-to-wgs84 <tile_column> <tile_row> <x> <y> <level>`

Convert tile coordinates and X/Y pixel offset (at given level) back to latitude/longitude.

**Example**:

```bash
java -jar tile-cli.jar from-tile-coords-to-wgs84 13724 5235 1180 572 14
```
```json
{
  "lat": 25.03299236,
  "lng": 121.56539440
}
```
---

### 3. `from-quadkey-coords-to-wgs84 <quadkey> <x> <y>`

Given a HERE quadkey and inner-tile X/Y offset, compute WGS84 coordinate.

**Example**:

```bash
java -jar tile-cli.jar from-quadkey-coords-to-wgs84 389114714 1180 572
```
```json
{
  "lat": 25.03299236,
  "lng": 121.56539440,
  "tile_level": 14,
  "tile_row": 5235,
  "tile_column": 13724
}
```
---

### 4. `from-quadkey-to-tile <quadkey>`

Get tile column, tile row, and level from a quadkey.

**Example**:

```bash
java -jar tile-cli.jar from-quadkey-to-tile 389114714
```
```json
{
  "tile_row": 5235,
  "tile_column": 13724,
  "tile_level": 14
}
```
---

### 5. `from-tile-to-quadkey <tile_column> <tile_row> <level>`

Convert tile row and column to HERE quadkey.

**Example**:

```bash
java -jar tile-cli.jar from-tile-to-quadkey 13724 5235 14
```
```json
{
  "quadkey": "389114714"
}
```
---

## Build

```bash
mvn clean install
```

Produces:

```
target/tile-cli-<version>-jar-with-dependencies.jar
```

---

## Usage

```bash
java -jar tile-cli-1.1.0-jar-with-dependencies.jar <command> [args...]
```

---

## Requirements

* Java 8+
* HERE Data SDK (`HereQuadFactory`, `HereQuad` classes)

---

## License

MIT License
