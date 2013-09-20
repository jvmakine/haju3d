package fi.haju.haju3d.client.ui.mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import fi.haju.haju3d.client.ui.mesh.MyMesh.MyFaceAndIndex;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

public class ChunkMeshBuilder {
  private static final int SMOOTH_BUFFER = 3;
  private static final Logger LOGGER = LoggerFactory.getLogger(ChunkMeshBuilder.class);
  
  public ChunkMeshBuilder() {
  }
  
  public Mesh makeMesh(World world, Vector3i chunkIndex, boolean useSimpleMesh) {
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
    for (Map.Entry<MyVertex, List<MyFaceAndIndex>> e : myMesh.vertexFaces.entrySet()) {
      Collections.sort(e.getValue(), new Comparator<MyFaceAndIndex>() {
        @Override
        public int compare(MyFaceAndIndex o1, MyFaceAndIndex o2) {
          return Integer.compare(o1.face.zIndex, o2.face.zIndex);
        }
      });
    }
    
    if (useSimpleMesh) {
      return new SimpleMeshBuilder(myMesh, realFaces).build();
    } else {
      return new NewMeshBuilder(myMesh, realFaces).build();
    }
  }
  
  
  private static class NewMeshBuilder {
    private List<MyFace> realFaces;
    private FloatBuffer vertexes;
    private FloatBuffer vertexNormals;
    private FloatBuffer textureUvs;
    private FloatBuffer textureUvs2;
    private FloatBuffer textureUvs3;
    private FloatBuffer textureUvs4;
    private IntBuffer indexes;
    private FloatBuffer[] allUvs;
    
    private MyMesh mesh;
    private int i;
    private Map<MyVertex, Vector3f> vertexToNormal = new HashMap<>();

    public NewMeshBuilder(MyMesh mesh, List<MyFace> realFaces) {
      this.mesh = mesh;
      this.realFaces = realFaces;
      int mul = 4;
      this.vertexes = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.vertexNormals = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs2 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs3 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs4 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.indexes = BufferUtils.createIntBuffer(realFaces.size() * 6 * mul);
      this.allUvs = new FloatBuffer[] {textureUvs, textureUvs2, textureUvs3, textureUvs4};
      
      for (MyFace face : realFaces) {
        calcVertexNormal(mesh.vertexFaces, vertexToNormal, face.v1);
        calcVertexNormal(mesh.vertexFaces, vertexToNormal, face.v2);
        calcVertexNormal(mesh.vertexFaces, vertexToNormal, face.v3);
        calcVertexNormal(mesh.vertexFaces, vertexToNormal, face.v4);
      }
    }

