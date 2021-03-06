package fi.haju.haju3d.protocol.world;

import fi.haju.haju3d.util.noise.InterpolationUtil;

import java.io.Serializable;
import java.util.Random;

public final class FloatArray3d implements Serializable {
  private static final long serialVersionUID = 1L;

  private final float[] data;
  private final int width;
  private final int height;
  private final int depth;

  public static interface Initializer {
    float getValue(int x, int y, int z);
  }

  public FloatArray3d(int width, int height, int depth) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.data = new float[width * height * depth];
  }

  public FloatArray3d(int width, int height, int depth, Initializer initializer) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.data = new float[width * height * depth];
    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        for (int z = 0; z < depth; ++z) {
          set(x, y, z, initializer.getValue(x, y, z));
        }
      }
    }
  }
  
  public FloatArray3d(int width, int height, int depth, Random random) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    this.data = new float[width * height * depth];
    for (int x = 0; x < width; ++x) {
      for (int y = 0; y < height; ++y) {
        for (int z = 0; z < depth; ++z) {
          set(x, y, z, 2.0f * random.nextFloat() - 1.0f);
        }
      }
    }
  }

  public void set(int x, int y, int z, float value) {
    data[getIndex(x, y, z)] = value;
  }

  public void add(int x, int y, int z, float value) {
    data[getIndex(x, y, z)] += value;
  }

  private int getIndex(int x, int y, int z) {
    return x + y * width + z * width * height;
  }

  public boolean isInside(int x, int y, int z) {
    return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
  }

  public float get(int x, int y, int z) {
    return data[getIndex(x, y, z)];
  }

  public float getInterpolated(float tx, float ty, float tz) {
    int tw = width;
    int td = depth;

    int x = (int) tx;
    int y = (int) ty;
    int z = (int) tz;

    float xt = tx - x;
    float yt = ty - y;
    float zt = tz - z;

    return InterpolationUtil.interpolateLinear3d(
        xt, yt, zt,
        data[x + y * tw + z * tw * td],
        data[x + 1 + y * tw + z * tw * td],
        data[x + y * tw + tw + z * tw * td],
        data[x + 1 + y * tw + tw + z * tw * td],
        data[x + y * tw + (z + 1) * tw * td],
        data[x + 1 + y * tw + (z + 1) * tw * td],
        data[x + y * tw + tw + (z + 1) * tw * td],
        data[x + 1 + y * tw + tw + (z + 1) * tw * td]);
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
