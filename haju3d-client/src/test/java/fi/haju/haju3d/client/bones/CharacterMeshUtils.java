package fi.haju.haju3d.client.bones;

import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.ByteArray3d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utilities for building final character mesh out of bones and their associated grids.
 */
public final class CharacterMeshUtils {
  private CharacterMeshUtils() {
    assert false;
  }

  private static class BoneWorldGrid {
    private ByteArray3d grid;
    private Vector3i location;
  }

  private static final int MAX_BONES_PER_TILE = 4;

  private static class ResultGrid {
    private ByteArray3d dataGrid;
    private ByteArray3d[] boneIndexGrid = new ByteArray3d[MAX_BONES_PER_TILE];
    private ByteArray3d[] boneWeightGrid = new ByteArray3d[MAX_BONES_PER_TILE];
    public Vector3i minLocation;
  }

  /**
   * Create final, HW skinned character mesh out of bone and meshGridMap information.
   *
   * @param meshGridMap maps bone's meshName to a boneMeshGrid that represents the bone's shape
   */
  public static Mesh buildMesh(List<MyBone> bones, Map<String, ByteArray3d> meshGridMap) {
    float worldScale = 10;
    List<BoneWorldGrid> boneWorldGrids = new ArrayList<>();
    // 1.Transform each bone into same "world grid"
    for (MyBone bone : bones) {
      boneWorldGrids.add(makeBoneWorldGrid(meshGridMap.get(bone.getMeshName()), worldScale, bone));
    }
    // 2.Merge boneWorldGrids into a single grid
    ResultGrid resultGrid = makeResultGrid(boneWorldGrids);
    // 3.Create a mesh from boneWorldGrids
    // this final meshing takes about 10x longer than any other part here
    return getMeshFromGrid(resultGrid, worldScale);
  }

  private static ResultGrid makeResultGrid(List<BoneWorldGrid> boneWorldGrids) {
    // find min and max location for all BoneWorldGrids
    Vector3i minLocation = new Vector3i(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Vector3i maxLocation = new Vector3i(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    for (BoneWorldGrid bwg : boneWorldGrids) {
      minLocation.x = Math.min(minLocation.x, bwg.location.x);
      minLocation.y = Math.min(minLocation.y, bwg.location.y);
      minLocation.z = Math.min(minLocation.z, bwg.location.z);

      maxLocation.x = Math.max(maxLocation.x, bwg.location.x + bwg.grid.getWidth());
      maxLocation.y = Math.max(maxLocation.y, bwg.location.y + bwg.grid.getHeight());
      maxLocation.z = Math.max(maxLocation.z, bwg.location.z + bwg.grid.getDepth());
    }

    int requiredSize = Math.max(Math.max(maxLocation.x - minLocation.x, maxLocation.y - minLocation.y), maxLocation.z - minLocation.z);
    //System.out.println("required grid size = " + requiredSize);

    ResultGrid resultGrid = new ResultGrid();
    resultGrid.minLocation = minLocation;
    resultGrid.dataGrid = new ByteArray3d(requiredSize, requiredSize, requiredSize);
    for (int i = 0; i < MAX_BONES_PER_TILE; i++) {
      resultGrid.boneIndexGrid[i] = new ByteArray3d(requiredSize, requiredSize, requiredSize);
      resultGrid.boneWeightGrid[i] = new ByteArray3d(requiredSize, requiredSize, requiredSize);
    }

    ByteArray3d dataGrid = resultGrid.dataGrid;

    int boneIndex = 0;
    for (BoneWorldGrid bwg : boneWorldGrids) {
      ByteArray3d grid = bwg.grid;
      int w = grid.getWidth();
      int h = grid.getHeight();
      int d = grid.getDepth();
      Vector3i offset = bwg.location.subtract(minLocation);
      Vector3i pos = new Vector3i();
      for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++) {
          for (int z = 0; z < d; z++) {
            byte add = grid.get(x, y, z);
            if (add > 0) {
              pos.set(x + offset.x, y + offset.y, z + offset.z);
              byte old = dataGrid.get(pos);
              int newValue = (int) old + (int) add;
              //int newValue = Math.max((int) old, (int) add);
              if (newValue > 127) {
                newValue = 127;
              }
              dataGrid.set(pos, (byte) newValue);

              // assign bone weight in next free slot in boneWeightGrid
              int bcount = 0;
              while (bcount < MAX_BONES_PER_TILE && resultGrid.boneWeightGrid[bcount].get(pos) != 0) {
                bcount++;
              }
              if (bcount < MAX_BONES_PER_TILE) {
                resultGrid.boneIndexGrid[bcount].set(pos, (byte) boneIndex);
                resultGrid.boneWeightGrid[bcount].set(pos, add);
              }
            }
          }
        }
      }
      boneIndex++;
    }
    return resultGrid;
  }

