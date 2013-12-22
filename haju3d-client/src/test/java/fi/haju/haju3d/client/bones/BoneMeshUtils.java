package fi.haju.haju3d.client.bones;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;
import fi.haju.haju3d.client.chunk.light.ChunkLightManager;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.client.ui.mesh.MyMesh;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public final class BoneMeshUtils {
  private BoneMeshUtils() {
    assert false;
  }

  public static Vector3f[] AXES = new Vector3f[] {
      Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z,
      Vector3f.UNIT_X.negate(), Vector3f.UNIT_Y.negate(), Vector3f.UNIT_Z.negate()};

  public static Spatial makeDragPlane(float sz, Mesh mesh, Material material, Vector3f direction, Vector3f left, Vector3f center) {
    final Geometry geom = new Geometry("DragPlane", mesh);
    geom.setMaterial(material);
    geom.setLocalTranslation(-sz / 2, -sz / 2, 0);
    Node node = new Node("DragPlane2");
    node.attachChild(geom);
    node.lookAt(direction.negate(), left);
    node.setLocalTranslation(center);
    return node;
  }

  public static BitmapText makeCircle(Vector3f screenPos, BitmapFont guiFont1) {
    BitmapText text = new BitmapText(guiFont1, false);
    text.setSize(guiFont1.getCharSet().getRenderedSize() * 2);
    text.setText("O");
    text.setLocalTranslation(screenPos.add(-text.getLineWidth() / 2, text.getLineHeight() / 2, 0));
    return text;
  }

  public static Geometry makeLine(Vector3f screenStart, Vector3f screenEnd, Material material) {
    Line lineMesh = new Line(screenStart, screenEnd);
    lineMesh.setLineWidth(2);
    Geometry line = new Geometry("line", lineMesh);
    line.setMaterial(material);
    return line;
  }

  public static Geometry makeArrow(Vector3f screenStart, Vector3f screenEnd, Material material) {
    Arrow lineMesh = new Arrow(screenEnd.subtract(screenStart));
    lineMesh.setLineWidth(2);
    Geometry line = new Geometry("line", lineMesh);
    line.setLocalTranslation(screenStart);
    line.setMaterial(material);
    return line;
  }

  public static Transform boneTransform(MyBone b) {
    return transformBetween(b.getStart(), b.getEnd(), Vector3f.UNIT_Z, b.getThickness());
  }

  public static Transform transformBetween(Vector3f start, Vector3f end, Vector3f front, float scale) {
    Vector3f dir = start.subtract(end);
    Vector3f left = dir.normalize().cross(front.normalize());
    Vector3f ahead = dir.normalize().cross(left.normalize());

    Quaternion q = new Quaternion();
    q.fromAxes(left.normalize(), ahead.normalize(), dir.normalize());
    return new Transform(
        start.add(end).multLocal(0.5f), q,
        new Vector3f(dir.length() * scale, dir.length() * scale, dir.length()));
  }

  public static Spatial makeFloor(Material material) {
    Mesh m = new Quad(100, 100);
    final Geometry geom = new Geometry("Floor", m);
    geom.setMaterial(material);
    geom.setShadowMode(RenderQueue.ShadowMode.Receive);
    Matrix3f rot = new Matrix3f();
    rot.fromAngleNormalAxis(3 * FastMath.PI / 2, Vector3f.UNIT_X);
    geom.setLocalRotation(rot);
    geom.setLocalTranslation(-50, -3, 50);
    return geom;
  }

  public static Mesh makeGridMesh(float width, float height, int xsteps, int ysteps) {
    Mesh mesh = new Mesh();
    mesh.setMode(Mesh.Mode.Lines);
    int lines = (xsteps + 1) + (ysteps + 1);
    IntBuffer indexes = BufferUtils.createIntBuffer(lines * 2);
    FloatBuffer vertexes = BufferUtils.createFloatBuffer(lines * 2 * 3);

    int i = 0;
    for (int x = 0; x <= xsteps; x++) {
      float xpos = x * width / xsteps;
      vertexes.put(xpos).put(0).put(0);
      vertexes.put(xpos).put(height).put(0);
      indexes.put(i).put(i + 1);
      i += 2;
    }
    for (int y = 0; y <= ysteps; y++) {
      float ypos = y * height / ysteps;
      vertexes.put(0).put(ypos).put(0);
      vertexes.put(width).put(ypos).put(0);
      indexes.put(i).put(i + 1);
      i += 2;
    }

    mesh.setBuffer(VertexBuffer.Type.Index, 2, indexes);
    mesh.setBuffer(VertexBuffer.Type.Position, 3, vertexes);

    mesh.updateBound();
    mesh.setStatic();
    return mesh;
  }

  public static Vector3f getAxisSnappedVector(Vector3f v) {
    double snapDist = 0.4;
    for (Vector3f axis : AXES) {
      if (v.distanceSquared(axis) < snapDist) {
        return axis;
      }
    }
    return v;
  }


  private static class BoneWorldGrid {
    private ByteArray3d grid;
    private Vector3i location;
  }

  public static Mesh buildMesh(List<MyBone> bones) {
    //-each bone has some 3d grid associated with it
    //-the grid should not be just binary, but values from 0 to 128 in order to do "meta"-shapes
    //-grid should have bounding box to represent valid x,y,z values where the shape is located..Or that's just grid size.
    //-grid should also indicate location of "start" and "end". So here it's ByteArray3d + startPos + endPos. During
    //  editing it could be represented with World, but when saving it's converted to ByteArray3d with "tight fit".
    //......
    //-based on desired output resolution, one can calculate how big grid is needed for each bone.
    //-each bone gridmesh should be transformed to world grid representation.
    //-each bone's world grid representation should be copied to single world grid that has simple binary value
    // for each cell

    ByteArray3d boneMeshGrid = makeBoneMeshGrid();

    float scale = 10;

    List<BoneWorldGrid> boneWorldGrids = new ArrayList<>();
    for (MyBone bone : bones) {
      Transform transform = boneTransform(bone);
      //bounding box needed for boneMeshGrid in world grid:
      float bs = 0.7f;
      Vector3f c1 = transform.transformVector(new Vector3f(-bs, -bs, -bs), null).multLocal(scale);
      Vector3f c2 = transform.transformVector(new Vector3f(+bs, -bs, -bs), null).multLocal(scale);
      Vector3f c3 = transform.transformVector(new Vector3f(-bs, +bs, -bs), null).multLocal(scale);
      Vector3f c4 = transform.transformVector(new Vector3f(-bs, -bs, +bs), null).multLocal(scale);
      Vector3f c5 = transform.transformVector(new Vector3f(+bs, +bs, -bs), null).multLocal(scale);
      Vector3f c6 = transform.transformVector(new Vector3f(-bs, +bs, +bs), null).multLocal(scale);
      Vector3f c7 = transform.transformVector(new Vector3f(+bs, -bs, +bs), null).multLocal(scale);
      Vector3f c8 = transform.transformVector(new Vector3f(+bs, +bs, +bs), null).multLocal(scale);

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

      BoneWorldGrid bwg2 = new BoneWorldGrid();
      bwg2.grid = new ByteArray3d(xsize, ysize, zsize);
      bwg2.location = new Vector3i(Math.round(cmin.x), Math.round(cmin.y), Math.round(cmin.z));

      ByteArray3d grid = bwg2.grid;
      int w = grid.getWidth();
      int h = grid.getHeight();
      int d = grid.getDepth();
      for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++) {
          for (int z = 0; z < d; z++) {
            Vector3f v = new Vector3f(x, y, z).add(cmin).divide(scale);
            Vector3f inv = transform.transformInverseVector(v, null);
            if (inv.x > -bs && inv.x < bs &&
                inv.y > -bs && inv.y < bs &&
                inv.z > -bs && inv.z < bs) {

              int bx = (int) ((inv.x - (-bs)) / (bs - (-bs)) * boneMeshGrid.getWidth());
              int by = (int) ((inv.y - (-bs)) / (bs - (-bs)) * boneMeshGrid.getHeight());
              int bz = (int) ((inv.z - (-bs)) / (bs - (-bs)) * boneMeshGrid.getDepth());

              grid.set(x, y, z, boneMeshGrid.get(bx, by, bz));
            }
          }
        }
      }
      boneWorldGrids.add(bwg2);
    }

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
    System.out.println("required grid size = " + requiredSize);

    ChunkCoordinateSystem chunkCoordinateSystem = new ChunkCoordinateSystem(requiredSize);
    World world = new World(chunkCoordinateSystem);
    final int sz = chunkCoordinateSystem.getChunkSize();

    for (int x = 0; x < 3; x++) {
      for (int y = 0; y < 3; y++) {
        for (int z = 0; z < 3; z++) {
          ChunkPosition pos = new ChunkPosition(x - 1, y - 1, z - 1);
          world.setChunk(pos, new Chunk(sz, 0, pos, Tile.AIR));
        }
      }
    }

    ChunkPosition cp = new ChunkPosition(0, 0, 0);
    Chunk chunk = new Chunk(sz, 0, cp);

    for (BoneWorldGrid bwg : boneWorldGrids) {
      ByteArray3d grid = bwg.grid;
      int w = grid.getWidth();
      int h = grid.getHeight();
      int d = grid.getDepth();
      Vector3i offset = bwg.location.subtract(minLocation);
      for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++) {
          for (int z = 0; z < d; z++) {
            if (grid.get(x, y, z) > 0) {
              chunk.set(x + offset.x, y + offset.y, z + offset.z, Tile.GROUND);
            }
          }
        }
      }
    }
    world.setChunk(cp, chunk);

    ChunkLightManager light = new ChunkLightManager();
    MyMesh myMesh = ChunkSpatialBuilder.makeCubeMesh(world, cp, light);
    ChunkSpatialBuilder.smoothMesh(myMesh, 8);
    ChunkSpatialBuilder.prepareMesh(myMesh);
    return new ChunkSpatialBuilder.SimpleMeshBuilder(myMesh, new Vector3f(-sz / 2, -sz / 2, 0), 1.0f / scale).build();
  }

  private static ByteArray3d makeBoneMeshGrid() {
    int sz = 128;
    ByteArray3d grid = new ByteArray3d(sz, sz, sz);
    int w = grid.getWidth();
    int h = grid.getHeight();
    int d = grid.getDepth();
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          int xd = x - sz / 2;
          int yd = y - sz / 2;
          int zd = z - sz / 2;
          int bsz = sz / 2;
          byte v = xd * xd + yd * yd + zd * zd < bsz * bsz ? (byte) 1 : 0;
          grid.set(x, y, z, v);
        }
      }
    }
    return grid;
  }
}
