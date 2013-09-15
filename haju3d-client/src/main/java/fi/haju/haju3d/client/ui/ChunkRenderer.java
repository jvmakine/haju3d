package fi.haju.haju3d.client.ui;

import java.util.Random;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.collision.CollisionResults;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Torus;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;

import fi.haju.haju3d.client.ui.mesh.ChunkMeshBuilder;
import fi.haju.haju3d.protocol.world.Chunk;

/**
 * Renderer application for rendering chunks from the server
 */
public class ChunkRenderer extends SimpleApplication {

  private Chunk chunk = null;
  private ChunkMeshBuilder builder;
  private Spatial groundObject;
  private Spatial characterObject;
  private Vector3f lastLocation = null;
  private DirectionalLight light;
  
  public ChunkRenderer(Chunk chunk) {
    this.chunk = chunk;
  }

  @Override
  public void simpleInitApp() {
    assetManager.registerLocator("assets", new ClasspathLocator().getClass());
    
    builder = new ChunkMeshBuilder();
    
    getFlyByCamera().setMoveSpeed(20 * 2);
    getFlyByCamera().setRotationSpeed(3);

    setupChunkAsMesh();
    setupLighting();
    setupCharacter();
    setupToruses();
  }

  private void setupToruses() {
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
  }

  private void setupCharacter() {
    Box characterMesh = new Box(1, 0.5f, 1);
    characterObject = new Geometry("Character", characterMesh);
    characterObject.setMaterial(makeColorMaterial(ColorRGBA.Red));
    characterObject.setShadowMode(ShadowMode.CastAndReceive);
    rootNode.attachChild(characterObject);
  }

  private void setupChunkAsMesh() {
    float scale = 1;
    
    Mesh m = builder.makeMesh(chunk);
    
    groundObject = new Geometry("ColoredMesh", m);
    Material mat = makeColorMaterial(ColorRGBA.White);
    mat.setBoolean("UseVertexColor", true);
    groundObject.setMaterial(mat);
    groundObject.setShadowMode(ShadowMode.CastAndReceive);

    groundObject.setLocalTranslation(-chunk.getWidth() * scale * 0.5f, -chunk.getHeight() * scale * 0.5f, -chunk.getDepth() * scale);
    rootNode.attachChild(groundObject);
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
    CollisionResults res = new CollisionResults();
    if(lastLocation != null) {
      Ray r = new Ray(cam.getLocation(), Vector3f.UNIT_Y);
      int collideWith = groundObject.collideWith(r, res);
      if (collideWith != 0) {
        getCamera().setLocation(lastLocation);
      } 
    }
    lastLocation = getCamera().getLocation().clone();
  }
}
