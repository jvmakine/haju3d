package fi.haju.haju3d.client.ui.mesh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.util.noise.InterpolationUtil;
import fi.haju.haju3d.util.noise.PerlinNoiseUtil;

public class ChunkMeshBuilder {
  
  public ChunkMeshBuilder() {
  }

  public Mesh makeMesh(Chunk chunk) {
    float scale = 1;

    int w = chunk.getWidth();
    int d = chunk.getDepth();
    
    MyMesh myMesh = makeCubeMesh(chunk);
    smoothMesh(myMesh);

    Map<MyVertex, Integer> vertexIndex = Maps.newHashMap();
    List<Integer> indexes = Lists.newArrayList();

    for (MyFace face : myMesh.faces) {
      face.normal = face.v2.v.subtract(face.v1.v).cross(face.v4.v.subtract(face.v1.v)).normalize();
    }

    for (MyFace face : myMesh.faces) {
      addQuad(indexes, vertexIndex, face.v1, face.v2, face.v3, face.v4);
    }

    Vector3f[] vArray = new Vector3f[vertexIndex.size()];
    Vector3f[] vnArray = new Vector3f[vertexIndex.size()];
    Vector2f[] texArray = new Vector2f[vertexIndex.size()];
    for (Map.Entry<MyVertex, Integer> e : vertexIndex.entrySet()) {
      Vector3f sum = Vector3f.ZERO.clone();
      for (MyFace f : myMesh.vertexFaces.get(e.getKey())) {
        sum.addLocal(f.normal);
      }
      sum.normalizeLocal();
      Vector3f v = e.getKey().v;
      vArray[e.getValue()] = v.mult(scale);
      vnArray[e.getValue()] = sum;
      texArray[e.getValue()] = new Vector2f(v.x / w * 8, v.z / d * 8);
    }
    int[] iArray = new int[indexes.size()];
    for (int i = 0; i < indexes.size(); i++) {
      iArray[i] = indexes.get(i);
    }

    int tw = 40;
    int th = 40;
    int td = 40;
    float[] noiseR = PerlinNoiseUtil.make3dPerlinNoise(chunk.getSeed(), tw, th, td);
    float[] noiseG = PerlinNoiseUtil.make3dPerlinNoise(chunk.getSeed()/2, tw, th, td);

    float[] cArray = new float[vertexIndex.size() * 4];
    for (Map.Entry<MyVertex, Integer> e : vertexIndex.entrySet()) {
      int vi = e.getValue();
      Vector3f pos = e.getKey().v;
      float tx = pos.x / chunk.getWidth() * tw;
      float ty = pos.y / chunk.getHeight() * th;
      float tz = pos.z / chunk.getDepth() * td;

      float n0r = Math.abs(getNoise(noiseR, tw, th, td, tx, ty, tz)) * 0.1f + 0.7f;
      float nr = FastMath.clamp(n0r, 0.6f, 1.0f);

      float n0g = Math.abs(getNoise(noiseG, tw, th, td, tx, ty, tz)) * 0.1f + 0.7f;
      float ng = FastMath.clamp(n0g, 0.3f, 1.0f);

      cArray[vi * 4 + 0] = nr;
      cArray[vi * 4 + 1] = ng;
      cArray[vi * 4 + 2] = ng;
      cArray[vi * 4 + 3] = 1.0f;
    }

    Mesh m = new Mesh();
    m.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vArray));
    m.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(vnArray));
    m.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(cArray));
    m.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texArray));
    m.setBuffer(Type.Index, 1, BufferUtils.createIntBuffer(iArray));
    m.updateBound();
    return m;
  }
  
  private static MyMesh makeCubeMesh(Chunk chunk) {
    MyMesh myMesh = new MyMesh();
    int w = chunk.getWidth();
    int h = chunk.getHeight();
    int d = chunk.getDepth();
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          if (chunk.get(x, y, z) == Tile.GROUND) {
            if (chunk.get(x, y - 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x, y, z + 1));
            }
            if (chunk.get(x, y + 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x, y + 1, z));
            }
            if (chunk.get(x - 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x, y, z));
            }
            if (chunk.get(x + 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y, z + 1));
            }
            if (chunk.get(x, y, z - 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y, z));
            }
            if (chunk.get(x, y, z + 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y, z + 1));
            }
          }
        }
      }
    }
    return myMesh;
  }

  private static void smoothMesh(MyMesh myMesh) {
    for (int i = 0; i < 3; i++) {
      Map<MyVertex, Vector3f> newPos = new HashMap<>();
      for (Map.Entry<MyVertex, List<MyFace>> e : myMesh.vertexFaces.entrySet()) {
        Vector3f sum = Vector3f.ZERO.clone();
        List<MyFace> faces = e.getValue();
        for (MyFace f : faces) {
          sum.addLocal(f.getCenter());
        }
        sum.divideLocal(faces.size());
        newPos.put(e.getKey(), sum);
      }
      for (Map.Entry<MyVertex, Vector3f> e : newPos.entrySet()) {
        e.getKey().v.set(e.getValue());
      }
    }
  }

  private float getNoise(float[] noise, int tw, int th, int td, float tx, float ty, float tz) {
    int x = (int) tx;
    int y = (int) ty;
    int z = (int) tz;

    float xt = tx - x;
    float yt = ty - y;
    float zt = tz - z;

    if (x >= 0 && x < tw - 1 && y >= 0 && y < th - 1 && z >= 0 && z < td - 1) {
      return InterpolationUtil.interpolateLinear3d(
          xt, yt, zt,
          noise[x + y * tw + z * tw * td],
          noise[x + 1 + y * tw + z * tw * td],
          noise[x + y * tw + tw + z * tw * td],
          noise[x + 1 + y * tw + tw + z * tw * td],
          noise[x + y * tw + (z + 1) * tw * td],
          noise[x + 1 + y * tw + (z + 1) * tw * td],
          noise[x + y * tw + tw + (z + 1) * tw * td],
          noise[x + 1 + y * tw + tw + (z + 1) * tw * td]);
    } else {
      return 0;
    }
  }

  private void addQuad(
      List<Integer> indexes, Map<MyVertex, Integer> vertexIndex,
      MyVertex vector3f1, MyVertex vector3f2,
      MyVertex vector3f3, MyVertex vector3f4) {
    indexes.add(getVertexIndex(vertexIndex, vector3f1));
    indexes.add(getVertexIndex(vertexIndex, vector3f2));
    indexes.add(getVertexIndex(vertexIndex, vector3f4));

    indexes.add(getVertexIndex(vertexIndex, vector3f2));
    indexes.add(getVertexIndex(vertexIndex, vector3f3));
    indexes.add(getVertexIndex(vertexIndex, vector3f4));
  }

  private Integer getVertexIndex(Map<MyVertex, Integer> vertexIndex, MyVertex v) {
    Integer i = vertexIndex.get(v);
    if (i == null) {
      i = vertexIndex.size();
      vertexIndex.put(v, i);
    }
    return i;
  }

}
