package fi.haju.haju3d.client.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;

import fi.haju.haju3d.client.ChunkProcessor;
import fi.haju.haju3d.client.ChunkProvider;
import fi.haju.haju3d.client.CloseEventHandler;
import fi.haju.haju3d.client.ui.input.InputActions;
import fi.haju.haju3d.client.ui.mesh.ChunkMeshBuilder;
import fi.haju.haju3d.client.ui.mesh.ChunkMeshBuilder.LodSpatial;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.World;

/**
 * Renderer application for rendering chunks from the server
 */
public class ChunkRenderer extends SimpleApplication {
  private static final float scale = 1;
  private ChunkMeshBuilder builder;
  private DirectionalLight light;
  private CloseEventHandler closeEventHandler;
  private ChunkProvider chunkProvider;
  
  private World world = new World();
  private Set<Vector3i> meshed = new HashSet<>();
  private boolean useSimpleMesh = false;
  private boolean isFullScreen = false;
  
  public ChunkRenderer(ChunkProvider chunkProvider) {
    this.chunkProvider = chunkProvider;
    setDisplayMode();
  }

  private void setDisplayMode() {
    AppSettings settings = new AppSettings(true);
//    settings.setResolution(1280, 720);
    settings.setVSync(true);
    settings.setAudioRenderer(null);
    settings.setFullscreen(isFullScreen);
    setSettings(settings);
    setShowSettings(false);
  }

  @Override
  public void simpleInitApp() {
    assetManager.registerLocator("assets", new ClasspathLocator().getClass());
    builder = new ChunkMeshBuilder(assetManager);
    
    setupInput();
    setupCamera();
    setupLighting();
  }

  private void setupCamera() {
    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);
    getCamera().setLocation(getGlobalPosition(new Vector3i().add(32, 62, 62)));
  }
  
  Material mat;

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
    Camera camera = getCamera();
    Vector3f camLoc = camera.getLocation();
    Vector3f camDir = camera.getDirection().mult(80);
    Vector3f camLeft = camera.getLeft().mult(30);
    Vector3f camUp = camera.getUp().mult(30);
    loadWorldMeshForLocation(camLoc);
    loadWorldMeshForLocation(camLoc.add(camDir));
    loadWorldMeshForLocation(camLoc.add(camDir).add(camLeft));
    loadWorldMeshForLocation(camLoc.add(camDir).subtract(camLeft));
    loadWorldMeshForLocation(camLoc.add(camDir).add(camUp));
    loadWorldMeshForLocation(camLoc.add(camDir).subtract(camUp));
  }

  private void loadWorldMeshForLocation(Vector3f location) {
    Vector3i worldPosition = getWorldPosition(location);
    final Vector3i chunkIndex = world.getChunkIndex(worldPosition);
    if (meshed.contains(chunkIndex)) {
      return;
    }
    meshed.add(chunkIndex);
    // need 3x3 chunks around meshing area so that mesh borders can be handled correctly
    chunkProvider.requestChunks(chunkIndex.getSurroundingPositions(), new ChunkProcessor() {
      @Override
      public void chunksLoaded(List<Chunk> chunks) {
        for (Chunk c : chunks) {
          world.setChunk(c.getPosition(), c);
        }
        setupChunkAsMesh(chunkIndex);
      }
    });
  }

  private void setupChunkAsMesh(Vector3i chunkIndex) {
    LodSpatial spatials = builder.makeSpatials(world, chunkIndex);
    final Spatial groundObject = useSimpleMesh ? spatials.lowDetail : spatials.highDetail;
    
    enqueue(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        rootNode.attachChild(groundObject);
        return null;
      }
    });
  }
  
  public Vector3f getGlobalPosition(Vector3i worldPosition) {
    return new Vector3f(worldPosition.x * scale, worldPosition.y * scale, worldPosition.z * scale);
  }
  
  private Vector3i getWorldPosition(Vector3f location) {
    return new Vector3i(
        (int) Math.floor(location.x / scale),
        (int) Math.floor(location.y / scale),
        (int) Math.floor(location.z / scale));
  }
  

  private void setupLighting() {
    createSky();
    
    light = new DirectionalLight();
    Vector3f lightDir = new Vector3f(-0.9140114f, 0.29160172f, -0.2820493f).negate();
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
    
    FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
//    FogFilter fog = new FogFilter();
//    fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 0.0f));
//    fog.setFogDistance(200);
//    fog.setFogDensity(1.5f);
//    fpp.addFilter(fog);
  
    LightScatteringFilter filter = new LightScatteringFilter(light.getDirection().mult(-20000f));
    filter.setLightDensity(1.2f);
    filter.setBlurWidth(1.5f);
    fpp.addFilter(filter);

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

    viewPort.addProcessor(fpp);
  }

  private void createSky() {
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
  }
  
  @Override
  public void destroy() {
    super.destroy();
    if (closeEventHandler != null) {
      this.closeEventHandler.onClose();
    }
  }

  public void setCloseEventHandler(CloseEventHandler closeEventHandler) {
    this.closeEventHandler = closeEventHandler;
  }
}
