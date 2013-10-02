package fi.haju.haju3d.client.ui;

import java.util.HashSet;
import java.util.Set;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.CartoonEdgeFilter;
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

/**
 * Renderer application for rendering chunks from the server
 */
public class ChunkRenderer extends SimpleApplication {
  private static final float MOVE_SPEED = 40;
  private static final float MOUSE_X_SPEED = 3.0f;
  private static final float MOUSE_Y_SPEED = MOUSE_X_SPEED;

  
  private static final int CHUNK_CUT_OFF = 3;
  private static final Vector3f lightDir = new Vector3f(-0.9140114f, 0.29160172f, -0.2820493f).negate();

  private ChunkSpatialBuilder builder;
  private DirectionalLight light;
  private CloseEventHandler closeEventHandler;
  private ChunkProvider chunkProvider;


  private boolean isFullScreen = false;
  private Node terrainNode = new Node("terrain");
  private WorldManager worldManager;
  private Set<String> activeInputs = new HashSet<>();
  private Node characterNode;
  private Vector3f characterVelocity = new Vector3f();
  private float characterLookAzimuth = 0f;
  private float characterLookElevation = 0f;

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
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

    assetManager.registerLocator("assets", new ClasspathLocator().getClass());
    this.builder = new ChunkSpatialBuilder(assetManager);
    this.worldManager = new WorldManager(chunkProvider, builder);
    this.worldManager.start();

    setupInput();
    setupCamera();
    setupSky();
    setupLighting();
    setupCharacter();
    setupPostFilters();

