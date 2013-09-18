package fi.haju.haju3d.client.ui.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

public class ChunkMeshBuilder {
  public ChunkMeshBuilder() {
  }
  
  public Mesh makeMesh(World chunk, Vector3i chunkIndex) {
    float scale = 1;

    int w = 64;// chunk.getWidth();
    int d = 64;//chunk.getDepth();
    
    MyMesh myMesh = makeCubeMesh(chunk, chunkIndex);
//    smoothMesh(myMesh);
    
    Map<MyVertex, Integer> vertexIndex = new HashMap<>();
    List<Integer> indexes = new ArrayList<>();

    for (MyFace face : myMesh.faces) {
      face.normal = face.v2.v.subtract(face.v1.v).cross(face.v4.v.subtract(face.v1.v)).normalize();
    }

    for (MyFace face : myMesh.faces) {
      addQuad(indexes, vertexIndex, face.v1, face.v2, face.v3, face.v4);
    }

    Vector3f[] vArray = new Vector3f[vertexIndex.size()];
    Vector3f[] vnArray = new Vector3f[vertexIndex.size()];
    Vector3f[] texArray = new Vector3f[vertexIndex.size()];
    Vector4f[] colArray = new Vector4f[vertexIndex.size()];
    for (Map.Entry<MyVertex, Integer> e : vertexIndex.entrySet()) {
      Vector3f sum = Vector3f.ZERO.clone();
      float colSum = 0.0f;
      List<MyFace> faces = myMesh.vertexFaces.get(e.getKey());
      for (MyFace f : faces) {
        sum.addLocal(f.normal);
        colSum += f.color;
      }
      sum.normalizeLocal();
      colSum /= faces.size();
      Vector3f v = e.getKey().v;
      vArray[e.getValue()] = v.mult(scale);
      vnArray[e.getValue()] = sum;
      texArray[e.getValue()] = new Vector3f(v.x / w * 8, v.z / d * 8, e.getValue() % 2);
      colArray[e.getValue()] = new Vector4f(colSum, colSum, 0.5f, 1.0f);
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
    m.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colArray));
    
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
          Tile tile = chunk.get(x, y, z);
          if (tile != Tile.AIR) {
            float color = chunk.getColor(x, y, z);
            if (chunk.get(x, y - 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x, y, z + 1),
                  topTexture(tile), color);
            }
            if (chunk.get(x, y + 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x, y + 1, z),
                  bottomTexture(tile), color);
            }
            if (chunk.get(x - 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x, y, z),
                  sideTexture(tile), color);
            }
            if (chunk.get(x + 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y, z + 1),
                  sideTexture(tile), color);
            }
            if (chunk.get(x, y, z - 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y, z),
                  sideTexture(tile), color);
            }
            if (chunk.get(x, y, z + 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y, z + 1),
                  sideTexture(tile), color);
            }
          }
        }
      }
    }
    return myMesh;
  }

  private static MyTexture sideTexture(Tile tile) {
    return new MyTexture(tile);
  }

  private static MyTexture bottomTexture(Tile tile) {
    return new MyTexture(tile);
  }

  private static MyTexture topTexture(Tile tile) {
    return new MyTexture(tile);
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
