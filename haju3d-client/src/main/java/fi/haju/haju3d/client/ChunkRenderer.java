package fi.haju.haju3d.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Torus;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.util.BufferUtils;

import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.util.noise.InterpolationUtil;
import fi.haju.haju3d.util.noise.PerlinNoiseUtil;

/**
 * Renderer application for rendering chunks from the server
 */
public class ChunkRenderer extends SimpleApplication {

  private Chunk chunk = null;
  private Spatial groundObject;
  private Spatial characterObject;
  
  public ChunkRenderer(Chunk chunk) {
    this.chunk = chunk;
  }

  @Override
  public void simpleInitApp() {
    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);

    setupChunkAsMesh();
    setupLighting();
    setupCharacter();
    setupToruses();
  }

  private void setupToruses() {
    Torus torus = new Torus(20, 20, 0.5f, 1.0f);

    BatchNode batch = new BatchNode("batch");
    batch.setShadowMode(ShadowMode.CastAndReceive);

    Random rnd = new Random(chunk.getSeed());
    Material red = makeColorMaterial(ColorRGBA.Red);
    Material blue = makeColorMaterial(ColorRGBA.Blue);
    for (int i = 0; i < 50; i++) {
      CollisionResults res = new CollisionResults();
      float x = (float) ((rnd.nextDouble() * chunk.getWidth()) - chunk.getHeight() * 0.5);
      float z = (float) (-(rnd.nextDouble() * chunk.getDepth()));
      Ray r = new Ray(new Vector3f(x, chunk.getHeight(), z), Vector3f.UNIT_Y.negate());
      int collideWith = groundObject.collideWith(r, res);
      if (collideWith != 0) {
        Vector3f pt = res.getClosestCollision().getContactPoint().subtract(0f, 0.1f, 0f);
        Geometry t = new Geometry("torus" + i, torus);
        int col = rnd.nextInt(2);
        t.setMaterial(col == 0 ? red : blue);
        float angle = (float) (rnd.nextDouble() * Math.PI);
        t.setLocalRotation(new Quaternion(new float[] { 0, angle, 0 }));
        t.setLocalTranslation(pt);
        batch.attachChild(t);
      }
    }

    batch.batch();
    rootNode.attachChild(batch);
  }

  private void setupCharacter() {
    Box characterMesh = new Box(1, 0.5f, 1);
    characterObject = new Geometry("Character", characterMesh);
    characterObject.setMaterial(makeColorMaterial(ColorRGBA.Red));
    characterObject.setShadowMode(ShadowMode.CastAndReceive);
    rootNode.attachChild(characterObject);
  }

  private static class MyVertex {
    private Vector3f v;

    public MyVertex(Vector3f v) {
      this.v = v;
    }
  }

  private static class MyFace {
    private MyVertex v1;
    private MyVertex v2;
    private MyVertex v3;
    private MyVertex v4;
    private Vector3f normal;

    public MyFace(MyVertex v1, MyVertex v2, MyVertex v3, MyVertex v4) {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      this.v4 = v4;
    }

    public Vector3f getCenter() {
      return v1.v.clone().addLocal(v2.v).addLocal(v3.v).addLocal(v4.v).divide(4);
    }
  }

  private static class MyMesh {
    private Map<Vector3f, MyVertex> vectorToVertex = new HashMap<>();
    private Map<MyVertex, List<MyFace>> vertexFaces = new HashMap<>();
    private List<MyFace> faces = new ArrayList<>();

    public void addFace(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4) {
      MyFace face = new MyFace(getVertex(v1), getVertex(v2), getVertex(v3), getVertex(v4));

      addVertexFace(face, face.v1);
      addVertexFace(face, face.v2);
      addVertexFace(face, face.v3);
      addVertexFace(face, face.v4);
      faces.add(face);
    }

    private MyVertex getVertex(Vector3f v1) {
      MyVertex v = vectorToVertex.get(v1);
      if (v == null) {
        v = new MyVertex(v1);
        vectorToVertex.put(v1, v);
      }
      return v;
    }

    private void addVertexFace(MyFace face, MyVertex v1) {
      List<MyFace> faces = vertexFaces.get(v1);
      if (faces == null) {
        faces = new ArrayList<>();
        vertexFaces.put(v1, faces);
      }
      faces.add(face);
    }
  }

  private void setupChunkAsMesh() {
    float scale = 1;

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

    // mesh smoothing
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
    float[] noiseR = PerlinNoiseUtil.make3dPerlinNoise(1, tw, th, td);
    float[] noiseG = PerlinNoiseUtil.make3dPerlinNoise(2, tw, th, td);

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

    groundObject = new Geometry("ColoredMesh", m);
    Material mat = makeColorMaterial(ColorRGBA.White);
    mat.setBoolean("UseVertexColor", true);
    groundObject.setMaterial(mat);
    groundObject.setShadowMode(ShadowMode.CastAndReceive);

    groundObject.setLocalTranslation(-chunk.getWidth() * scale * 0.5f, -chunk.getHeight() * scale * 0.5f, -chunk.getDepth() * scale);
    rootNode.attachChild(groundObject);
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

  private Material makeColorMaterial(ColorRGBA color) {
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    return mat;
  }

  DirectionalLight light;

  private void setupLighting() {
    light = new DirectionalLight();
    light.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
    light.setColor(new ColorRGBA(1f, 1f, 1f, 1f).mult(0.6f));
    rootNode.addLight(light);

    AmbientLight al = new AmbientLight();
    al.setColor(new ColorRGBA(0.3f, 0.3f, 0.3f, 1));
    rootNode.addLight(al);

    DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 4);
    dlsr.setLight(light);
    dlsr.setShadowIntensity(0.4f);
    dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
    viewPort.addProcessor(dlsr);
  }

  @Override
  public void simpleUpdate(float tpf) {
    CollisionResults res = new CollisionResults();
    Ray r = new Ray(cam.getLocation(), Vector3f.UNIT_Y.negate());
    int collideWith = groundObject.collideWith(r, res);
    if (collideWith != 0) {
      Vector3f pt = res.getClosestCollision().getContactPoint();
    }
  }
}
