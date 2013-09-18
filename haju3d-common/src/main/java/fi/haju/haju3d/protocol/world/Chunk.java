package fi.haju.haju3d.protocol.world;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import fi.haju.haju3d.protocol.Vector3i;


public final class Chunk implements Serializable {
  private static final long serialVersionUID = 3L;

  private final ByteArray3d tiles;
  private final ByteArray3d colors;
  private final int seed;
  private final Vector3i position;
  private final Tile tile;
  
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
  }
  
  public void set(int x, int y, int z, Tile value) {
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
  
  public float getColor(int x, int y, int z) {
    return tile != null ? 0.0f : colors.get(x, y, z) / 127f;
  }

  public int getWidth() {
    return tiles.getWidth();
  }

  public int getHeight() {
    return tiles.getHeight();
  }

  public int getDepth() {
    return tiles.getDepth();
  }

  public int getSeed() {
    return seed;
  }

  public Vector3i getPosition() {
    return position;
  }
}
