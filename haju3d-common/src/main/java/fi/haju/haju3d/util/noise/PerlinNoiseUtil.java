package fi.haju.haju3d.util.noise;

import java.util.Random;

import fi.haju.haju3d.protocol.world.FloatArray3d;

public final class PerlinNoiseUtil {
  
  private PerlinNoiseUtil() {
  }
  
  public static FloatArray3d make3dPerlinNoise(long seed, int w, int h, int d) {
    Random random = new Random(seed);
    FloatArray3d data = new FloatArray3d(w, h, d);
    for (int sc = 4; sc != 128; sc *= 2) {
      add3dNoise(random, data, sc, (float)Math.pow(0.5f * sc * 1.0f, 1.0f));
    }
    return data;
  }
    
  private static void add3dNoise(Random random, FloatArray3d data, int scale, float amp) {
    int w = data.getWidth();
    int h = data.getHeight();
    int d = data.getDepth();
    
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

          data.add(x, y, z, InterpolationUtil.interpolateLinear3d(xt, yt, zt, n1, n2, n3, n4, n5, n6, n7, n8));
        }
      }
    }
  }

}
