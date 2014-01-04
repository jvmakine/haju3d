package fi.haju.haju3d.client.bones;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import fi.haju.haju3d.protocol.world.ByteArray3d;
import fi.haju.haju3d.protocol.world.FloatArray3d;
import fi.haju.haju3d.util.noise.InterpolationUtil;

import java.util.Random;

/**
 * Utilities for creating bone meshes.
 * </p>
 * <li>makeBoneMesh converts boneMeshGrid into a Mesh
 * <li>makeSphereBoneMeshGrid, makeBoxBoneMeshGrid etc create different kinds of boneMeshGrids.
 */
public final class BoneMeshUtils {
  private BoneMeshUtils() {
    assert false;
  }

  private static final int BONE_MESH_SIZE = 64;
  private static final int HARDNESS = 8;

  public static Mesh makeBoneMesh(ByteArray3d boneMeshGrid) {
    final int sz = boneMeshGrid.getWidth();
    return MarchingCubesMesher.createMesh(boneMeshGrid, 1.0f / (sz * 0.5f), new Vector3f(-sz / 2, -sz / 2, -sz / 2));
  }

  public static ByteArray3d makeSphereBoneMeshGrid() {
    final int sz = BONE_MESH_SIZE;
    ByteArray3d grid = new ByteArray3d(sz, sz, sz);
    grid.set(new ByteArray3d.GetValue() {
      @Override
      public byte getValue(int x, int y, int z) {
        int xd = x - sz / 2;
        int yd = y - sz / 2;
        int zd = z - sz / 2;
        int bsz = sz / 3;
        float value = bsz - FastMath.sqrt(xd * xd + yd * yd + zd * zd);

        value = (value * HARDNESS) + 64;
        if (value < 0) value = 0;
        if (value > 127) value = 127;
        return (byte) value;
      }
    });
    return grid;
  }

  public static ByteArray3d makeBoxBoneMeshGrid() {
    final int sz = BONE_MESH_SIZE;
    ByteArray3d grid = new ByteArray3d(sz, sz, sz);
    grid.set(new ByteArray3d.GetValue() {
      @Override
      public byte getValue(int x, int y, int z) {
        int xd = Math.abs(x - sz / 2);
        int yd = Math.abs(y - sz / 2);
        int zd = Math.abs(z - sz / 2);
        int bsz = sz / 4;
        int value = bsz - Math.max(Math.max(xd, yd), zd);

        value = (value * HARDNESS) + 64;
        if (value < 0) value = 0;
        if (value > 127) value = 127;
        return (byte) value;
      }
    });
    return grid;
  }

  public static ByteArray3d makeCylinderBoneMeshGrid() {
    final int sz = BONE_MESH_SIZE;
    ByteArray3d grid = new ByteArray3d(sz, sz, sz);
    grid.set(new ByteArray3d.GetValue() {
      @Override
      public byte getValue(int x, int y, int z) {
        int xd = Math.abs(x - sz / 2);
        int yd = Math.abs(y - sz / 2);
        int zd = Math.abs(z - sz / 2);
        int bsz = sz / 4;
        float value = bsz - Math.max(FastMath.sqrt(xd * xd + yd * yd), zd);

        value = (value * HARDNESS) + 64;
        if (value < 0) value = 0;
        if (value > 127) value = 127;
        return (byte) value;
      }
    });
    return grid;
  }

  public static ByteArray3d makeBlobBoneMeshGrid() {
    return makeBlobBoneMeshGrid(1);
  }

  public static ByteArray3d makeBlobBoneMeshGrid(int randomSeed) {
    final int sz = BONE_MESH_SIZE;
    ByteArray3d grid = new ByteArray3d(sz, sz, sz);
    final FloatArray3d noise = make3dPerlinNoise(randomSeed, sz, sz, sz);

    grid.set(new ByteArray3d.GetValue() {
      @Override
      public byte getValue(int x, int y, int z) {
        int xd = x - sz / 2;
        int yd = y - sz / 2;
        int zd = z - sz / 2;
        int bsz = sz / 3;
        float value = bsz - FastMath.sqrt(xd * xd + yd * yd + zd * zd);

        value = (value * HARDNESS * 1.5f) + 64;

        //x-symmetric noise
        value += noise.get(x, Math.abs(sz / 2 - y), z) * 6;
        value -= 4;
        if (value < 0) value = 0;
        if (value > 127) value = 127;
        return (byte) value;
      }
    });
    return grid;
  }


  private static FloatArray3d make3dPerlinNoise(long seed, int w, int h, int d) {
    Random random = new Random(seed);
    FloatArray3d data = new FloatArray3d(w, h, d);
    for (int scale = 4; scale != 128; scale *= 2) {
      add3dNoise(random, data, scale, (float) Math.pow(0.5f * scale * 1.0f, 1.0f));
    }
    return data;
  }

  private static void add3dNoise(final Random random, FloatArray3d data, int scale, final float amp) {
    int w = data.getWidth();
    int h = data.getHeight();
    int d = data.getDepth();

    int nw = w / scale + 2;
    int nh = h / scale + 2;
    int nd = d / scale + 2;

    FloatArray3d noise = new FloatArray3d(nw, nh, nd, new FloatArray3d.Initializer() {
      @Override
      public float getValue(int x, int y, int z) {
        return (float) ((random.nextDouble() - 0.5) * amp);
      }
    });

    for (int z = 0; z < d; z++) {
      float zt = (float) (z % scale) / scale;
      int zs = z / scale;
      for (int y = 0; y < h; y++) {
        float yt = (float) (y % scale) / scale;
        int ys = y / scale;
        for (int x = 0; x < w; x++) {
          float xt = (float) (x % scale) / scale;
          int xs = x / scale;

          float n1 = noise.get(xs, ys, zs);
          float n2 = noise.get(xs + 1, ys, zs);
          float n3 = noise.get(xs, ys + 1, zs);
          float n4 = noise.get(xs + 1, ys + 1, zs);

          float n5 = noise.get(xs, ys, zs + 1);
          float n6 = noise.get(xs + 1, ys, zs + 1);
          float n7 = noise.get(xs, ys + 1, zs + 1);
          float n8 = noise.get(xs + 1, ys + 1, zs + 1);

          data.add(x, y, z, InterpolationUtil.interpolateLinear3d(xt, yt, zt, n1, n2, n3, n4, n5, n6, n7, n8));
        }
      }
    }
  }
}
