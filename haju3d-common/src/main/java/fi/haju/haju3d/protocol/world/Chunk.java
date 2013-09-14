package fi.haju.haju3d.protocol.world;

import java.io.Serializable;

import fi.haju.haju3d.protocol.Vector3i;


public final class Chunk implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private Tile[] data;
  private final int width;
  private final int height;
  private final int depth;
  private final int seed;
  private final Vector3i position;

  public Chunk(int width, int height, int depth, int seed, Vector3i position) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.seed = seed;
    this.position = position;

    int n = width * height * depth;
    this.data = new Tile[n];
    for (int i = 0; i < n; i++) {
      data[i] = Tile.AIR;
    }
  }

  public void set(int x, int y, int z, Tile value) {
    if (isInside(x, y, z)) {
      data[getIndex(x, y, z)] = value;
    }
  }

  private int getIndex(int x, int y, int z) {
    return x + y * width + z * width * height;
  }

  private boolean isInside(int x, int y, int z) {
    return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
  }

  public Tile get(int x, int y, int z) {
    if (isInside(x, y, z)) {
      return data[getIndex(x, y, z)];
    } else {
      return Tile.AIR;
    }
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
}