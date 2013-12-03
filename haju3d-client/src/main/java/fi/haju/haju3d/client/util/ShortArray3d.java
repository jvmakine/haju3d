package fi.haju.haju3d.client.util;

import fi.haju.haju3d.protocol.coordinate.Vector3i;

import java.util.Arrays;

public final class ShortArray3d {
  private final short[] data;
  public final int width;
  public final int height;
  public final int depth;

  public ShortArray3d(int width, int height, int depth) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.data = new short[width * height * depth];
  }

  public void fill(short value) {
    Arrays.fill(data, 0, data.length, value);
  }

  public void set(Vector3i pos, short value) {
    data[getIndex(pos.x, pos.y, pos.z)] = value;
  }

  private int getIndex(int x, int y, int z) {
    return x + z * width + y * width * depth;
  }

  public boolean isInside(int x, int y, int z) {
    return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
  }

  public boolean isInside(Vector3i pos) {
    return pos.x >= 0 && pos.x < width && pos.y >= 0 && pos.y < height && pos.z >= 0 && pos.z < depth;
  }

  public short get(Vector3i pos) {
    return data[getIndex(pos.x, pos.y, pos.z)];
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

  public short[] getData() {
    return data;
  }
}
