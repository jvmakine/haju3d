package fi.haju.haju3d.client.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.collision.CollisionResults;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.TextureArray;

import fi.haju.haju3d.client.ChunkProcessor;
import fi.haju.haju3d.client.ChunkProvider;
import fi.haju.haju3d.client.CloseEventHandler;
import fi.haju.haju3d.client.ui.mesh.ChunkMeshBuilder;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.World;

/**
 * Renderer application for rendering chunks from the server
 */
public class ChunkRenderer extends SimpleApplication {
  private static final float scale = 1;
  private ChunkMeshBuilder builder;
//  private Spatial groundObject;
  private Spatial characterObject;
  private Vector3f lastLocation = null;
  private DirectionalLight light;
  private boolean useVertexColor;
  private CloseEventHandler closeEventHandler;
  private ChunkProvider chunkProvider;
  
  private World world = new World();
  private TextureArray textures;
  private Set<Vector3i> meshed = new HashSet<>();

  public ChunkRenderer(ChunkProvider chunkProvider) {
    this.chunkProvider = chunkProvider;
    
    setUseVertexColor(false);
    AppSettings settings = new AppSettings(true);
    settings.setVSync(true);
    settings.setAudioRenderer(null);
    settings.setFullscreen(false);
    setSettings(settings);
    setShowSettings(false);
  }
  
  public void setUseVertexColor(boolean useVertexColor) {
    this.useVertexColor = useVertexColor;
  }

  @Override
  public void simpleInitApp() {
    assetManager.registerLocator("assets", new ClasspathLocator().getClass());
    
    builder = new ChunkMeshBuilder();
    builder.setUseVertexColor(useVertexColor);
    
    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);
    getCamera().setLocation(getGlobalPosition(new Vector3i().add(32, 62, 62)));

    Texture tex1 = assetManager.loadTexture("fi/haju/haju3d/client/textures/grass.png");
    Texture tex2 = assetManager.loadTexture("fi/haju/haju3d/client/textures/rock.png");
    List<Image> images = new ArrayList<Image>();
    images.add(tex1.getImage());
    images.add(tex2.getImage());
    textures = new TextureArray(images);
    textures.setWrap(WrapMode.Repeat);
    
    setupLighting();
    setupCharacter();
//    setupToruses();
  }

  private void updateWorldMesh() {
    Vector3i worldPosition = getWorldPosition(getCamera().getLocation());
    final Vector3i chunkIndex = world.getChunkIndex(worldPosition);
    if (meshed.contains(chunkIndex)) {
      return;
    }
    meshed.add(chunkIndex);
    List<Vector3i> positions = new ArrayList<>();
    positions.add(chunkIndex);
    positions.add(chunkIndex.add(1, 0, 0));
    positions.add(chunkIndex.add(-1, 0, 0));
    positions.add(chunkIndex.add(0, 0, 1));
    positions.add(chunkIndex.add(0, 0, -1));
    positions.add(chunkIndex.add(0, 1, 0));
    positions.add(chunkIndex.add(0, -1, 0));
    chunkProvider.requestChunks(positions, new ChunkProcessor() {
      @Override
      public void chunksLoaded(List<Chunk> chunks) {
        for (Chunk c : chunks) {
          world.setChunk(c.getPosition(), c);
        }
        setupChunkAsMesh(chunkIndex);
      }
    });
  }


/* private void setupToruses() {
    Torus torus = new Torus(20, 20, 0.5f, 1.0f);

    BatchNode batch = new BatchNode("batch");
    batch.setShadowMode(ShadowMode.CastAndReceive);

    Random rnd = new Random(chunk.getSeed());
    Material red = makeColorMaterial(ColorRGBA.Red);
    Material blue = makeColorMaterial(ColorRGBA.Blue);
    for (int i = 0; i < 50; i++) {
      CollisionResults res = new CollisionResults();
      float x = (float) ((rnd.nextDouble() * chunk.getWidth()) - chunk.getHeight() * 0.5);
      float z = (float) (-(rnd.nextDouble() * chunk.getDepth()));
      Ray r = new Ray(new Vector3f(x, chunk.getHeight(), z), Vector3f.UNIT_Y.negate());
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
  }*/

  private void setupCharacter() {
    Box characterMesh = new Box(1, 0.5f, 1);
    characterObject = new Geometry("Character", characterMesh);
    characterObject.setMaterial(makeColorMaterial(ColorRGBA.Red));
    characterObject.setShadowMode(ShadowMode.CastAndReceive);
    rootNode.attachChild(characterObject);
  }

  private void setupChunkAsMesh(Vector3i chunkIndex) {
    Mesh m = builder.makeMesh(world, chunkIndex);
    
    Geometry groundObject = new Geometry("ColoredMesh", m);
    ColorRGBA color = ColorRGBA.White;
    Material mat = new Material(assetManager, "fi/haju/haju3d/client/shaders/Lighting.j3md");
//    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setTexture("DiffuseMap", textures);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    if (useVertexColor) {
      mat.setBoolean("UseVertexColor", true);
    }
    groundObject.setMaterial(mat);
    groundObject.setShadowMode(ShadowMode.CastAndReceive);
    rootNode.attachChild(groundObject);
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
  

  private Material makeColorMaterial(ColorRGBA color) {
    Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Ambient", color);
    mat.setColor("Diffuse", color);
    return mat;
  }

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

  @Override
  public void simpleUpdate(float tpf) {
    // collision with ground, currently disabled
    /*
    CollisionResults res = new CollisionResults();
    if(lastLocation != null) {
      Ray r = new Ray(cam.getLocation(), Vector3f.UNIT_Y);
      int collideWith = groundObject.collideWith(r, res);
      if (collideWith != 0) {
        getCamera().setLocation(lastLocation);
      }
    }
      */
    lastLocation = getCamera().getLocation().clone();
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
