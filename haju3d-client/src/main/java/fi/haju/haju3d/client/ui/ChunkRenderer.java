package fi.haju.haju3d.client.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.water.WaterFilter;

import fi.haju.haju3d.client.Character;
import fi.haju.haju3d.client.ClientSettings;
import fi.haju.haju3d.client.CloseEventHandler;
import fi.haju.haju3d.client.ui.input.CharacterInputHandler;
import fi.haju.haju3d.client.ui.input.InputActions;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.TilePosition;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Renderer application for rendering chunks from the server
 */
@Singleton
public class ChunkRenderer extends SimpleApplication {

  private static final float GRAVITY_CONSTANT = 0.5f;
  private static final int WALK_SPEED = 10;
  private static final float WALL_CLIMB_ACCURACY = 0.01f;
  private static final int WALL_CLIMB_LOOPS = 40;
  private static final float SELECTOR_DISTANCE = 10.0f;
  private static final float MOVE_SPEED = 40;
  private static final int CHUNK_CUT_OFF = 3;

  private static final Vector3f LIGHT_DIR = new Vector3f(-0.9140114f, 0.29160172f, -0.2820493f).negate();

  @Inject
  private ChunkSpatialBuilder builder;
  @Inject
  private WorldManager worldManager;
  @Inject
  private CharacterInputHandler inputHandler;

  private ClientSettings clientSettings;

  private DirectionalLight light;
  private CloseEventHandler closeEventHandler;
  private boolean isFullScreen = false;
  private Node terrainNode = new Node("terrain");
  private Character character;
  private Node characterNode;
  private Leg characterLegLeft;
  private Leg characterLegRight;
  private TilePosition selectedTile;
  private TilePosition selectedBuildTile;
  private Node selectedVoxelNode;
  private BitmapText crossHair;
  private BitmapText selectedMaterialGui;
  private ViewMode viewMode = ViewMode.FLYCAM;
  private Tile selectedBuildMaterial = Tile.BRICK;

  private static final class Leg {
    private Spatial characterFoot;
    private Spatial characterLegTop;
    private Spatial characterLegBot;
  }

  @Inject
  public ChunkRenderer(ClientSettings clientSettings) {
    clientSettings.init();
    this.clientSettings = clientSettings;
    setDisplayMode();
    setShowSettings(false);
  }

  private void setDisplayMode() {
    AppSettings settings = new AppSettings(true);
    settings.setVSync(true);
    settings.setAudioRenderer(null);
    settings.setFullscreen(isFullScreen);
    settings.setResolution(clientSettings.getScreenWidth(), clientSettings.getScreenHeight());
    setSettings(settings);
  }

