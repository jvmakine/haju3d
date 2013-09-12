package fi.haju.haju3d.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

/**
 * Testing application that builds a smoothed grid mesh.
 */
public class TestGrid extends SimpleApplication {

  private static final int WIDTH = 120;
  private static final int HEIGHT = 120;
  private static final int DEPTH = 120;

  private Grid grid = new Grid(WIDTH, HEIGHT, DEPTH);
  private Spatial groundObject;
  private Spatial characterObject;

  private static float[] make3dPerlinNoise(long seed, int w, int h, int d) {
    Random random = new Random(seed);
    float[] data = new float[w * h * d];
    for (int sc = 4; sc != 128; sc *= 2) {
      add3dNoise(random, data, w, h, d, sc, FastMath.pow(0.5f * sc * 1.0f, 1.0f));
    }
    return data;
  }

  private static void add3dNoise(Random random, float[] data, int w, int h, int d, int scale, float amp) {
    int nw = w / scale + 2;
    int nh = h / scale + 2;
    int nd = d / scale + 2;
    int n = nw * nh * nd;
    float noise[] = new float[n];
    for (int i = 0; i < n; i++) {
      noise[i] = (float) (random.nextDouble() - 0.5) * amp;
    }

    int nwh = nw * nh;

    for (int z = 0; z < d; z++) {
      float zt = (float) (z % scale) / scale;
      int zs = z / scale;
      for (int y = 0; y < h; y++) {
        float yt = (float) (y % scale) / scale;
        int ys = y / scale;
        for (int x = 0; x < w; x++) {
          float xt = (float) (x % scale) / scale;
          int xs = x / scale;

          float n1 = noise[xs + ys * nw + zs * nwh];
          float n2 = noise[xs + 1 + ys * nw + zs * nwh];
          float n3 = noise[xs + ys * nw + nw + zs * nwh];
          float n4 = noise[xs + 1 + ys * nw + nw + zs * nwh];

          float n5 = noise[xs + ys * nw + zs * nwh + nwh];
          float n6 = noise[xs + 1 + ys * nw + zs * nwh + nwh];
          float n7 = noise[xs + ys * nw + nw + zs * nwh + nwh];
          float n8 = noise[xs + 1 + ys * nw + nw + zs * nwh + nwh];

          data[x + y * w + z * w * h] += interpolateLinear3d(xt, yt, zt, n1, n2, n3, n4, n5, n6, n7, n8);
        }
      }
    }
  }

  private static enum Tile {
    AIR, GROUND
  }

  private static final class Grid {
    private Tile[] data;
    private int width;
    private int height;
    private int depth;

    public Grid(int width, int height, int depth) {
      this.width = width;
      this.height = height;
      this.depth = depth;

      int n = width * height * depth;
      this.data = new Tile[n];
      for (int i = 0; i < n; i++) {
        data[i] = Tile.AIR;
      }
    }

    public void set(int x, int y, int z, Tile value) {
      if (isInside(x, y, z)) {
        data[getIndex(x, y, z)] = value;
      }
    }

    private int getIndex(int x, int y, int z) {
      return x + y * width + z * width * height;
    }

    private boolean isInside(int x, int y, int z) {
      return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
    }

    public Tile get(int x, int y, int z) {
      if (isInside(x, y, z)) {
        return data[getIndex(x, y, z)];
      } else {
        return Tile.AIR;
      }
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }

    public int getDepth() {
      return depth;
    }
  }

  @Override
  public void simpleInitApp() {
    initGrid();

    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);

