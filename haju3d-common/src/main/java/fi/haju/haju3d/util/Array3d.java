package fi.haju.haju3d.util;

import java.io.Serializable;
import java.lang.reflect.Array;

import fi.haju.haju3d.protocol.coordinate.Vector3i;

public final class Array3d<T> implements Serializable {
  private static final long serialVersionUID = 1L;

  private final T[] data;
  private final int width;
  private final int height;
  private final int depth;

  @SuppressWarnings("unchecked")
  public Array3d(int width, int height, int depth, T initial) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    int size = width * height * depth;
    this.data = (T[])Array.newInstance(initial.getClass(), size);
    for(int i = 0; i < size; ++i) data[i] = initial;
  }

  public void set(int x, int y, int z, T value) {
    data[getIndex(x, y, z)] = value;
  }
  
  public void set(Vector3i pos, T value) {
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

  public T get(int x, int y, int z) {
    return data[getIndex(x, y, z)];
  }
  
  public T get(Vector3i pos) {
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
