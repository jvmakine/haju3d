package fi.haju.haju3d.client.ui;

import java.util.HashSet;
import java.util.Set;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bounding.BoundingSphere;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
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
  private static final float scale = 1;
  private ChunkSpatialBuilder builder;
  private DirectionalLight light;
  private CloseEventHandler closeEventHandler;
  private ChunkProvider chunkProvider;
  
  enum ChunkSpatialType { LOW_QUALITY, HIGH_QUALITY }
  
  private World world = new World();
  private boolean isFullScreen = false;
  private Node terrainNode = new Node("terrain");
  private WorldBuilder worldBuilder;
  private Set<String> activeInputs = new HashSet<>();
  private CameraNode camNode;
  private boolean lockView = false;
  private Node characterNode;
  private Vector3f characterVelocity = new Vector3f();
  
  public ChunkRenderer(ChunkProvider chunkProvider) {
    this.chunkProvider = chunkProvider;
    setDisplayMode();
  }

  private void setDisplayMode() {
    AppSettings settings = new AppSettings(true);
//    settings.setResolution(1280, 720);
//    settings.setResolution(1920, 1080);
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
    this.worldBuilder.start();
    
    setupInput();
    setupCamera();
    setupLighting();
    setupCharacter();
    
    rootNode.attachChild(terrainNode);
  }

  private void setupCharacter() {
    characterNode = new Node("character");
    characterNode.setLocalTranslation(getGlobalPosition(new Vector3i().add(32, 62, 62)));
    
    Box characterMesh = new Box(0.5f, 1.5f, 0.5f);
    Geometry characterModel = new Geometry ("CharacterModel", characterMesh);
    
    ColorRGBA color = ColorRGBA.Red;
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors",true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    characterModel.setMaterial(mat);
    
    characterNode.attachChild(characterModel);
    
    camNode = new CameraNode("CamNode", cam);
    camNode.setLocalTranslation(new Vector3f(0, 3, -10));
    Quaternion quat = new Quaternion();
    quat.lookAt(Vector3f.UNIT_Z.subtract(0, 0.2f, 0), Vector3f.UNIT_Y);
    camNode.setLocalRotation(quat);
    characterNode.attachChild(camNode);
    camNode.setEnabled(false);
    
    rootNode.attachChild(characterNode);
  }

  private void setupCamera() {
    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);
    getCamera().setLocation(getGlobalPosition(new Vector3i().add(32, 62, 62)));
  }
  
  Material mat;

  private void setupInput() {
    inputManager.addMapping(InputActions.STRAFE_LEFT, new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping(InputActions.STRAFE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping(InputActions.WALK_FORWARD, new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping(InputActions.WALK_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping(InputActions.JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addMapping(InputActions.LOCK_VIEW, new KeyTrigger(KeyInput.KEY_RETURN));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          activeInputs.add(name);
        } else {
          activeInputs.remove(name);
        }
      }
    }, InputActions.STRAFE_LEFT, InputActions.STRAFE_RIGHT, InputActions.WALK_FORWARD, InputActions.WALK_BACKWARD);
    
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          characterVelocity.y += 0.5;
        }
      }
    }, InputActions.JUMP);
    
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          lockView = !lockView;
          flyCam.setEnabled(!lockView);
          camNode.setEnabled(lockView);
        }
      }
    }, InputActions.LOCK_VIEW);
    
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
    worldBuilder.setPosition(getCurrentChunkIndex());
  }

  private Vector3i getCurrentChunkIndex() {
    return world.getChunkIndex(getWorldPosition(getCamera().getLocation()));
  }
  
  private void updateChunkSpatialVisibility() {
    Vector3i chunkIndex = getCurrentChunkIndex();
    
    terrainNode.detachAllChildren();
    
    for (Vector3i pos : chunkIndex.getSurroundingPositions(2, 2, 2)) {
      ChunkSpatial cs = worldBuilder.getChunkSpatial(pos);
      if (cs != null) {
        terrainNode.attachChild(pos.equals(chunkIndex) ? cs.highDetail : cs.lowDetail);
      }
    }
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
    
    // FogFilter will also apply fog on the skybox..
//    FogFilter fog = new FogFilter();
//    fog.setFogColor(new ColorRGBA(0.9f, 0.9f, 0.9f, 0.0f));
//    fog.setFogDistance(200);
//    fog.setFogDensity(1.5f);
//    fpp.addFilter(fog);
  
    // disabled LightScatteringFilter for now, it looks very strange if something is blocking the sun
//    LightScatteringFilter filter = new LightScatteringFilter(light.getDirection().mult(-20000f));
//    filter.setLightDensity(1.2f);
//    filter.setBlurWidth(1.5f);
//    fpp.addFilter(filter);
    
    
    CartoonEdgeFilter rimLightFilter = new CartoonEdgeFilter();
    rimLightFilter.setEdgeColor(ColorRGBA.Black);
    
    rimLightFilter.setEdgeIntensity(0.5f);
    rimLightFilter.setEdgeWidth(1.0f);
    
    rimLightFilter.setNormalSensitivity(0.0f);
    rimLightFilter.setNormalThreshold(0.0f);
    
    rimLightFilter.setDepthSensitivity(20.0f);
    rimLightFilter.setDepthThreshold(0.0f);
    
    fpp.addFilter(rimLightFilter);
    
    BloomFilter bloom=new BloomFilter();
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
    updateChunkSpatialVisibility();
    updateCharacter(tpf);
  }
  
  private void updateCharacter(float tpf) {
    if (worldBuilder.getChunkSpatial(getCurrentChunkIndex()) == null) {
      return;
    }
    
    characterVelocity.y -= tpf * 0.5;
    Vector3f characterPos = characterNode.getLocalTranslation();
    characterPos = characterPos.add(characterVelocity);
    
    {
      BoundingSphere bs = new BoundingSphere(1, characterPos);
      CollisionResults res = new CollisionResults();
      int collideWith = terrainNode.collideWith(bs, res);
      while (collideWith != 0) {
        characterVelocity.y = 0;
        characterPos = characterPos.add(0, 0.01f, 0);
        res = new CollisionResults();
        bs = new BoundingSphere(1, characterPos);
        collideWith = terrainNode.collideWith(bs, res);
      }
    }
    
    Vector3f oldPos = characterPos.clone();
    final float walkSpeed = 10;
    if (activeInputs.contains(InputActions.WALK_FORWARD)) {
      characterPos.z += tpf * walkSpeed;
    }
    if (activeInputs.contains(InputActions.WALK_BACKWARD)) {
      characterPos.z -= tpf * walkSpeed;
    }
    if (activeInputs.contains(InputActions.STRAFE_LEFT)) {
      characterPos.x += tpf * walkSpeed;
    }
    if (activeInputs.contains(InputActions.STRAFE_RIGHT)) {
      characterPos.x -= tpf * walkSpeed;
    }
    
    {
      Vector3f newPos = characterPos;
      int i = 0;
      final int loops = 40;
      for (i = 0; i < loops; i++) {
        newPos = newPos.add(0, 0.01f, 0);
        CollisionResults res = new CollisionResults();
        BoundingSphere bs = new BoundingSphere(1, newPos);
        int collideWith = terrainNode.collideWith(bs, res);
        if (collideWith == 0) {
          characterPos = newPos;
          break;
        }
      }
      if (i == loops) {
        characterPos = oldPos;
      }
    }
    
    characterNode.setLocalTranslation(characterPos);
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
