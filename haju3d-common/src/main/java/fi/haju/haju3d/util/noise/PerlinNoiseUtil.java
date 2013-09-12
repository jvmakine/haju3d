package fi.haju.haju3d.util.noise;

import java.util.Random;

public final class PerlinNoiseUtil {
  
  private PerlinNoiseUtil() {
  }
  
  public static float[] make3dPerlinNoise(long seed, int w, int h, int d) {
    Random random = new Random(seed);
    float[] data = new float[w * h * d];
    for (int sc = 4; sc != 128; sc *= 2) {
      add3dNoise(random, data, w, h, d, sc, (float)Math.pow(0.5f * sc * 1.0f, 1.0f));
    }
    return data;
  }
  
  public static float interpolateLinear(float t, float v1, float v2) {
    return v1 + (v2 - v1) * t;
  }

  public static float interpolateLinear3d(
      float xt, float yt, float zt,
      float n1, float n2, float n3, float n4, float n5, float n6, float n7, float n8) {

    float z1 = interpolateLinear2d(xt, yt, n1, n2, n3, n4);
    float z2 = interpolateLinear2d(xt, yt, n5, n6, n7, n8);
    return interpolateLinear(zt, z1, z2);
  }
  
  public static float interpolateLinear2d(
      float xt, float yt, float n1, float n2, float n3, float n4) {
    float x1 = interpolateLinear(xt, n1, n2);
    float x2 = interpolateLinear(xt, n3, n4);
    return interpolateLinear(yt, x1, x2);
  }
  
  private static void add3dNoise(Random random, float[] data, int w, int h, int d, int scale, float amp) {
    int nw = w / scale + 2;
    int nh = h / scale + 2;
    int nd = d / scale + 2;
    int n = nw * nh * nd;
    float noise[] = new float[n];
    for (int i = 0; i < n; i++) {
      noise[i] = (float) (random.nextDouble() - 0.5) * amp;
    }

    int nwh = nw * nh;

    for (int z = 0; z < d; z++) {
      float zt = (float) (z % scale) / scale;
      int zs = z / scale;
      for (int y = 0; y < h; y++) {
        float yt = (float) (y % scale) / scale;
        int ys = y / scale;
        for (int x = 0; x < w; x++) {
          float xt = (float) (x % scale) / scale;
          int xs = x / scale;

          float n1 = noise[xs + ys * nw + zs * nwh];
          float n2 = noise[xs + 1 + ys * nw + zs * nwh];
          float n3 = noise[xs + ys * nw + nw + zs * nwh];
          float n4 = noise[xs + 1 + ys * nw + nw + zs * nwh];

          float n5 = noise[xs + ys * nw + zs * nwh + nwh];
          float n6 = noise[xs + 1 + ys * nw + zs * nwh + nwh];
          float n7 = noise[xs + ys * nw + nw + zs * nwh + nwh];
          float n8 = noise[xs + 1 + ys * nw + nw + zs * nwh + nwh];

          data[x + y * w + z * w * h] += interpolateLinear3d(xt, yt, zt, n1, n2, n3, n4, n5, n6, n7, n8);
        }
      }
    }
  }

}
