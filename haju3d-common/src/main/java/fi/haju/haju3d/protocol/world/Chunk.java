package fi.haju.haju3d.protocol.world;

import com.google.common.collect.Lists;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class Chunk implements Serializable {
  private static final long serialVersionUID = 5L;

  private ByteArray3d tiles;
  private ByteArray3d colors;
  private final int seed;
  private final ChunkPosition position;
  private Tile tile;
  private final int size;

  private final static Map<Byte, Tile> byteToTile = new HashMap<>();
  private final static Map<Tile, Byte> tileToByte = new HashMap<>();

  static {
    for (Tile t : Tile.values()) {
      byteToTile.put((byte) t.ordinal(), t);
      tileToByte.put(t, (byte) t.ordinal());
    }
  }

  public static interface GetValue {
    Tile getValue(int x, int y, int z);
  }

  public Chunk(int size, int seed, ChunkPosition position) {
    this.seed = seed;
    this.position = position;
    this.tiles = new ByteArray3d(size, size, size);
    this.colors = new ByteArray3d(size, size, size);
    this.tile = null;
    this.size = size;
  }

  /**
   * Chunk that has constant tile value (typically AIR or GROUND).
   */
  public Chunk(int size, int seed, ChunkPosition position, Tile tile) {
    this.seed = seed;
    this.position = position;
    this.tiles = null;
    this.colors = null;
    this.tile = tile;
    this.size = size;
  }

  public void set(GetValue getValue) {
    for (int x = 0; x < size; x++) {
      for (int y = 0; y < size; y++) {
        for (int z = 0; z < size; z++) {
          set(x, y, z, getValue.getValue(x, y, z));
        }
      }
    }
  }

  public void set(int x, int y, int z, Tile value) {
    if (tiles == null) { //Changing a constant chunk -> convert
      this.tiles = new ByteArray3d(size, size, size);
      this.colors = new ByteArray3d(size, size, size);
      tiles.fill(tileToByte.get(tile));
      tile = null;
    }
    tiles.set(x, y, z, tileToByte.get(value));
  }

  public void setColor(int x, int y, int z, float color) {
    colors.set(x, y, z, (byte) (color * 127f));
  }

  public boolean isInside(int x, int y, int z) {
    return x >= 0 && x < size && y >= 0 && y < size && z >= 0 && z < size;
  }

  public boolean isWithin(LocalTilePosition pos) {
    return
        pos.x >= 0 && pos.x < size
            && pos.y >= 0 && pos.y < size
            && pos.z >= 0 && pos.z < size;
  }

  public Tile get(int x, int y, int z) {
    return tile != null ? tile : byteToTile.get(tiles.get(x, y, z));
  }

  public Tile get(LocalTilePosition pos) {
    return tile != null ? tile : byteToTile.get(tiles.get(pos.x, pos.y, pos.z));
  }

  public float getColor(int x, int y, int z) {
    return tile != null ? 0.0f : colors.get(x, y, z) / 127f;
  }

  public boolean hasLight() {
    return tile == null;
  }

  public int getSize() {
    return size;
  }

  public int getSeed() {
    return seed;
  }

  public ChunkPosition getPosition() {
    return position;
  }

  public List<LocalTilePosition> getNeighbours(LocalTilePosition pos) {
    if (!isWithin(pos)) {
      throw new IllegalArgumentException(pos + " is not within the chunk, chunkSize = " + size);
    }
    List<LocalTilePosition> surroundings = Lists.newArrayList(
        new LocalTilePosition(pos.x + 1, pos.y, pos.z),
        new LocalTilePosition(pos.x - 1, pos.y, pos.z),
        new LocalTilePosition(pos.x, pos.y + 1, pos.z),
        new LocalTilePosition(pos.x, pos.y - 1, pos.z),
        new LocalTilePosition(pos.x, pos.y, pos.z + 1),
        new LocalTilePosition(pos.x, pos.y, pos.z - 1)
    );
    List<LocalTilePosition> result = Lists.newArrayList();
    for (LocalTilePosition sur : surroundings) {
      if (isWithin(sur)) {
        result.add(sur);
      }
    }
    return result;
  }

}
