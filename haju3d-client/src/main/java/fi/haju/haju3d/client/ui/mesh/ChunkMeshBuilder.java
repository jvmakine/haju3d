package fi.haju.haju3d.client.ui.mesh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

public class ChunkMeshBuilder {
  private boolean useVertexColor = true;
  
  public ChunkMeshBuilder() {
  }
  
  public void setUseVertexColor(boolean useVertexColor) {
    this.useVertexColor = useVertexColor;
  }

  public Mesh makeMesh(World chunk, Vector3i chunkIndex) {
    float scale = 1;

    int w = 64;// chunk.getWidth();
    int d = 64;//chunk.getDepth();
    
    MyMesh myMesh = makeCubeMesh(chunk, chunkIndex);
    
    System.out.println(myMesh.faces.size());
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
    Vector3f[] texArray = new Vector3f[vertexIndex.size()];
    for (Map.Entry<MyVertex, Integer> e : vertexIndex.entrySet()) {
      Vector3f sum = Vector3f.ZERO.clone();
      for (MyFace f : myMesh.vertexFaces.get(e.getKey())) {
        sum.addLocal(f.normal);
      }
      sum.normalizeLocal();
      Vector3f v = e.getKey().v;
      vArray[e.getValue()] = v.mult(scale);
      vnArray[e.getValue()] = sum;
      texArray[e.getValue()] = new Vector3f(v.x / w * 8, v.z / d * 8, e.getValue() % 2);
    }
    int[] iArray = new int[indexes.size()];
    for (int i = 0; i < indexes.size(); i++) {
      iArray[i] = indexes.get(i);
    }

    Mesh m = new Mesh();
    m.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vArray));
    m.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(vnArray));
    m.setBuffer(Type.TexCoord, 3, BufferUtils.createFloatBuffer(texArray));
    m.setBuffer(Type.Index, 1, BufferUtils.createIntBuffer(iArray));
    
    if (useVertexColor) {
      /*
      int tw = 40;
      int th = 40;
      int td = 40;
      FloatArray3d noiseR = PerlinNoiseUtil.make3dPerlinNoise(chunk.getSeed(), tw, th, td);
      FloatArray3d noiseG = PerlinNoiseUtil.make3dPerlinNoise(chunk.getSeed()/2, tw, th, td);
  
      float[] cArray = new float[vertexIndex.size() * 4];
      for (Map.Entry<MyVertex, Integer> e : vertexIndex.entrySet()) {
        int vi = e.getValue();
        Vector3f pos = e.getKey().v;
        float tx = pos.x / chunk.getWidth() * tw;
        float ty = pos.y / chunk.getHeight() * th;
        float tz = pos.z / chunk.getDepth() * td;
  
        float n0r = Math.abs(noiseR.getInterpolated(tx, ty, tz)) * 0.1f + 0.7f;
        float nr = FastMath.clamp(n0r, 0.6f, 1.0f);
  
        float n0g = Math.abs(noiseG.getInterpolated(tx, ty, tz)) * 0.1f + 0.7f;
        float ng = FastMath.clamp(n0g, 0.3f, 1.0f);
  
        cArray[vi * 4 + 0] = nr;
        cArray[vi * 4 + 1] = ng;
        cArray[vi * 4 + 2] = ng;
        cArray[vi * 4 + 3] = 1.0f;
      }
      m.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(cArray));
      */
    }
    
    m.updateBound();
    return m;
  }
  
  private static MyMesh makeCubeMesh(World chunk, Vector3i chunkIndex) {
    MyMesh myMesh = new MyMesh();
    
    Vector3i w1 = chunk.getWorldPosition(chunkIndex);
    Vector3i w2 = chunk.getWorldPosition(chunkIndex.add(1, 1, 1));
    
    for (int x = w1.x; x < w2.x; x++) {
      for (int y = w1.y; y < w2.y; y++) {
        for (int z = w1.z; z < w2.z; z++) {
          if (chunk.get(x, y, z) != Tile.AIR) {
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

  private static void addQuad(
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

  private static Integer getVertexIndex(Map<MyVertex, Integer> vertexIndex, MyVertex v) {
    Integer i = vertexIndex.get(v);
    if (i == null) {
      i = vertexIndex.size();
      vertexIndex.put(v, i);
    }
    return i;
  }

}
