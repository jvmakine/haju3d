package fi.haju.haju3d.protocol.world;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 3d array of objects.
 * <p/>
 * For enumerated types, prefer ByteArray3d which uses 1/4 or 1/8 the amount of memory.
 */
public final class ObjArray3d<T> implements Serializable {
  private static final long serialVersionUID = 1L;

  private final T[] data;
  private final int width;
  private final int height;
  private final int depth;

  @SuppressWarnings("unchecked")
  public ObjArray3d(int width, int height, int depth, T initialValue) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.data = (T[]) new Object[width * height * depth];
    fill(initialValue);
  }

  public void fill(T value) {
    Arrays.fill(data, 0, data.length, value);
  }

  public void set(int x, int y, int z, T value) {
    data[getIndex(x, y, z)] = value;
  }

  private int getIndex(int x, int y, int z) {
    return x + y * width + z * width * height;
  }

  public boolean isInside(int x, int y, int z) {
    return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
  }

  public T get(int x, int y, int z) {
    return data[getIndex(x, y, z)];
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