    setupGridAsMesh();
    setupLighting();
    setupCharacter();
    setupToruses();
  }

  private void setupToruses() {
    Torus torus = new Torus(20, 20, 0.5f, 1.0f);

    BatchNode batch = new BatchNode("batch");
    batch.setShadowMode(ShadowMode.CastAndReceive);

    Random rnd = new Random(0L);
    Material red = makeColorMaterial(ColorRGBA.Red);
    Material blue = makeColorMaterial(ColorRGBA.Blue);
    for (int i = 0; i < 50; i++) {
      CollisionResults res = new CollisionResults();
      float x = (float) ((rnd.nextDouble() * WIDTH) - WIDTH * 0.5);
      float z = (float) (-(rnd.nextDouble() * DEPTH));
      Ray r = new Ray(new Vector3f(x, HEIGHT, z), Vector3f.UNIT_Y.negate());
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

  private void setupGridAsMesh() {
    float scale = 1;

    MyMesh myMesh = new MyMesh();

    int w = grid.getWidth();
    int h = grid.getHeight();
    int d = grid.getDepth();
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          if (grid.get(x, y, z) == Tile.GROUND) {
            if (grid.get(x, y - 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y, z + 1),
                  new Vector3f(x, y, z + 1));
            }
            if (grid.get(x, y + 1, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x, y + 1, z));
            }
            if (grid.get(x - 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z + 1),
                  new Vector3f(x, y + 1, z + 1),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x, y, z));
            }
            if (grid.get(x + 1, y, z) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x + 1, y, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y + 1, z + 1),
                  new Vector3f(x + 1, y, z + 1));
            }
            if (grid.get(x, y, z - 1) == Tile.AIR) {
              myMesh.addFace(
                  new Vector3f(x, y, z),
                  new Vector3f(x, y + 1, z),
                  new Vector3f(x + 1, y + 1, z),
                  new Vector3f(x + 1, y, z));
            }
            if (grid.get(x, y, z + 1) == Tile.AIR) {
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
    float[] noiseR = make3dPerlinNoise(1, tw, th, td);
    float[] noiseG = make3dPerlinNoise(2, tw, th, td);

    float[] cArray = new float[vertexIndex.size() * 4];
    for (Map.Entry<MyVertex, Integer> e : vertexIndex.entrySet()) {
      int vi = e.getValue();
      Vector3f pos = e.getKey().v;
      float tx = pos.x / grid.width * tw;
      float ty = pos.y / grid.height * th;
      float tz = pos.z / grid.depth * td;

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

    groundObject.setLocalTranslation(-grid.width * scale * 0.5f, -grid.height * scale * 0.5f, -grid.depth * scale);
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
      return interpolateLinear3d(
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

  private void initGrid() {
    int w = grid.getWidth();
    int h = grid.getHeight();
    int d = grid.getDepth();

    float[] noise = make3dPerlinNoise(12, w, h, d);
    float thres = h / 3;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          float v = FastMath.abs(y);
          if (y < h / 5) {
            v += interpolateLinear(y / (h / 5), -10, 0);
          }
          v += noise[x + y * w + z * w * h] * 3;
          grid.set(x, y, z, v < thres ? Tile.GROUND : Tile.AIR);
        }
      }
    }

    filterFloaters();
  }

  private void filterFloaters() {
    Grid ground = new Grid(grid.getWidth(), grid.getHeight(), grid.getDepth());
    new FloodFill(ground, grid).fill();
    grid = ground;
  }

  private static class FloodFill {
    private List<Vector3i> front = new ArrayList<>();
    private Set<Vector3i> visited = new HashSet<>();
    private Grid ground;
    private Grid orig;

    public FloodFill(Grid ground, Grid orig) {
      this.ground = ground;
      this.orig = orig;
    }

    public void fill() {
      test(new Vector3i(0, 0, 0));
      while (!front.isEmpty()) {
        Vector3i v = front.remove(front.size() - 1);
        test(v.add(1, 0, 0));
        test(v.add(-1, 0, 0));
        test(v.add(0, 1, 0));
        test(v.add(0, -1, 0));
        test(v.add(0, 0, 1));
        test(v.add(0, 0, -1));
      }
    }

    private void test(Vector3i n) {
      if (visited.contains(n)) {
        return;
      }
      if (orig.get(n.x, n.y, n.z) == Tile.AIR) {
        return;
      }
      ground.set(n.x, n.y, n.z, Tile.GROUND);
      visited.add(n);
      front.add(n);
    }
  }

  private static float interpolateLinear(float t, float v1, float v2) {
    return v1 + (v2 - v1) * t;
  }

  private static float interpolateLinear2d(
      float xt, float yt, float n1, float n2, float n3, float n4) {
    float x1 = interpolateLinear(xt, n1, n2);
    float x2 = interpolateLinear(xt, n3, n4);
    return interpolateLinear(yt, x1, x2);
  }

  private static float interpolateLinear3d(
      float xt, float yt, float zt,
      float n1, float n2, float n3, float n4, float n5, float n6, float n7, float n8) {

    float z1 = interpolateLinear2d(xt, yt, n1, n2, n3, n4);
    float z2 = interpolateLinear2d(xt, yt, n5, n6, n7, n8);
    return interpolateLinear(zt, z1, z2);
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
