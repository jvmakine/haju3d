package fi.haju.haju3d.protocol.world;

import java.io.Serializable;

import fi.haju.haju3d.protocol.Vector3i;


public final class Chunk implements Serializable {
  private static final long serialVersionUID = 1L;

  private final ObjArray3d<Tile> tiles;
  private final FloatArray3d colors;
  private final int seed;
  private final Vector3i position;

  public Chunk(int width, int height, int depth, int seed, Vector3i position) {
    this.seed = seed;
    this.position = position;
    this.tiles = new ObjArray3d<Tile>(width, height, depth, Tile.AIR);
    this.colors = new FloatArray3d(width, height, depth);
  }
  
  public void fill(Tile value) {
    tiles.fill(value);
  }

  public void set(int x, int y, int z, Tile value) {
    tiles.set(x, y, z, value);
  }
  
  public void setColor(int x, int y, int z, float color) {
    colors.set(x, y, z, color);
  }
  
  public boolean isInside(int x, int y, int z) {
    return tiles.isInside(x, y, z);
  }

  public Tile get(int x, int y, int z) {
    return tiles.get(x, y, z);
  }
  
  public float getColor(int x, int y, int z) {
    return colors.get(x, y, z);
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
