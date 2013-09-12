package fi.haju.haju3d.protocol.world;

import java.io.Serializable;


public final class Chunk implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private Tile[] data;
  int width;
  int height;
  int depth;
  int seed;

  public Chunk(int width, int height, int depth, int seed) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.seed = seed;

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
  
}