    rootNode.attachChild(terrainNode);
  }

  private void setupCharacter() {
    characterNode = new Node("character");
    characterNode.setLocalTranslation(worldManager.getGlobalPosition(new Vector3i().add(32, 62, 32)));

    Box characterMesh = new Box(0.5f, 1.5f, 0.5f);
    Geometry characterModel = new Geometry("CharacterModel", characterMesh);

    ColorRGBA color = ColorRGBA.Red;
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    characterModel.setMaterial(mat);

    characterNode.attachChild(characterModel);

    rootNode.attachChild(characterNode);
  }

  private void setupCamera() {
    getFlyByCamera().setMoveSpeed(MOVE_SPEED);
    getFlyByCamera().setRotationSpeed(MOUSE_X_SPEED);
    getCamera().setLocation(worldManager.getGlobalPosition(new Vector3i().add(32, 62, 62)));
  }

  private void setupInput() {
    // moving
    inputManager.addMapping(InputActions.STRAFE_LEFT, new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping(InputActions.STRAFE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping(InputActions.WALK_FORWARD, new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping(InputActions.WALK_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
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

    // looking
    inputManager.addMapping(InputActions.LOOK_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true), new KeyTrigger(KeyInput.KEY_LEFT));
    inputManager.addMapping(InputActions.LOOK_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false), new KeyTrigger(KeyInput.KEY_RIGHT));
    inputManager.addMapping(InputActions.LOOK_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false), new KeyTrigger(KeyInput.KEY_UP));
    inputManager.addMapping(InputActions.LOOK_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true), new KeyTrigger(KeyInput.KEY_DOWN));

    inputManager.addListener(new AnalogListener() {
      @Override
      public void onAnalog(String name, float value, float tpf) {
        if (name.equals(InputActions.LOOK_LEFT)) {
          characterLookAzimuth += value * MOUSE_X_SPEED;
        } else if (name.equals(InputActions.LOOK_RIGHT)) {
          characterLookAzimuth -= value * MOUSE_X_SPEED;
        } else if (name.equals(InputActions.LOOK_UP)) {
          characterLookElevation -= value * MOUSE_Y_SPEED;
        } else if (name.equals(InputActions.LOOK_DOWN)) {
          characterLookElevation += value * MOUSE_Y_SPEED;
        }
      }
    }, InputActions.LOOK_LEFT, InputActions.LOOK_RIGHT, InputActions.LOOK_UP, InputActions.LOOK_DOWN);

    // jumping
    inputManager.addMapping(InputActions.JUMP, new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed && canJump(characterNode)) {
          characterVelocity.y += 0.5;
        }
      }
    }, InputActions.JUMP);

    // toggle flycam
    inputManager.addMapping(InputActions.TOGGLE_FLYCAM, new KeyTrigger(KeyInput.KEY_RETURN));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          flyCam.setEnabled(!flyCam.isEnabled());
          inputManager.setCursorVisible(false);
        }
      }
    }, InputActions.TOGGLE_FLYCAM);

    // toggle fullscreen
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

  private boolean canJump(Node characterNode) {
    Vector3f pos = characterNode.getLocalTranslation();
    return worldManager.getTerrainCollisionPoint(pos, pos.add(new Vector3f(0, -2.0f, 0)), 0.0f) != null;
  }
  
  private void updateWorldMesh() {
    worldManager.setPosition(getCurrentChunkIndex());
  }

  private Vector3i getCurrentChunkIndex() {
    return worldManager.getChunkIndexForLocation(getCamera().getLocation());
  }

  private void updateChunkSpatialVisibility() {
    Vector3i chunkIndex = getCurrentChunkIndex();
    terrainNode.detachAllChildren();
    for (Vector3i pos : chunkIndex.getSurroundingPositions(CHUNK_CUT_OFF, CHUNK_CUT_OFF, CHUNK_CUT_OFF)) {
      ChunkSpatial cs = worldManager.getChunkSpatial(pos);
      if (cs != null) {
        terrainNode.attachChild(pos.equals(chunkIndex) ? cs.highDetail : cs.lowDetail);
      }
    }
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

    CartoonEdgeFilter rimLightFilter = new CartoonEdgeFilter();
    rimLightFilter.setEdgeColor(ColorRGBA.Black);

    rimLightFilter.setEdgeIntensity(0.5f);
    rimLightFilter.setEdgeWidth(1.0f);

    rimLightFilter.setNormalSensitivity(0.0f);
    rimLightFilter.setNormalThreshold(0.0f);

    rimLightFilter.setDepthSensitivity(20.0f);
    rimLightFilter.setDepthThreshold(0.0f);

    fpp.addFilter(rimLightFilter);

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
    updateCharacter(tpf);
  }

  private void updateCharacter(float tpf) {
    if (flyCam.isEnabled()) {
      return;
    }
    if (worldManager.getChunkSpatial(getCurrentChunkIndex()) == null) {
      return;
    }

    // apply gravity
    characterVelocity.y -= tpf * 0.5;
    Vector3f characterPos = characterNode.getLocalTranslation();
    characterPos = characterPos.add(characterVelocity);

    // check if character falls below ground level, lift up to ground level
    while (true) {
      CollisionResults res = new CollisionResults();
      int collideWith = terrainNode.collideWith(makeCharacterBoundingVolume(characterPos), res);
      if (collideWith == 0) {
        break;
      }
      characterVelocity.y = 0;
      characterPos = characterPos.add(0, 0.01f, 0);
    }

    // move character based on used input
    Vector3f oldPos = characterPos.clone();
    final float walkSpeed = 10;
    if (activeInputs.contains(InputActions.WALK_FORWARD)) {
      characterPos.z += FastMath.cos(characterLookAzimuth) * tpf * walkSpeed;
      characterPos.x += FastMath.sin(characterLookAzimuth) * tpf * walkSpeed;
    }
    if (activeInputs.contains(InputActions.WALK_BACKWARD)) {
      characterPos.z -= FastMath.cos(characterLookAzimuth) * tpf * walkSpeed;
      characterPos.x -= FastMath.sin(characterLookAzimuth) * tpf * walkSpeed;
    }
    if (activeInputs.contains(InputActions.STRAFE_LEFT)) {
      characterPos.z -= FastMath.sin(characterLookAzimuth) * tpf * walkSpeed;
      characterPos.x -= -FastMath.cos(characterLookAzimuth) * tpf * walkSpeed;
    }
    if (activeInputs.contains(InputActions.STRAFE_RIGHT)) {
      characterPos.z += FastMath.sin(characterLookAzimuth) * tpf * walkSpeed;
      characterPos.x += -FastMath.cos(characterLookAzimuth) * tpf * walkSpeed;
    }

    // check if character hits wall. either climb it or return to old position
    Vector3f newPos = characterPos;
    int i = 0;
    final int loops = 40;
    for (i = 0; i < loops; i++) {
      newPos = newPos.add(0, 0.01f, 0);
      CollisionResults res = new CollisionResults();
      int collideWith = terrainNode.collideWith(makeCharacterBoundingVolume(newPos), res);
      if (collideWith == 0) {
        break;
      }
    }
    if (i == loops) {
      characterPos = oldPos;
    } else {
      characterPos = newPos;
    }

    characterNode.setLocalTranslation(characterPos);

    // set camera position and rotation
    Quaternion quat = new Quaternion();
    quat.fromAngles(characterLookElevation, characterLookAzimuth, 0.0f);
    cam.setRotation(quat);

    Vector3f camPos = characterNode.getLocalTranslation().clone();
    Vector3f lookDir = quat.mult(Vector3f.UNIT_Z);
    camPos.addLocal(lookDir.mult(-10));

    Vector3f coll = worldManager.getTerrainCollisionPoint(characterNode.getLocalTranslation(), camPos, 0.0f);
    if (coll != null) {
      camPos.set(coll);
    }

    cam.setLocation(camPos);
  }

  private BoundingVolume makeCharacterBoundingVolume(Vector3f characterPos) {
    return new BoundingSphere(1, characterPos.add(0, -0.5f, 0));
  }

  @Override
  public void destroy() {
    super.destroy();
    if (this.worldManager != null) {
      this.worldManager.stop();
    }
    if (closeEventHandler != null) {
      this.closeEventHandler.onClose();
    }
  }

  public void setCloseEventHandler(CloseEventHandler closeEventHandler) {
    this.closeEventHandler = closeEventHandler;
  }
}
