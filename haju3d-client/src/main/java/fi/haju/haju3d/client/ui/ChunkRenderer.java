package fi.haju.haju3d.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
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
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.TextureArray;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;

import fi.haju.haju3d.client.ChunkProcessor;
import fi.haju.haju3d.client.ChunkProvider;
import fi.haju.haju3d.client.CloseEventHandler;
import fi.haju.haju3d.client.ui.input.InputActions;
import fi.haju.haju3d.client.ui.mesh.ChunkMeshBuilder;
import fi.haju.haju3d.client.ui.mesh.MyTexture;
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
  private TextureArray textures;
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
    builder = new ChunkMeshBuilder();
    
    setupInput();
    setupCamera();
    setupTextures();
    setupLighting();
  }

  private void setupCamera() {
    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);
    getCamera().setLocation(getGlobalPosition(new Vector3i().add(32, 62, 62)));
  }

  private void setupTextures() {
    Map<MyTexture, String> textureToFilename = new HashMap<>();
    if (useSimpleMesh) {
      textureToFilename.put(MyTexture.DIRT, "mc-dirt.png");
      textureToFilename.put(MyTexture.GRASS, "mc-grass.png");
      textureToFilename.put(MyTexture.ROCK, "mc-rock.png");
      textureToFilename.put(MyTexture.BRICK, "mc-brick.png");
    } else {
      textureToFilename.put(MyTexture.DIRT, "new-dirt.png");
      textureToFilename.put(MyTexture.GRASS, "new-grass.png");
      textureToFilename.put(MyTexture.ROCK, "new-rock.png");
      textureToFilename.put(MyTexture.BRICK, "new-brick.png");
    }
    
    List<Image> images = new ArrayList<Image>();
    for (MyTexture tex : MyTexture.values()) {
      String textureResource = "fi/haju/haju3d/client/textures/" + textureToFilename.get(tex);
      TextureKey key = new TextureKey(textureResource);
      key.setGenerateMips(true);
      images.add(assetManager.loadTexture(key).getImage());
    }
    textures = new TextureArray(images);
    textures.setWrap(WrapMode.Repeat);
    textures.setMinFilter(MinFilter.BilinearNearestMipMap);
    textures.setAnisotropicFilter(4);
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
    Camera camera = getCamera();
    Vector3f camLoc = camera.getLocation();
    Vector3f camDir = camera.getDirection().mult(50);
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
    Mesh m = builder.makeMesh(world, chunkIndex, useSimpleMesh);
    
    final Geometry groundObject = new Geometry("ColoredMesh", m);
    ColorRGBA color = ColorRGBA.White;
    Material mat = new Material(assetManager,
        useSimpleMesh ? "fi/haju/haju3d/client/shaders/Lighting.j3md" :
          "fi/haju/haju3d/client/shaders/Terrain.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setTexture("DiffuseMap", textures);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    if (useSimpleMesh) {
      mat.setBoolean("UseVertexColor", true);
    }
    groundObject.setMaterial(mat);
    groundObject.setShadowMode(ShadowMode.CastAndReceive);
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
    light.setColor(new ColorRGBA(1f, 1f, 1f, 1f).mult(1.5f));
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
