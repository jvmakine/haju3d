package fi.haju.haju3d.client.ui;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;

import fi.haju.haju3d.client.ChunkProvider;
import fi.haju.haju3d.client.CloseEventHandler;
import fi.haju.haju3d.client.ui.input.InputActions;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.World;

/**
 * Renderer application for rendering chunks from the server
 */
public class ChunkRenderer extends SimpleApplication {
  private static final float SCALE = 1;
  private static final int CHUNK_CUT_OFF = 3;
  private static final Vector3f lightDir = new Vector3f(-0.9140114f, 0.29160172f, -0.2820493f).negate();
  
  private ChunkSpatialBuilder builder;
  private DirectionalLight light;
  private CloseEventHandler closeEventHandler;
  private ChunkProvider chunkProvider;
  private Vector3f lastLocation = null;

  private World world = new World();
  private boolean isFullScreen = false;
  private Node terrainNode = new Node("terrain");
  private WorldBuilder worldBuilder;

  public ChunkRenderer(ChunkProvider chunkProvider) {
    this.chunkProvider = chunkProvider;
    setDisplayMode();
  }

  private void setDisplayMode() {
    AppSettings settings = new AppSettings(true);
    settings.setVSync(true);
    settings.setAudioRenderer(null);
    settings.setFullscreen(isFullScreen);
    setSettings(settings);
    setShowSettings(false);
  }

  @Override
  public void simpleInitApp() {
    assetManager.registerLocator("assets", new ClasspathLocator().getClass());
    this.builder = new ChunkSpatialBuilder(assetManager);
    this.worldBuilder = new WorldBuilder(world, chunkProvider, builder);
    new Thread(worldBuilder).start();

    setupInput();
    setupCamera();
    setupSky();
    setupLighting();
    setupPostFilters();

    rootNode.attachChild(terrainNode);
  }

