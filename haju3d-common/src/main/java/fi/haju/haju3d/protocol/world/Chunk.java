package fi.haju.haju3d.protocol.world;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import fi.haju.haju3d.protocol.Vector3i;


public final class Chunk implements Serializable {
  private static final long serialVersionUID = 3L;

  private ByteArray3d tiles;
  private ByteArray3d colors;
  private final int seed;
  private final Vector3i position;
  private Tile tile;
  private final int width;
  private final int height;
  private final int depth;
  
  private final static Map<Byte, Tile> byteToTile = new HashMap<>();
  private final static Map<Tile, Byte> tileToByte = new HashMap<>();
  static {
    for (Tile t : Tile.values()) {
      byteToTile.put((byte) t.ordinal(), t);
      tileToByte.put(t, (byte) t.ordinal());
    }
  }
  
  public Chunk(int width, int height, int depth, int seed, Vector3i position) {
    this.seed = seed;
    this.position = position;
    this.tiles = new ByteArray3d(width, height, depth);
    this.colors = new ByteArray3d(width, height, depth);
    this.tile = null;
    this.width = width;
    this.height = height;
    this.depth = depth;
  }
  
  /**
   * Chunk that has constant tile value (typically AIR or GROUND).
   */
  public Chunk(int width, int height, int depth, int seed, Vector3i position, Tile tile) {
    this.seed = seed;
    this.position = position;
    this.tiles = null;
    this.colors = null;
    this.tile = tile;
    this.width = width;
    this.height = height;
    this.depth = depth;
  }
  
  public void set(int x, int y, int z, Tile value) {
    if(tiles == null) { //Changing a constant chunk -> convert
      this.tiles = new ByteArray3d(getWidth(), getHeight(), getDepth());
      this.colors = new ByteArray3d(getWidth(), getHeight(), getDepth());
      tiles.fill(tileToByte.get(tile));
      tile = null;
    }
    tiles.set(x, y, z, tileToByte.get(value));
  }
  
  public void setColor(int x, int y, int z, float color) {
    colors.set(x, y, z, (byte)(color * 127f));
  }
  
  public boolean isInside(int x, int y, int z) {
    return tiles.isInside(x, y, z);
  }

  public Tile get(int x, int y, int z) {
    return tile != null ? tile : byteToTile.get(tiles.get(x, y, z));
  }
  
  public Tile get(Vector3i pos) {
    return tile != null ? tile : byteToTile.get(tiles.get(pos.x, pos.y, pos.z));
  }
  
  public float getColor(int x, int y, int z) {
    return tile != null ? 0.0f : colors.get(x, y, z) / 127f;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getDepth() {
    return depth;
  }

  public int getSeed() {
    return seed;
  }

  public Vector3i getPosition() {
    return position;
  }

  public boolean isWithin(Vector3i pos) {
    return
        pos.x > 0 && pos.x < getWidth()
        && pos.y > 0 && pos.y < getHeight()
        && pos.z > 0 && pos.z < getDepth();
  }
}
