package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import fi.haju.haju3d.client.ui.input.InputActions;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.client.ui.mesh.MyMesh;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.TilePosition;
import fi.haju.haju3d.protocol.world.World;

import java.util.HashMap;
import java.util.Map;

public class CharacterCreationApp extends SimpleApplication {

  public static final float SCALE = 0.1f;
  private static final float SELECTOR_DISTANCE = 100f;
  private World world;
  private boolean dig;
  private boolean build;

  public static void main(String[] args) {
    CharacterCreationApp app = new CharacterCreationApp();
    SimpleApplicationUtils.configureSimpleApplication(app);
    app.start();
  }

  private final Map<Vector3i, Spatial> spatials = new HashMap<>();

  @Override
  public void simpleInitApp() {
    this.world = createWorld();
    flyCam.setMoveSpeed(20);
    flyCam.setRotationSpeed(2.0f);
    setupLights();
    setupCrosshair();
    updateChunkMesh(new Vector3i());
    setupControls();
  }

  private void setupCrosshair() {
    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
    BitmapText crossHair = new BitmapText(guiFont, false);
    crossHair.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    crossHair.setText("+");
    crossHair.setLocalTranslation(settings.getWidth() / 2 - crossHair.getLineWidth() / 2,
        settings.getHeight() / 2 + crossHair.getLineHeight() / 2, 0);
    guiNode.attachChild(crossHair);
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
      setTile(Tile.BRICK);
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

  private void setupLights() {
    DirectionalLight light = new DirectionalLight();
    Vector3f lightDir = new Vector3f(-1, -2, -3);
    light.setDirection(lightDir.normalizeLocal());
    light.setColor(new ColorRGBA(1f, 1f, 1f, 1f).mult(1.0f));
    rootNode.addLight(light);

    AmbientLight ambient = new AmbientLight();
    ambient.setColor(ColorRGBA.Blue);
    rootNode.addLight(ambient);

    DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 4);
    dlsr.setLight(light);
    dlsr.setShadowIntensity(0.4f);
    dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
    viewPort.addProcessor(dlsr);

    CartoonEdgeFilter rimLightFilter = new CartoonEdgeFilter();
    rimLightFilter.setEdgeColor(ColorRGBA.Black);

    rimLightFilter.setEdgeIntensity(0.5f);
    rimLightFilter.setEdgeWidth(1.0f);

    rimLightFilter.setNormalSensitivity(0.0f);
    rimLightFilter.setNormalThreshold(0.0f);

    rimLightFilter.setDepthSensitivity(20.0f);
    rimLightFilter.setDepthThreshold(0.0f);
    FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
    fpp.addFilter(rimLightFilter);
    viewPort.addProcessor(fpp);
  }

  private void updateChunkMesh(Vector3i pos) {
    Spatial oldSpatial = spatials.get(pos);
    if (oldSpatial != null) {
      rootNode.detachChild(oldSpatial);
    }

    MyMesh myMesh = ChunkSpatialBuilder.makeCubeMesh(world, pos);
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
      return TilePosition.getTilePosition(SCALE, world.getChunkSize(), collisionPoint);
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
    World world = new World(32);
    final int sz = world.getChunkSize();

    for (int x = 0; x < 10; x++) {
      for (int y = 0; y < 10; y++) {
        for (int z = 0; z < 10; z++) {
          Vector3i pos = new Vector3i(x - 5, y - 5, z - 5);
          world.setChunk(pos, new Chunk(sz, sz, sz, 0, pos, Tile.AIR));
        }
      }
    }

    Chunk chunk = new Chunk(sz, sz, sz, 0, new Vector3i());
    chunk.set(new Chunk.GetValue() {
      @Override
      public Tile getValue(int x, int y, int z) {
        return (x - sz / 2) * (x - sz / 2) + (y - sz / 2) * (y - sz / 2) + (z - sz / 2) * (z - sz / 2) < (sz / 2) * (sz / 2) ? Tile.BRICK : Tile.AIR;
      }
    });
    world.setChunk(new Vector3i(), chunk);
    return world;
  }

  private Material makeColorMaterial(ColorRGBA color) {
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    return mat;
  }

  public Spatial makeSpatial(MyMesh myMesh) {
    Mesh m = new ChunkSpatialBuilder.SimpleMeshBuilder(myMesh).build();
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(makeColorMaterial(ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

}
