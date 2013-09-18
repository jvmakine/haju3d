package fi.haju.haju3d.protocol.world;

import java.io.Serializable;

import fi.haju.haju3d.protocol.Vector3i;


public final class Chunk implements Serializable {
  private static final long serialVersionUID = 1L;

  private final ObjArray3d<Tile> data;
  private final int seed;
  private final Vector3i position;

  public Chunk(int width, int height, int depth, int seed, Vector3i position) {
    this.seed = seed;
    this.position = position;
    this.data = new ObjArray3d<Tile>(width, height, depth, Tile.AIR);
  }
  
  public void fill(Tile value) {
    data.fill(value);
  }

  public void set(int x, int y, int z, Tile value) {
    data.set(x, y, z, value);
  }
  
  public boolean isInside(int x, int y, int z) {
    return data.isInside(x, y, z);
  }

  public Tile get(int x, int y, int z) {
    return data.get(x, y, z);
  }

  public int getWidth() {
    return data.getWidth();
  }

  public int getHeight() {
    return data.getHeight();
  }

  public int getDepth() {
    return data.getDepth();
  }

  public int getSeed() {
    return seed;
  }

  public Vector3i getPosition() {
    return position;
  }
}
