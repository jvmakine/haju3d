package fi.haju.haju3d.protocol.world;

import java.io.Serializable;
import java.util.Arrays;

import fi.haju.haju3d.protocol.coordinate.Vector3i;

public final class ByteArray3d implements Serializable {
  private static final long serialVersionUID = 1L;

  private final byte[] data;
  public final int width;
  public final int height;
  public final int depth;

  public ByteArray3d(int width, int height, int depth) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.data = new byte[width * height * depth];
  }

  public void fill(byte value) {
    Arrays.fill(data, 0, data.length, value);
  }

  public void set(int x, int y, int z, byte value) {
    data[getIndex(x, y, z)] = value;
  }
  
  public void set(Vector3i pos, byte value) {
    data[getIndex(pos.x, pos.y, pos.z)] = value;
  }

  private int getIndex(int x, int y, int z) {
    return x + y * width + z * width * height;
  }

  public boolean isInside(int x, int y, int z) {
    return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
  }
  
  public boolean isInside(Vector3i pos) {
    return pos.x >= 0 && pos.x < width && pos.y >= 0 && pos.y < height && pos.z >= 0 && pos.z < depth;
  }

  public byte get(int x, int y, int z) {
    return data[getIndex(x, y, z)];
  }
  
  public byte get(Vector3i pos) {
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
}