  private static Mesh getMeshFromGrid(final ResultGrid resultGrid, float worldScale) {
    Vector3f translate = new Vector3f(resultGrid.minLocation.x, resultGrid.minLocation.y, resultGrid.minLocation.z);
    float scale = 1.0f / worldScale;
    Mesh mesh = MarchingCubesMesher.createMesh(resultGrid.dataGrid, scale, translate);

    // Setup bone weight buffer
    FloatBuffer weights = BufferUtils.createFloatBuffer(mesh.getVertexCount() * 4);
    VertexBuffer weightsBuf = new VertexBuffer(VertexBuffer.Type.HWBoneWeight);
    weightsBuf.setupData(VertexBuffer.Usage.Static, 4, VertexBuffer.Format.Float, weights);
    mesh.setBuffer(weightsBuf);

    // Setup bone index buffer
    ByteBuffer indices = BufferUtils.createByteBuffer(mesh.getVertexCount() * 4);
    VertexBuffer indicesBuf = new VertexBuffer(VertexBuffer.Type.HWBoneIndex);
    indicesBuf.setupData(VertexBuffer.Usage.Static, 4, VertexBuffer.Format.UnsignedByte, indices);
    mesh.setBuffer(indicesBuf);

    Vector3f v1 = new Vector3f();
    Vector3f v2 = new Vector3f();
    Vector3f v3 = new Vector3f();
    for (int i = 0; i < mesh.getTriangleCount(); i++) {
      mesh.getTriangle(i, v1, v2, v3);
      putBoneData(weights, indices, v1, resultGrid, scale, translate);
      putBoneData(weights, indices, v2, resultGrid, scale, translate);
      putBoneData(weights, indices, v3, resultGrid, scale, translate);
    }

    return mesh;
  }

  private static void putBoneData(FloatBuffer weights, ByteBuffer indices, Vector3f vert, ResultGrid resultGrid, float scale, Vector3f translate) {
    Vector3f vorig = vert.divide(scale).subtractLocal(translate);
    Vector3i wp = new Vector3i((int) (vorig.x + 0.5f), (int) (vorig.y + 0.5f), (int) (vorig.z + 0.5f));
    float sumWeights = 0;
    for (int i = 0; i < MAX_BONES_PER_TILE; i++) {
      sumWeights += resultGrid.boneWeightGrid[i].get(wp);
    }
    for (int i = 0; i < MAX_BONES_PER_TILE; i++) {
      indices.put(resultGrid.boneIndexGrid[i].get(wp));
      weights.put(resultGrid.boneWeightGrid[i].get(wp) / sumWeights);
    }
  }

