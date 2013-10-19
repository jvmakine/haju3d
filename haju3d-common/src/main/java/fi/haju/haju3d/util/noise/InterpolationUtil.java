package fi.haju.haju3d.util.noise;

public class InterpolationUtil {

  private InterpolationUtil() {
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


}