    public Mesh build() {
      i = 0;
      for (MyFace face : realFaces) {
        face.calcCenter();
        
        Vector3f v1 = face.v1.v;
        Vector3f v2 = face.v2.v;
        Vector3f v3 = face.v3.v;
        Vector3f v4 = face.v4.v;
        
        Vector3f n1 = vertexToNormal.get(face.v1);
        Vector3f n2 = vertexToNormal.get(face.v2);
        Vector3f n3 = vertexToNormal.get(face.v3);
        Vector3f n4 = vertexToNormal.get(face.v4);
        
        Vector3f v12 = v1.add(v2).mult(0.5f);
        Vector3f v23 = v2.add(v3).mult(0.5f);
        Vector3f v34 = v3.add(v4).mult(0.5f);
        Vector3f v41 = v4.add(v1).mult(0.5f);
        
        Vector3f n12 = n1.add(n2).normalizeLocal();
        Vector3f n23 = n2.add(n3).normalizeLocal();
        Vector3f n34 = n3.add(n4).normalizeLocal();
        Vector3f n41 = n4.add(n1).normalizeLocal();
        
        Vector3f vc = face.center;
        Vector3f nc = face.normal;

        // v1 quadrant
        makeQuadrant(face,
            v1, v12, vc, v41,
            n1, n12, nc, n41,
            face.v1,
            0.5f, 0.5f, //current face
            0.5f, 0.0f, //below
            0.0f, 0.0f, //below right
            0.0f, 0.5f //right
            );

        // v2 quadrant
        makeQuadrant(face,
            v12, v2, v23, vc,
            n12, n2, n23, nc,
            face.v2,
            0.5f, 0.75f, //above
            0.5f, 0.25f, //current
            0.0f, 0.25f, //right
            0.0f, 0.75f //above right
            );
        
        // v3 quadrant
        makeQuadrant(face,
            vc, v23, v3, v34,
            nc, n23, n3, n34,
            face.v3,
            0.75f, 0.75f, //above left
            0.75f, 0.25f, //left
            0.25f, 0.25f, //current
            0.25f, 0.75f //above
            );

        // v4 quadrant
        makeQuadrant(face,
            v41, vc, v34, v4,
            n41, nc, n34, n4,
            face.v4,
            0.75f, 0.5f, //left
            0.75f, 0.0f, //below left
            0.25f, 0.0f, //below
            0.25f, 0.5f //current
            );
      }

      Mesh m = new Mesh();
      m.setBuffer(Type.Position, 3, vertexes);
      m.setBuffer(Type.Normal, 3, vertexNormals);
      m.setBuffer(Type.TexCoord, 3, textureUvs);
      m.setBuffer(Type.TexCoord2, 3, textureUvs2);
      m.setBuffer(Type.TexCoord3, 3, textureUvs3);
      m.setBuffer(Type.TexCoord4, 3, textureUvs4);
      m.setBuffer(Type.Index, 1, indexes);
      m.updateBound();
      return m;
    }

    private void makeQuadrant(
        MyFace face,
        Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
        Vector3f n1, Vector3f n2, Vector3f n3, Vector3f n4,
        MyVertex vert,
        float tu1, float tv1, float tu2, float tv2, float tu3, float tv3, float tu4, float tv4) {
      putVector(vertexes, v1);
      putVector(vertexes, v2);
      putVector(vertexes, v3);
      putVector(vertexes, v4);
  
      if (face.texture == MyTexture.BRICK) {
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
      } else {
        putVector(vertexNormals, n1);
        putVector(vertexNormals, n2);
        putVector(vertexNormals, n3);
        putVector(vertexNormals, n4);
      }
      
      List<MyFaceAndIndex> faces = new ArrayList<>(mesh.vertexFaces.get(vert));
      while (faces.size() > 4) {
        // remove faces with lowest zindex, *except* the current face which should cover all of the poly
        if (faces.get(0).face == face) {
          faces.remove(1);
        } else {
          faces.remove(0);
        }
      }
      
      for (int fi = 0; fi < faces.size(); fi++) {
        MyFaceAndIndex fi1 = faces.get(fi);
        FloatBuffer uvs = allUvs[fi];
        int ti = fi1.face.texture.ordinal();
        if (fi1.index == 1) {
          putUvs(uvs, tu1, tv1, ti);
        } else if (fi1.index == 2) {
          putUvs(uvs, tu2, tv2, ti);
        } else if (fi1.index == 3) {
          putUvs(uvs, tu3, tv3, ti);
        } else if (fi1.index == 4) {
          putUvs(uvs, tu4, tv4, ti);
        }
      }
      // for unused textures, put all vertexes at 0.0f
      for (int fi = faces.size(); fi < 4; fi++) {
        FloatBuffer uvs = allUvs[fi];
        for (int e = 0; e < 4; e++) {
          uvs.put(0.0f).put(0.0f).put(0);
        }
      }
      
      indexes.put(i + 0).put(i + 1).put(i + 3);
      indexes.put(i + 1).put(i + 2).put(i + 3);
      i += 4;
    }
  }
  
  private static class SimpleMeshBuilder {
    private List<MyFace> realFaces;
    private MyMesh myMesh;
    private Map<MyVertex, Vector3f> vertexToNormal = new HashMap<>();
    
