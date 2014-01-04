package fi.haju.haju3d.client.bones;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import fi.haju.haju3d.client.SimpleApplicationUtils;
import fi.haju.haju3d.protocol.world.ByteArray3d;

public class MarchingCubesMesherTest extends SimpleApplication {
  public static void main(String[] args) {
    MarchingCubesMesherTest app = new MarchingCubesMesherTest();
    SimpleApplicationUtils.configureSimpleApplication(app);
    app.start();
//    benchmark();
  }

  private static void benchmark() {
    //time: 593 (max alloc, correct normals) ==> 458 by changing loop order! ==> 384 with reduced allocs
    //time: 508 (few alloc)
    //time: 409 (zero alloc)
    //time just to read the grid: 171
    //time just to read the grid, when x is innermost loop: 53
    //how to get time down: only update changed parts
    ByteArray3d grid = makeBoneMeshGrid(220);
    for (int i = 0; i < 20; i++) {
      long t0 = System.currentTimeMillis();
      MarchingCubesMesher.createMesh(grid);
      long t1 = System.currentTimeMillis();
      System.out.println("tt_grid = " + (t1 - t0));
    }
  }

  @Override
  public void simpleInitApp() {
    viewPort.setBackgroundColor(ColorRGBA.DarkGray);
    flyCam.setMoveSpeed(21);
    flyCam.setRotationSpeed(2.0f);
    SimpleApplicationUtils.addLights(this);
    SimpleApplicationUtils.addCartoonEdges(this);
    SimpleApplicationUtils.setupCrosshair(this, this.settings);

    if (false) {
      Sphere sphere = new Sphere(20, 20, 5);
      final Geometry geom = new Geometry("Sphere", sphere);
      geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
      geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
      geom.setLocalTranslation(0, 0, -20);
      rootNode.attachChild(geom);
    }

    {
      //ByteArray3d grid = makeBoneMeshGrid(32);
      ByteArray3d grid = BoneMeshUtils.makeBlobBoneMeshGrid(3);
      Mesh mesh = MarchingCubesMesher.createMesh(grid, 0.2f, new Vector3f(-grid.getWidth() / 2, -grid.getHeight() / 2, 0));
      final Geometry geom = new Geometry("ColoredMesh", mesh);

      Material mat = SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White);

      Texture tex = assetManager.loadTexture("fi/haju/haju3d/client/textures/sky9-top.jpg");
      tex.setWrap(Texture.WrapMode.Repeat);
      mat.setTexture("DiffuseMap", tex);

      geom.setMaterial(mat);
      geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
      geom.setLocalTranslation(0, 0, -20);
      rootNode.attachChild(geom);
    }
  }

  private static ByteArray3d makeBoneMeshGrid(int sz) {
    ByteArray3d grid = new ByteArray3d(sz, sz, sz);
    int w = grid.getWidth();
    int h = grid.getHeight();
    int d = grid.getDepth();

    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          int xd = x - sz / 2;
          int yd = y - sz / 2;
          int zd = z - sz / 2;
          int bsz = (int) (sz / 2.5);
          float value = bsz - FastMath.sqrt(xd * xd + yd * yd + zd * zd);

          value = (value * 4) + 64;
          if (value < 0) value = 0;
          if (value > 127) value = 127;

          grid.set(x, y, z, (byte) value);
        }
      }
    }
    return grid;
  }
}