  @Override
  public void simpleInitApp() {
    setDisplayMode();
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    assetManager.registerLocator("assets", new ClasspathLocator().getClass());

    this.builder.init();
    this.worldManager.start();

    setupInput();
    setupCamera();
    setupSky();
    setupLighting();
    setupCharacter();
    setupPostFilters();
    setupSelector();

    this.inputHandler.register(inputManager);

    rootNode.attachChild(terrainNode);

    // Really hacky way of setting THIRD_PERSON mode at start-up.
    // Without this "sleep+set", the mouse focus will be lost for some reason (issue in jMonkeyEngine?).
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(10L);
        } catch (InterruptedException e) {
        }
        enqueue(new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            setViewMode(ViewMode.THIRD_PERSON);
            return null;
          }
        });
      }
    }).start();
  }

  private void setupCharacter() {
    character = new Character();
    character.setPosition(worldManager.getGlobalPosition(new Vector3i().add(20, 32, 25)));

    Geometry characterBody = makeSimpleMesh(
        new Box(0.3f, 0.4f, 0.2f),
        new ColorRGBA(1.0f, 0.5f, 0.3f, 1.0f));
    characterBody.setLocalTranslation(0, 0.5f, 0);

    Geometry characterHead = makeSimpleMesh(
        new Sphere(6, 6, 0.3f),
        new ColorRGBA(0.4f, 0.7f, 0.3f, 1.0f));
    characterHead.setLocalTranslation(0, 1, 0);

    characterNode = new Node("character");
    characterNode.attachChild(characterBody);
    characterNode.attachChild(characterHead);

    characterLegLeft = makeLeg();
    characterLegRight = makeLeg();
    attachLeg(characterLegLeft);
    attachLeg(characterLegRight);

    showCharacter();
  }

  private void attachLeg(Leg leg) {
    rootNode.attachChild(leg.characterFoot);
    rootNode.attachChild(leg.characterLegBot);
    rootNode.attachChild(leg.characterLegTop);
  }

  private void detachLeg(Leg leg) {
    rootNode.detachChild(leg.characterFoot);
    rootNode.detachChild(leg.characterLegBot);
    rootNode.detachChild(leg.characterLegTop);
  }

  private Leg makeLeg() {
    Leg leg = new Leg();
    leg.characterFoot = makeSimpleMesh(new Sphere(6, 6, 0.2f), new ColorRGBA(0.4f, 0.2f, 0.8f, 1.0f));
    leg.characterLegBot = makeSimpleMesh(new Box(0.1f, 0.1f, 0.5f), ColorRGBA.Orange);
    leg.characterLegTop = makeSimpleMesh(new Box(0.1f, 0.1f, 0.5f), ColorRGBA.Orange);
    return leg;
  }

  private Geometry makeSimpleMesh(Mesh mesh, ColorRGBA color) {
    Geometry characterModel = new Geometry("SimpleMesh", mesh);
    characterModel.setShadowMode(ShadowMode.CastAndReceive);
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    characterModel.setMaterial(mat);
    return characterModel;
  }

  private void setupCamera() {
    getFlyByCamera().setMoveSpeed(MOVE_SPEED);
    getFlyByCamera().setRotationSpeed(CharacterInputHandler.MOUSE_X_SPEED);
    getCamera().setLocation(worldManager.getGlobalPosition(new Vector3i().add(32, 62, 62)));
    getCamera().setFrustumPerspective(45f, (float) getCamera().getWidth() / getCamera().getHeight(), 0.1f, 200f);

    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
    crossHair = new BitmapText(guiFont, false);
    crossHair.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    crossHair.setText("+");
    crossHair.setLocalTranslation(settings.getWidth() / 2 - crossHair.getLineWidth() / 2,
        settings.getHeight() / 2 + crossHair.getLineHeight() / 2, 0);
  }

  private void showSelectedMaterial() {
    selectedMaterialGui = new BitmapText(guiFont, false);
    selectedMaterialGui.setSize(guiFont.getCharSet().getRenderedSize() * 1.5f);
    selectedMaterialGui.setText(selectedBuildMaterial.name());
    selectedMaterialGui.setLocalTranslation(20, settings.getHeight() - 20, 0);
    guiNode.attachChild(selectedMaterialGui);
  }

  private void hideSelectedMaterial() {
    if (selectedMaterialGui != null) {
      guiNode.detachChild(selectedMaterialGui);
    }
  }

  private void setupInput() {

  }

  private void updateWorldMesh() {
    worldManager.setPosition(getCurrentChunkIndex());
  }

  private ChunkPosition getCurrentChunkIndex() {
    return worldManager.getChunkIndexForLocation(getCamera().getLocation());
  }

  private void updateChunkSpatialVisibility() {
    ChunkPosition chunkIndex = getCurrentChunkIndex();
    terrainNode.detachAllChildren();
    for (ChunkPosition pos : chunkIndex.getSurroundingPositions(CHUNK_CUT_OFF, CHUNK_CUT_OFF, CHUNK_CUT_OFF)) {
      ChunkSpatial cs = worldManager.getChunkSpatial(pos);
      if (cs != null) {
        terrainNode.attachChild(pos.distanceTo(chunkIndex) <= 2 ? cs.highDetail : cs.lowDetail);
      }
    }
  }

  private void setupLighting() {
    light = new DirectionalLight();
    light.setDirection(LIGHT_DIR.normalizeLocal());
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

  private void setupSelector() {
    Box selectorMesh = new Box(WorldManager.SCALE / 2 * 1.05f, WorldManager.SCALE / 2 * 1.05f, WorldManager.SCALE / 2 * 1.05f);
    Geometry selectorModel = new Geometry("SelectorModel", selectorMesh);

    ColorRGBA color = ColorRGBA.Red;
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    mat.getAdditionalRenderState().setWireframe(true);
    selectorModel.setMaterial(mat);

    selectedVoxelNode = new Node();
    selectedVoxelNode.attachChild(selectorModel);
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

    WaterFilter water = new WaterFilter(rootNode, LIGHT_DIR);
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

    FogFilter fog = new FogFilter(new ColorRGBA(0.8f, 0.8f, 1.0f, 1.0f), 0.4f, 200.0f);
    fpp.addFilter(fog);
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
    //tpf has max value so that gc pauses etc won't cause sudden jumps
    tpf = Math.min(tpf, 1.0f / 30);

    if (viewMode == ViewMode.FLYCAM
        || worldManager.getChunkSpatial(getCurrentChunkIndex()) == null) {
      return;
    }

    // apply gravity
    character.setVelocity(character.getVelocity().add(new Vector3f(0.0f, -tpf * GRAVITY_CONSTANT, 0.0f)));
    Vector3f characterPos = character.getPosition();
    characterPos = characterPos.add(character.getVelocity());

    characterPos = updateCharacterOnFloorHit(characterPos);
    Vector3f oldPos = characterPos;
    characterPos = getCharacterPositionAfterUserInput(tpf, characterPos);
    characterPos = checkWallHit(characterPos, oldPos);
    character.setPosition(characterPos);

    updateCharacterFacingDirection();

    if (!inputHandler.getActiveInputs().isEmpty()) {
      if (Float.isNaN(character.getFeetCycle())) {
        character.setFeetCycle(0);
      } else {
        character.setFeetCycle(character.getFeetCycle() + tpf * 20f);
      }
    } else {
      character.setFeetCycle(Float.NaN);
    }

    // update character node locations
    float fc = character.getFeetCycle();

    if (!Float.isNaN(fc)) {
      characterNode.setLocalTranslation(character.getPosition().add(0, 0.1f * FastMath.sin(fc), 0));
    } else {
      characterNode.setLocalTranslation(character.getPosition());
    }
    characterNode.setLocalRotation(character.getFacingQuaternion());

    updateLegPosition(fc, characterLegLeft, 1);
    updateLegPosition(fc + HALF_CIRCLE, characterLegRight, -1);

    Vector3f camPos = getCameraPositionFromCharacter();
    cam.setLocation(camPos);
    updateSelectedTile(camPos);
  }

  private void updateLegPosition(float fc, Leg leg, float dir) {
    float feetLen = 0.4f;
    float feetDist = 0.25f;

    Vector3f charPos = character.getPosition().add(0, -0.6f, 0);
    Vector3f legPos = charPos.add(turnLeftLocal(character.getFaceVector()).multLocal(dir * feetDist));

    if (!Float.isNaN(fc)) {
      legPos.addLocal(character.getFaceVector().multLocal(FastMath.sin(fc)).multLocal(feetLen));
      legPos.addLocal(Vector3f.UNIT_Y.mult(FastMath.cos(fc)).multLocal(feetLen));
    }

    Vector3f legPosp = worldManager.getTerrainCollisionPoint(legPos, legPos.add(Vector3f.UNIT_Y.mult(0.6f)), 0);
    if (legPosp != null) {
      legPos = legPosp;
    }
    leg.characterFoot.setLocalTranslation(legPos);

    Vector3f ankle = legPos.subtract(character.getFaceVector().multLocal(0.2f));
    Vector3f hip = characterNode.getLocalTranslation().add(turnLeftLocal(character.getFaceVector()).multLocal(dir * feetDist)).add(0, 0.3f, 0);
    Vector3f knee = ankle.add(hip).mult(0.5f).add(character.getFaceVector().multLocal(0.2f));
    leg.characterLegBot.setLocalTransform(transformBetween(ankle, knee, character.getFaceVector()));
    leg.characterLegTop.setLocalTransform(transformBetween(knee, hip, character.getFaceVector()));
  }

  private Transform transformBetween(Vector3f start, Vector3f end, Vector3f front) {
    Vector3f dir = start.subtract(end);
    Vector3f left = dir.normalize().cross(front.normalize());
    Vector3f ahead = dir.normalize().cross(left.normalize());

    Quaternion q = new Quaternion();
    q.fromAxes(left.normalize(), ahead.normalize(), dir.normalize());
    return new Transform(start.add(end).multLocal(0.5f), q, new Vector3f(1, 1, dir.length()));
  }

  private void updateCharacterFacingDirection() {
    Set<String> activeInputs = inputHandler.getActiveInputs();
    if (activeInputs.contains(InputActions.WALK_FORWARD)) {
      updateCharacterFacingDirection(character.getLookAzimuth());
    }
    if (activeInputs.contains(InputActions.WALK_BACKWARD)) {
      updateCharacterFacingDirection((float) (character.getLookAzimuth() + Math.PI));
    }
    if (activeInputs.contains(InputActions.STRAFE_LEFT)) {
      updateCharacterFacingDirection((float) (character.getLookAzimuth() + Math.PI / 2));
    }
    if (activeInputs.contains(InputActions.STRAFE_RIGHT)) {
      updateCharacterFacingDirection((float) (character.getLookAzimuth() - Math.PI / 2));
    }

  }

  private static final float FULL_CIRCLE = (float) (Math.PI * 2);
  private static final float HALF_CIRCLE = (float) (Math.PI);

  private void updateCharacterFacingDirection(float desiredFacingAzimuth) {
    character.setFaceAzimuth(
        interpolateAngle(character.getFaceAzimuth(), desiredFacingAzimuth, 0.2f));
  }

  private static float interpolateAngle(float old, float target, float step) {
    float add = fixAngle(target - old);
    if (add >= 0 && add <= HALF_CIRCLE) {
      if (add < step) {
        old += add;
      } else {
        old += step;
      }
    } else {
      add = FULL_CIRCLE - add;
      if (add < step) {
        old -= add;
      } else {
        old -= step;
      }
    }
    return fixAngle(old);
  }

  private static float fixAngle(float angle) {
    while (angle >= FULL_CIRCLE) {
      angle -= FULL_CIRCLE;
    }
    while (angle < 0) {
      angle += FULL_CIRCLE;
    }
    return angle;
  }

  private void updateSelectedTile(Vector3f camPos) {
    selectedTile = worldManager.getVoxelCollisionPoint(camPos, camPos.add(cam.getDirection().normalize().mult(SELECTOR_DISTANCE)));
    selectedBuildTile = worldManager.getVoxelCollisionDirection(camPos, camPos.add(cam.getDirection().normalize().mult(SELECTOR_DISTANCE)));
    rootNode.detachChild(selectedVoxelNode);
    if (selectedTile != null) {
      selectedVoxelNode.setLocalTranslation(selectedTile.getWorldPosition(WorldManager.SCALE, worldManager.getChunkSize()));
      rootNode.attachChild(selectedVoxelNode);
    }
  }

  private Vector3f getCameraPositionFromCharacter() {
    // set camera position and rotation
    Quaternion quat = character.getLookQuaternion();
    cam.setRotation(quat);
    Vector3f camPos = character.getPosition().clone();
    Vector3f lookDir = quat.mult(Vector3f.UNIT_Z);
    if (viewMode == ViewMode.THIRD_PERSON) {
      camPos.addLocal(lookDir.mult(-10));
    }
    Vector3f coll = worldManager.getTerrainCollisionPoint(character.getPosition(), camPos, 0.0f);
    if (coll != null) {
      camPos.set(coll);
    }
    if (viewMode == ViewMode.FIRST_PERSON) {
      camPos = camPos.add(new Vector3f(0, 0.8f, 0));
    }
    return camPos;
  }

  /**
   * check if character falls below ground level, lift up to ground level
   */
  private Vector3f updateCharacterOnFloorHit(Vector3f oldPos) {
    Vector3f characterPos = oldPos.clone();
    while (true) {
      CollisionResults res = new CollisionResults();
      int collideWith = terrainNode.collideWith(makeCharacterBoundingVolume(characterPos), res);
      if (collideWith == 0) {
        break;
      }
      Vector3f oldVelocity = character.getVelocity();
      character.setVelocity(new Vector3f(oldVelocity.x, 0.0f, oldVelocity.z));
      characterPos = characterPos.add(0, 0.002f, 0);
    }
    return characterPos;
  }

  /**
   * move character based on used input
   */
  private Vector3f getCharacterPositionAfterUserInput(float tpf, Vector3f oldPos) {
    Vector3f characterPos = oldPos.clone();
    Set<String> activeInputs = inputHandler.getActiveInputs();
    float add = tpf * (float) WALK_SPEED;
    if (activeInputs.contains(InputActions.WALK_FORWARD)) {
      characterPos.addLocal(character.getLookVector().multLocal(add));
    }
    if (activeInputs.contains(InputActions.WALK_BACKWARD)) {
      characterPos.subtractLocal(character.getLookVector().multLocal(add));
    }
    if (activeInputs.contains(InputActions.STRAFE_LEFT)) {
      characterPos.addLocal(turnLeftLocal(character.getLookVector().multLocal(add)));
    }
    if (activeInputs.contains(InputActions.STRAFE_RIGHT)) {
      characterPos.addLocal(turnRightLocal(character.getLookVector().multLocal(add)));
    }
    return characterPos;
  }

  public static Vector3f turnRightLocal(Vector3f v) {
    float x = v.x;
    v.x = -v.z;
    v.z = x;
    return v;
  }

  public static Vector3f turnLeftLocal(Vector3f v) {
    float x = v.x;
    v.x = v.z;
    v.z = -x;
    return v;
  }

  /**
   * check if character hits wall. either climb it or return to old position
   */
  private Vector3f checkWallHit(Vector3f characterPos, Vector3f oldPos) {
    Vector3f newPos = characterPos.clone();
    int i = 0;
    for (i = 0; i < WALL_CLIMB_LOOPS; i++) {
      newPos = newPos.add(0, WALL_CLIMB_ACCURACY, 0);
      CollisionResults res = new CollisionResults();
      int collideWith = terrainNode.collideWith(makeCharacterBoundingVolume(newPos), res);
      if (collideWith == 0) {
        break;
      }
    }
    if (i == WALL_CLIMB_LOOPS) {
      return oldPos;
    } else {
      return newPos;
    }
  }

  private BoundingVolume makeCharacterBoundingVolume(Vector3f characterPos) {
    return new BoundingBox(characterPos.add(0, 0.2f, 0), 0.3f, 0.8f, 0.3f);
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

  public void toggleFullScreen() {
    isFullScreen = !isFullScreen;
    setDisplayMode();
    restart();
  }

  public void setViewMode(ViewMode mode) {
    if (mode == ViewMode.FLYCAM) {
      flyCam.setEnabled(true);
      inputManager.setCursorVisible(false);
      showCharacter();
      guiNode.detachChild(crossHair);
      hideSelectedMaterial();
    } else if (mode == ViewMode.FIRST_PERSON) {
      flyCam.setEnabled(false);
      inputManager.setCursorVisible(false);
      hideCharacter();
      guiNode.attachChild(crossHair);
      showSelectedMaterial();
    } else if (mode == ViewMode.THIRD_PERSON) {
      flyCam.setEnabled(false);
      inputManager.setCursorVisible(false);
      showCharacter();
      guiNode.detachChild(crossHair);
      hideSelectedMaterial();
    }
    viewMode = mode;
  }

  private void showCharacter() {
    rootNode.attachChild(characterNode);
    attachLeg(characterLegRight);
    attachLeg(characterLegLeft);
  }

  private void hideCharacter() {
    rootNode.detachChild(characterNode);
    detachLeg(characterLegRight);
    detachLeg(characterLegLeft);
  }

  public WorldManager getWorldManager() {
    return worldManager;
  }

  public TilePosition getSelectedTile() {
    return selectedTile;
  }

  public TilePosition getSelectedBuildTile() {
    return selectedBuildTile;
  }

  public Tile getSelectedBuildMaterial() {
    return selectedBuildMaterial;
  }

  public void setSelectedBuildMaterial(Tile selectedBuildMaterial) {
    this.selectedBuildMaterial = selectedBuildMaterial;
    hideSelectedMaterial();
    if (viewMode == ViewMode.FIRST_PERSON) {
      showSelectedMaterial();
    }
  }

  public Character getCharacter() {
    return character;
  }

}
