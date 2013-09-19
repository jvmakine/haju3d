package fi.haju.haju3d.client.ui.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

public class ChunkMeshBuilder {
  private static final int SMOOTH_BUFFER = 2;
  private static final Logger LOGGER = LoggerFactory.getLogger(ChunkMeshBuilder.class);
  
  public ChunkMeshBuilder() {
  }
  
  public Mesh makeMesh(World world, Vector3i chunkIndex) {
    LOGGER.info("makeMesh:" + chunkIndex);
    
    MyMesh myMesh;
    synchronized (world) {
      myMesh = makeCubeMesh(world, chunkIndex);
    }
    smoothMesh(myMesh);

    // only faces based on a real tile should be meshed; the other ones were used for smoothing context
    List<MyFace> realFaces = new ArrayList<>();
    for (MyFace face : myMesh.faces)  {
      if (face.realTile) {
        realFaces.add(face);
      }
    }
    for (MyFace face : myMesh.faces) {
      face.normal = face.v2.v.subtract(face.v1.v).cross(face.v4.v.subtract(face.v1.v)).normalize();
    }
    
    Map<MyVertex, Vector3f> vertexToNormal = new HashMap<>();
    for (MyFace face : realFaces) {
      calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v1);
      calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v2);
      calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v3);
      calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v4);
    }
    
    FloatBuffer vertexes = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
    FloatBuffer vertexNormals = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
    FloatBuffer textures = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
    IntBuffer indexes = BufferUtils.createIntBuffer(realFaces.size() * 6);
    FloatBuffer colors = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 4);
    
    int i = 0;
    for (MyFace face : realFaces) {
      putVector(vertexes, face.v1.v);
      putVector(vertexes, face.v2.v);
      putVector(vertexes, face.v3.v);
      putVector(vertexes, face.v4.v);
      
      if (face.texture == MyTexture.BRICK) {
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
      } else {
        putVector(vertexNormals, vertexToNormal.get(face.v1));
        putVector(vertexNormals, vertexToNormal.get(face.v2));
        putVector(vertexNormals, vertexToNormal.get(face.v3));
        putVector(vertexNormals, vertexToNormal.get(face.v4));
      }
      
      int ti = face.texture.ordinal();
      textures.put(0).put(0).put(ti);
      textures.put(0).put(1).put(ti);
      textures.put(1).put(1).put(ti);
      textures.put(1).put(0).put(ti);
      
      indexes.put(i + 0).put(i + 1).put(i + 3);
      indexes.put(i + 1).put(i + 2).put(i + 3);
      
      float col = face.color;
      colors.put(col).put(col).put(1).put(1);
      colors.put(col).put(col).put(1).put(1);
      colors.put(col).put(col).put(1).put(1);
      colors.put(col).put(col).put(1).put(1);
      
      i += 4;
    }

    Mesh m = new Mesh();
    m.setBuffer(Type.Position, 3, vertexes);
    m.setBuffer(Type.Normal, 3, vertexNormals);
    m.setBuffer(Type.TexCoord, 3, textures);
    m.setBuffer(Type.Index, 1, indexes);
    m.setBuffer(Type.Color, 4, colors);
    
    m.updateBound();
    return m;
  }
  
  private void calcVertexNormal(
      Map<MyVertex, List<MyFace>> vertexFaces,
      Map<MyVertex, Vector3f> vertexToNormal,
      MyVertex v1) {
    
    if (vertexToNormal.containsKey(v1)) {
      return;
    }
    
    Vector3f sum = Vector3f.ZERO.clone();
    List<MyFace> faces = vertexFaces.get(v1);
    for (MyFace f : faces) {
      sum.addLocal(f.normal);
    }
    sum.normalizeLocal();
    
    vertexToNormal.put(v1, sum);
  }

  private void putVector(FloatBuffer vertexes, Vector3f v) {
    vertexes.put(v.x).put(v.y).put(v.z);
  }

  private static MyMesh makeCubeMesh(World world, Vector3i chunkIndex) {
    MyMesh myMesh = new MyMesh();
    
    Vector3i w1o = world.getWorldPosition(chunkIndex);
    Vector3i w2o = world.getWorldPosition(chunkIndex.add(1, 1, 1));
    
    Vector3i w1 = w1o.add(-SMOOTH_BUFFER, -SMOOTH_BUFFER, -SMOOTH_BUFFER);
    Vector3i w2 = w2o.add(SMOOTH_BUFFER, SMOOTH_BUFFER, SMOOTH_BUFFER);
    
    for (int x = w1.x; x < w2.x; x++) {
      for (int y = w1.y; y < w2.y; y++) {
        for (int z = w1.z; z < w2.z; z++) {
          Tile tile = world.get(x, y, z);
          boolean realTile =
              x >= w1o.x && x < w2o.x &&
              y >= w1o.y && y < w2o.y &&
              z >= w1o.z && z < w2o.z;
          if (tile != Tile.AIR) {
            float color = world.getColor(x, y, z);
            if (world.get(x, y - 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x, y, z + 1),
                  bottomTexture(tile), color,
                  realTile);
            }
            if (world.get(x, y + 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x, y + 1, z),
                  topTexture(tile), color,
                  realTile);
            }
            if (world.get(x - 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x, y, z),
                  sideTexture(tile), color,
                  realTile);
            }
            if (world.get(x + 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y, z + 1),
                  sideTexture(tile), color,
                  realTile);
            }
            if (world.get(x, y, z - 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y, z),
                  sideTexture(tile), color,
                  realTile);
            }
            if (world.get(x, y, z + 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y, z + 1),
                  sideTexture(tile), color,
                  realTile);
            }
          }
        }
      }
    }
    return myMesh;
  }

  private static MyTexture sideTexture(Tile tile) {
    switch (tile) {
    case GROUND:
      return MyTexture.DIRT;
    case ROCK:
      return MyTexture.ROCK;
    case BRICK:
      return MyTexture.BRICK;
    case AIR:
    }
    throw new IllegalStateException("Unknown case: " + tile);
  }

  private static MyTexture bottomTexture(Tile tile) {
    switch (tile) {
    case GROUND:
      return MyTexture.DIRT;
    case ROCK:
      return MyTexture.ROCK;
    case BRICK:
      return MyTexture.BRICK;
    case AIR:
    }
    throw new IllegalStateException("Unknown case: " + tile);
  }

  private static MyTexture topTexture(Tile tile) {
    switch (tile) {
    case GROUND:
      return MyTexture.GRASS;
    case ROCK:
      return MyTexture.ROCK;
    case BRICK:
      return MyTexture.BRICK;
    case AIR:
    }
    throw new IllegalStateException("Unknown case: " + tile);
  }

  private static void smoothMesh(MyMesh myMesh) {
    for (int i = 0; i < SMOOTH_BUFFER; i++) {
      Map<MyVertex, Vector3f> newPos = new HashMap<>();
      for (Map.Entry<MyVertex, List<MyFace>> e : myMesh.vertexFaces.entrySet()) {
        Vector3f sum = Vector3f.ZERO.clone();
        List<MyFace> faces = e.getValue();
        boolean hasBrick = false;
        for (MyFace f : faces) {
          sum.addLocal(f.getCenter());
          hasBrick = hasBrick | (f.texture == MyTexture.BRICK);
        }
        sum.divideLocal(faces.size());
        if (!hasBrick) {
          newPos.put(e.getKey(), sum);
        }
      }
      for (Map.Entry<MyVertex, Vector3f> e : newPos.entrySet()) {
        e.getKey().v.set(e.getValue());
      }
    }
  }
}