  private static BoneWorldGrid makeBoneWorldGrid(ByteArray3d boneMeshGrid, float worldScale, MyBone bone) {
    Transform transform = BoneTransformUtils.boneTransform2(bone);
    //bounding box needed for boneMeshGrid in world grid:
    float bs = 1.0f;
    Vector3f c1 = transform.transformVector(new Vector3f(-bs, -bs, -bs), null).multLocal(worldScale);
    Vector3f c2 = transform.transformVector(new Vector3f(+bs, -bs, -bs), null).multLocal(worldScale);
    Vector3f c3 = transform.transformVector(new Vector3f(-bs, +bs, -bs), null).multLocal(worldScale);
    Vector3f c4 = transform.transformVector(new Vector3f(-bs, -bs, +bs), null).multLocal(worldScale);
    Vector3f c5 = transform.transformVector(new Vector3f(+bs, +bs, -bs), null).multLocal(worldScale);
    Vector3f c6 = transform.transformVector(new Vector3f(-bs, +bs, +bs), null).multLocal(worldScale);
    Vector3f c7 = transform.transformVector(new Vector3f(+bs, -bs, +bs), null).multLocal(worldScale);
    Vector3f c8 = transform.transformVector(new Vector3f(+bs, +bs, +bs), null).multLocal(worldScale);

    Vector3f cmin = c1.clone();
    cmin.minLocal(c2);
    cmin.minLocal(c3);
    cmin.minLocal(c4);
    cmin.minLocal(c5);
    cmin.minLocal(c6);
    cmin.minLocal(c7);
    cmin.minLocal(c8);
    Vector3f cmax = c1.clone();
    cmax.maxLocal(c2);
    cmax.maxLocal(c3);
    cmax.maxLocal(c4);
    cmax.maxLocal(c5);
    cmax.maxLocal(c6);
    cmax.maxLocal(c7);
    cmax.maxLocal(c8);

    int xsize = (int) FastMath.ceil(cmax.x - cmin.x);
    int ysize = (int) FastMath.ceil(cmax.y - cmin.y);
    int zsize = (int) FastMath.ceil(cmax.z - cmin.z);

    ByteArray3d grid = new ByteArray3d(xsize, ysize, zsize);
    int w = grid.getWidth();
    int h = grid.getHeight();
    int d = grid.getDepth();
    Vector3f v = new Vector3f();
    Vector3f inv = new Vector3f();
    Vector3f inv2 = new Vector3f();

    //we want to calculate transform: (inv - (-bs)) * (sz / (bs - (-bs)))
    //se let's precalculate it to (inv + shift) * scale
    Vector3f scale = new Vector3f(boneMeshGrid.getWidth(), boneMeshGrid.getHeight(), boneMeshGrid.getDepth()).divideLocal(bs * 2);
    Vector3f shift = Vector3f.UNIT_XYZ.mult(bs);

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        // calculate inverse transform at (x,y,0) and (x,y,1), the rest of the transforms in inner loop
        // can be calculated by adding (inv2-inv1) because the transforms are linear
        v.set(x, y, 0).addLocal(cmin).divideLocal(worldScale);
        transform.transformInverseVector(v, inv);
        inv.addLocal(shift).multLocal(scale);

        v.set(x, y, 1).addLocal(cmin).divideLocal(worldScale);
        transform.transformInverseVector(v, inv2);
        inv2.addLocal(shift).multLocal(scale);

        Vector3f add = inv2.subtractLocal(inv);

        for (int z = 0; z < d; z++) {
          inv.addLocal(add);
          if (inv.x >= 0 && inv.x < boneMeshGrid.getWidth() &&
              inv.y >= 0 && inv.y < boneMeshGrid.getHeight() &&
              inv.z >= 0 && inv.z < boneMeshGrid.getDepth()) {

            grid.set(x, y, z, boneMeshGrid.get((int) inv.x, (int) inv.y, (int) inv.z));
          }
        }
      }
    }

    // Once the boneMeshGrid has been transformed into world grid, it may suffer from
    // downsampling and upsampling artifacts (because the sampling is very simple nearest-neighbor).
    // Blurring the grid helps with both issues (blur=fake antialias). It has the added benefit
    // that each BoneWorldGrid will have some "smoothing buffer" around the actual shape, so that
    // the shape blends better with other bones' shapes.
    blurGrid(grid);

    BoneWorldGrid bwg2 = new BoneWorldGrid();
    bwg2.grid = grid;
    bwg2.location = new Vector3i(Math.round(cmin.x), Math.round(cmin.y), Math.round(cmin.z));
    return bwg2;
  }

  /**
   * Apply small blur filter on the grid.
   */
  private static void blurGrid(ByteArray3d grid) {
    int w = grid.getWidth();
    int h = grid.getHeight();
    int d = grid.getDepth();
    for (int x = 1; x < w - 1; x++) {
      for (int y = 1; y < h - 1; y++) {
        for (int z = 1; z < d - 1; z++) {
          int newVal = ((int) grid.get(x, y, z) + grid.get(x - 1, y, z) + grid.get(x + 1, y, z)
              + grid.get(x, y - 1, z) + grid.get(x, y + 1, z)
              + grid.get(x, y, z - 1) + grid.get(x, y, z + 1)) / 7;
          grid.set(x, y, z, (byte) newVal);
        }
      }
    }
  }


}