    public SimpleMeshBuilder(MyMesh myMesh, List<MyFace> realFaces) {
      this.myMesh = myMesh;
      this.realFaces = realFaces;
      for (MyFace face : realFaces) {
        calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v1);
        calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v2);
        calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v3);
        calcVertexNormal(myMesh.vertexFaces, vertexToNormal, face.v4);
      }
    }
    
    public Mesh build() {
      
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
  }
  
  private static void calcVertexNormal(
      Map<MyVertex, List<MyFaceAndIndex>> vertexFaces,
      Map<MyVertex, Vector3f> vertexToNormal,
      MyVertex v1) {
    
    if (vertexToNormal.containsKey(v1)) {
      return;
    }
    
    Vector3f sum = Vector3f.ZERO.clone();
    for (MyFaceAndIndex f : vertexFaces.get(v1)) {
      sum.addLocal(f.face.normal);
    }
    sum.normalizeLocal();
    
    vertexToNormal.put(v1, sum);
  }

  private static void putVector(FloatBuffer vertexes, Vector3f v) {
    vertexes.put(v.x).put(v.y).put(v.z);
  }

  private static void putUvs(FloatBuffer textureUvs, float u, float v, int ti) {
    textureUvs.put(u + 0.25f).put(v + 0.25f).put(ti);
    textureUvs.put(u + 0.25f).put(v + 0.0f).put(ti);
    textureUvs.put(u + 0.0f).put(v + 0.0f).put(ti);
    textureUvs.put(u + 0.0f).put(v + 0.25f).put(ti);
  }

  private static MyMesh makeCubeMesh(World world, Vector3i chunkIndex) {
    MyMesh myMesh = new MyMesh();
    
    Vector3i w1o = world.getWorldPosition(chunkIndex);
    Vector3i w2o = world.getWorldPosition(chunkIndex.add(1, 1, 1));
    
    Vector3i w1 = w1o.add(-SMOOTH_BUFFER, -SMOOTH_BUFFER, -SMOOTH_BUFFER);
    Vector3i w2 = w2o.add(SMOOTH_BUFFER, SMOOTH_BUFFER, SMOOTH_BUFFER);
    
    Random r = new Random(0L);
    
    for (int z = w1.z; z < w2.z; z++) {
      for (int y = w1.y; y < w2.y; y++) {
        for (int x = w1.x; x < w2.x; x++) {
          Tile tile = world.get(x, y, z);
          if (tile != Tile.AIR) {
            boolean realTile =
                x >= w1o.x && x < w2o.x &&
                y >= w1o.y && y < w2o.y &&
                z >= w1o.z && z < w2o.z;
            float color = world.getColor(x, y, z);
            if (world.get(x, y - 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x, y, z + 1),
                  bottomTexture(tile), color,
                  realTile,
                  r.nextInt());
            }
            if (world.get(x, y + 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x, y + 1, z),
                  topTexture(tile), color,
                  realTile,
                  r.nextInt());
            }
            if (world.get(x - 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x, y, z),
                  sideTexture(tile), color,
                  realTile,
                  r.nextInt());
            }
            if (world.get(x + 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y, z + 1),
                  sideTexture(tile), color,
                  realTile,
                  r.nextInt());
            }
            if (world.get(x, y, z - 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y, z),
                  sideTexture(tile), color,
                  realTile,
                  r.nextInt());
            }
            if (world.get(x, y, z + 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y, z + 1),
                  sideTexture(tile), color,
                  realTile,
                  r.nextInt());
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
      for (MyFace f : myMesh.faces) {
        f.calcCenter();
      }
      for (Map.Entry<MyVertex, List<MyFaceAndIndex>> e : myMesh.vertexFaces.entrySet()) {
        Vector3f sum = Vector3f.ZERO.clone();
        List<MyFaceAndIndex> faces = e.getValue();
        boolean hasBrick = false;
        for (MyFaceAndIndex f : faces) {
          sum.addLocal(f.face.getCenter());
          hasBrick = hasBrick | (f.face.texture == MyTexture.BRICK);
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
