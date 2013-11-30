package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import fi.haju.haju3d.client.chunk.light.ChunkLightManager;
import fi.haju.haju3d.client.ui.input.InputActions;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.client.ui.mesh.MyMesh;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.TilePosition;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.ChunkCoordinateSystem;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

import java.util.HashMap;
import java.util.Map;

public class CharacterCreationApp extends SimpleApplication {

  public static final float SCALE = 0.1f;
  private static final float SELECTOR_DISTANCE = 100f;
  public static final Tile GROUND_TILE = Tile.GROUND;

  private ChunkCoordinateSystem chunkCoordinateSystem = new ChunkCoordinateSystem(32);

  private World world;
  private boolean dig;
  private boolean build;
  private ChunkLightManager light = new ChunkLightManager();

  public static void main(String[] args) {
    CharacterCreationApp app = new CharacterCreationApp();
    SimpleApplicationUtils.configureSimpleApplication(app);
    app.start();
  }

  private final Map<Vector3i, Spatial> spatials = new HashMap<>();

  @Override
  public void simpleInitApp() {
    this.world = createWorld();
    flyCam.setMoveSpeed(21);
    flyCam.setRotationSpeed(2.0f);
    SimpleApplicationUtils.addLights(this);
    SimpleApplicationUtils.addCartoonEdges(this);
    SimpleApplicationUtils.setupCrosshair(this, this.settings);
    updateChunkMesh(new ChunkPosition(0, 0, 0));
    setupControls();
  }

  private void setupControls() {
    // Dig
    inputManager.addMapping(InputActions.DIG, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        dig = isPressed;
      }
    }, InputActions.DIG);

    // Build
    inputManager.addMapping(InputActions.BUILD, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        build = isPressed;
      }
    }, InputActions.BUILD);

  }


  @Override
  public void simpleUpdate(float tpf) {
    if (dig) {
      setTile(Tile.AIR);
    }
    if (build) {
      setTile(GROUND_TILE);
    }
  }

  private void setTile(Tile tile) {
    TilePosition collisionTile = getCollisionTile();
    if (collisionTile != null) {
      Vector3i t = collisionTile.getTileWithinChunk();

      Chunk chunk = world.getChunk(collisionTile.getChunkPosition());
      for (int x = -1; x < 3; x++) {
        for (int y = -1; y < 3; y++) {
          for (int z = -1; z < 3; z++) {
            if (chunk.isInside(t.x + x, t.y + y, t.z + z)) {
              chunk.set(t.x + x, t.y + y, t.z + z, tile);
            }
          }
        }
      }
      updateChunkMesh(collisionTile.getChunkPosition());
    }
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

  private TilePosition getCollisionTile() {
    Vector3f camPos = cam.getLocation();
    Vector3f collisionPoint = getCollisionPoint(camPos, camPos.add(cam.getDirection().normalize().mult(SELECTOR_DISTANCE)));
    if (collisionPoint != null) {
      return TilePosition.getTilePosition(SCALE, chunkCoordinateSystem.getChunkSize(), collisionPoint);
    }
    return null;
  }

  private Vector3f getCollisionPoint(Vector3f from, Vector3f to) {
    Ray ray = new Ray(from, to.subtract(from).normalize());
    float distance = from.distance(to);
    for (Spatial spatial : spatials.values()) {
      CollisionResults collision = new CollisionResults();
      if (spatial.collideWith(ray, collision) != 0) {
        Vector3f closest = collision.getClosestCollision().getContactPoint();
        boolean collided = closest.distance(from) <= distance;
        if (collided) {
          return closest;
        }
      }
    }
    return null;
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

  public Spatial makeSpatial(MyMesh myMesh) {
    Mesh m = new ChunkSpatialBuilder.SimpleMeshBuilder(myMesh).build();
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

}
