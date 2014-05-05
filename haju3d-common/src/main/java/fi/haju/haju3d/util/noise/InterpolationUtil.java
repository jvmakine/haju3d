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
    float x1 = n1 + (n2 - n1) * xt;
    float x2 = n3 + (n4 - n3) * xt;
    float z1 = x1 + (x2 - x1) * yt;
    float xx1 = n5 + (n6 - n5) * xt;
    float xx2 = n7 + (n8 - n7) * xt;
    float z2 = xx1 + (xx2 - xx1) * yt;
    return z1 + (z2 - z1) * zt;
  }

  public static float interpolateLinear2d(float xt, float yt, float n1, float n2, float n3, float n4) {
    float xx1 = n1 + (n2 - n1) * xt;
    float xx2 = n3 + (n4 - n3) * xt;
    return xx1 + (xx2 - xx1) * yt;
  }


}