  private void setupCamera() {
    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);
    getCamera().setLocation(getGlobalPosition(new Vector3i().add(32, 62, 62)));
  }

  private void setupInput() {
    inputManager.addMapping(InputActions.CHANGE_FULL_SCREEN, new KeyTrigger(KeyInput.KEY_F));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean keyPressed, float tpf) {
        isFullScreen = !isFullScreen;
        setDisplayMode();
        restart();
      }
    }, InputActions.CHANGE_FULL_SCREEN);
  }

  private void updateWorldMesh() {
    worldBuilder.setPosition(world.getChunkIndex(getWorldPosition(getCamera().getLocation())));
  }

  private void updateChunkSpatialVisibility() {
    Vector3i chunkIndex = getChunkIndexForLocation();
    terrainNode.detachAllChildren();
    for (Vector3i pos : chunkIndex.getSurroundingPositions(CHUNK_CUT_OFF, CHUNK_CUT_OFF, CHUNK_CUT_OFF)) {
      ChunkSpatial cs = worldBuilder.getChunkSpatial(pos);
      if (cs != null) {
        terrainNode.attachChild(pos.equals(chunkIndex) ? cs.highDetail : cs.lowDetail);
      }
    }
  }

  private Vector3i getChunkIndexForLocation() {
    Vector3f location = getCamera().getLocation();
    Vector3i worldPosition = getWorldPosition(location);
    return world.getChunkIndex(worldPosition);
  }

  public Vector3f getGlobalPosition(Vector3i worldPosition) {
    return new Vector3f(worldPosition.x * SCALE, worldPosition.y * SCALE, worldPosition.z * SCALE);
  }

  private Vector3i getWorldPosition(Vector3f location) {
    return new Vector3i((int) Math.floor(location.x / SCALE), (int) Math.floor(location.y / SCALE), (int) Math.floor(location.z / SCALE));
  }

  private void setupLighting() {
    light = new DirectionalLight();
    light.setDirection(lightDir.normalizeLocal());
    light.setColor(new ColorRGBA(1f, 1f, 1f, 1f).mult(1.0f));
    rootNode.addLight(light);

    AmbientLight al = new AmbientLight();
    al.setColor(new ColorRGBA(1f, 1f, 1f, 1f).mult(0.6f));
    rootNode.addLight(al);

    DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 4);
    dlsr.setLight(light);
    dlsr.setShadowIntensity(0.4f);
    dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
    viewPort.addProcessor(dlsr);
  }

  private void setupPostFilters() {
    FilterPostProcessor fpp = new FilterPostProcessor(assetManager);

    BloomFilter bloom = new BloomFilter();
    bloom.setDownSamplingFactor(2);
    bloom.setBlurScale(1.37f);
    bloom.setExposurePower(4.30f);
    bloom.setExposureCutOff(0.2f);
    bloom.setBloomIntensity(0.8f);
    fpp.addFilter(bloom);

    WaterFilter water = new WaterFilter(rootNode, lightDir);
    water.setCenter(new Vector3f(319.6663f, -18.367947f, -236.67674f));
    water.setRadius(26000);
    water.setWaterHeight(14);
    water.setWaveScale(0.01f);
    water.setSpeed(0.4f);
    water.setMaxAmplitude(1.0f);
    water.setFoamExistence(new Vector3f(1f, 4, 0.5f).mult(0.2f));
    water.setFoamTexture((Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
    water.setRefractionStrength(0.1f);
    fpp.addFilter(water);

    CartoonEdgeFilter rimLightFilter = new CartoonEdgeFilter();
    rimLightFilter.setEdgeColor(ColorRGBA.Black);
    rimLightFilter.setEdgeIntensity(0.5f);
    rimLightFilter.setEdgeWidth(1.0f);
    rimLightFilter.setNormalSensitivity(0.0f);
    rimLightFilter.setNormalThreshold(0.0f);
    rimLightFilter.setDepthSensitivity(20.0f);
    rimLightFilter.setDepthThreshold(0.0f);
    fpp.addFilter(rimLightFilter);

    viewPort.addProcessor(fpp);
  }

  private void setupSky() {
    Texture west = assetManager.loadTexture("fi/haju/haju3d/client/textures/sky9-left.jpg");
    Texture east = assetManager.loadTexture("fi/haju/haju3d/client/textures/sky9-right.jpg");
    Texture north = assetManager.loadTexture("fi/haju/haju3d/client/textures/sky9-front.jpg");
    Texture south = assetManager.loadTexture("fi/haju/haju3d/client/textures/sky9-back.jpg");
    Texture up = assetManager.loadTexture("fi/haju/haju3d/client/textures/sky9-top.jpg");
    Texture down = assetManager.loadTexture("fi/haju/haju3d/client/textures/sky9-top.jpg");
    rootNode.attachChild(SkyFactory.createSky(assetManager, west, east, north, south, up, down));
  }

  @Override
  public void simpleUpdate(float tpf) {
    updateWorldMesh();
    updateChunkSpatialVisibility();
    Vector3f position = cam.getLocation().clone();
    Vector3i chunkIndex = getChunkIndexForLocation();
    // Check for collisions
    if(lastLocation != null) {
      Ray movement = new Ray(lastLocation, position.subtract(lastLocation).normalize());
      float distance = lastLocation.distance(position);
      //TODO: More efficient way of selecting chunks to check against
      for (Vector3i pos : chunkIndex.getSurroundingPositions(1, 1, 1)) {
        ChunkSpatial cs = worldBuilder.getChunkSpatial(pos);
        CollisionResults collision = new CollisionResults();
        if(cs != null && cs.lowDetail.collideWith(movement, collision) != 0) {
          Vector3f closest = collision.getClosestCollision().getContactPoint();
          boolean collided = closest.distance(lastLocation) <= distance + 1.5f; 
          if(collided) {
            cam.setLocation(lastLocation);
            position = lastLocation;
            break;
          }
        }
      }
    }
    lastLocation = position;
  }

  @Override
  public void destroy() {
    super.destroy();
    if (this.worldBuilder != null) {
      this.worldBuilder.stop();
    }
    if (closeEventHandler != null) {
      this.closeEventHandler.onClose();
    }
  }

  public void setCloseEventHandler(CloseEventHandler closeEventHandler) {
    this.closeEventHandler = closeEventHandler;
  }
}
