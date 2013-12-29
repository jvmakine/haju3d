package fi.haju.haju3d.client.bones;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import fi.haju.haju3d.client.SimpleApplicationUtils;
import fi.haju.haju3d.client.chunk.light.ChunkLightManager;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.client.ui.mesh.MyMesh;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.*;

import java.util.HashMap;
import java.util.Map;

public class MarchingCubesMesherTest extends SimpleApplication {
  public static final float SCALE = 0.1f;
  public static final Tile GROUND_TILE = Tile.GROUND;

  private ChunkCoordinateSystem chunkCoordinateSystem = new ChunkCoordinateSystem(32);

  private World world;
  private ChunkLightManager light = new ChunkLightManager();

  public static void main(String[] args) {
    MarchingCubesMesherTest app = new MarchingCubesMesherTest();
    SimpleApplicationUtils.configureSimpleApplication(app);
    app.start();
  }

  private final Map<Vector3i, Spatial> spatials = new HashMap<>();

  @Override
  public void simpleInitApp() {
    viewPort.setBackgroundColor(ColorRGBA.DarkGray);
    this.world = createWorld();
    flyCam.setMoveSpeed(21);
    flyCam.setRotationSpeed(2.0f);
    SimpleApplicationUtils.addLights(this);
    SimpleApplicationUtils.addCartoonEdges(this);
    SimpleApplicationUtils.setupCrosshair(this, this.settings);
    //updateChunkMesh(new ChunkPosition(0, 0, 0));

    ByteArray3d grid = makeBoneMeshGrid();
    Mesh mesh = MarchingCubesMesher.createMesh(grid);
    final Geometry geom = new Geometry("ColoredMesh", mesh);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    geom.setLocalTranslation(0, 0, 100);
    rootNode.attachChild(geom);


    for (int i = 0; i < 20; i++) {
      long t0 = System.currentTimeMillis();
      bench1();
      long t1 = System.currentTimeMillis();
      System.out.println("tt_grid = " + (t1 - t0));
    }
    for (int i = 0; i < 20; i++) {
      long t0 = System.currentTimeMillis();
      bench2(grid);
      long t1 = System.currentTimeMillis();
      System.out.println("tt_mc = " + (t1 - t0));
    }
  }

  public void bench1() {
    MyMesh myMesh = ChunkSpatialBuilder.makeCubeMesh(world, new ChunkPosition(0, 0, 0), light);
    ChunkSpatialBuilder.smoothMesh(myMesh);
    ChunkSpatialBuilder.prepareMesh(myMesh);
    Mesh m = new ChunkSpatialBuilder.SimpleMeshBuilder(myMesh).build();

    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setLocalTranslation(0, 0, 100);
    rootNode.attachChild(geom);
    System.out.println("m.getTriangleCount() = " + m.getTriangleCount());
  }

  public void bench2(ByteArray3d grid) {
    Mesh m = MarchingCubesMesher.createMesh(grid);
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setLocalTranslation(0, 0, -100);
    rootNode.attachChild(geom);
    System.out.println("m.getTriangleCount() = " + m.getTriangleCount());
  }

  private void updateChunkMesh(ChunkPosition pos) {
    Spatial oldSpatial = spatials.get(pos);
    if (oldSpatial != null) {
      rootNode.detachChild(oldSpatial);
    }

    MyMesh myMesh = ChunkSpatialBuilder.makeCubeMesh(world, pos, light);
    ChunkSpatialBuilder.smoothMesh(myMesh);
    ChunkSpatialBuilder.prepareMesh(myMesh);
    Spatial spatial = makeSpatial(myMesh);
    spatial.setLocalScale(SCALE);
    spatials.put(pos, spatial);
    rootNode.attachChild(spatial);
  }

  private World createWorld() {
    World world = new World(chunkCoordinateSystem);
    final int sz = chunkCoordinateSystem.getChunkSize();

    for (int x = 0; x < 10; x++) {
      for (int y = 0; y < 10; y++) {
        for (int z = 0; z < 10; z++) {
          ChunkPosition pos = new ChunkPosition(x - 5, y - 5, z - 5);
          world.setChunk(pos, new Chunk(sz, 0, pos, Tile.AIR));
        }
      }
    }

    ChunkPosition cp = new ChunkPosition(0, 0, 0);
    Chunk chunk = new Chunk(sz, 0, cp);
    chunk.set(new Chunk.GetValue() {
      @Override
      public Tile getValue(int x, int y, int z) {
        int xd = x - sz / 2;
        int yd = y - sz / 2;
        int zd = z - sz / 2;
        int bsz = sz / 2;
        return xd * xd + yd * yd + zd * zd < bsz * bsz ? GROUND_TILE : Tile.AIR;
      }
    });
    world.setChunk(cp, chunk);
    return world;
  }

  private static ByteArray3d makeBoneMeshGrid() {
    int sz = 32;
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
          int bsz = (int) (sz / 2.5);
          int value = bsz - (int) FastMath.sqrt(xd * xd + yd * yd + zd * zd);

          value = (value) + 32;
          if (value < 0) value = 0;
          if (value > 63) value = 63;

          grid.set(x, y, z, (byte) value);
        }
      }
    }
    return grid;
  }

  public Spatial makeSpatial(MyMesh myMesh) {
    Mesh m = new ChunkSpatialBuilder.SimpleMeshBuilder(myMesh).build();
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

}
