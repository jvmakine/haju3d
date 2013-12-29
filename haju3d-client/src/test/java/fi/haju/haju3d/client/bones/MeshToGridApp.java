package fi.haju.haju3d.client.bones;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bounding.BoundingBox;
import com.jme3.collision.CollisionResults;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import fi.haju.haju3d.client.SimpleApplicationUtils;

public class MeshToGridApp extends SimpleApplication {

  private Spatial model;

  public static void main(String[] args) {
    MeshToGridApp app = new MeshToGridApp();
    SimpleApplicationUtils.configureSimpleApplication(app);
    app.start();
  }

  @Override
  public void simpleInitApp() {
    viewPort.setBackgroundColor(ColorRGBA.DarkGray);
    assetManager.registerLocator("C:\\Users\\Hannu\\Downloads", FileLocator.class);

    model = assetManager.loadModel("model.blend");
    model.setLocalScale(0.2f);
    rootNode.attachChild(model);

    // sunset light
    DirectionalLight dl = new DirectionalLight();
    dl.setDirection(new Vector3f(-0.1f, -0.7f, 1).normalizeLocal());
    dl.setColor(new ColorRGBA(0.44f, 0.30f, 0.20f, 1.0f));
    rootNode.addLight(dl);

    // skylight
    dl = new DirectionalLight();
    dl.setDirection(new Vector3f(-0.6f, -1, -0.6f).normalizeLocal());
    dl.setColor(new ColorRGBA(0.10f, 0.22f, 0.44f, 1.0f));
    rootNode.addLight(dl);

    // white ambient light
    dl = new DirectionalLight();
    dl.setDirection(new Vector3f(1, -0.5f, -0.1f).normalizeLocal());
    dl.setColor(new ColorRGBA(0.80f, 0.70f, 0.80f, 1.0f));
    rootNode.addLight(dl);

    intersectModel(model);
  }

  @Override
  public void simpleUpdate(float tpf) {
  }

  private void intersectModel(Spatial model) {

    BoundingBox bound = (BoundingBox) model.getWorldBound();

    Vector3f min = bound.getMin(null);
    System.out.println("min = " + min);
    System.out.println("max = " + bound.getMax(null));
    System.out.println("bound.getXExtent() = " + bound.getXExtent());
    System.out.println("bound.getYExtent() = " + bound.getYExtent());
    System.out.println("bound.getZExtent() = " + bound.getZExtent());

    int sz = 160;
    /*
    //shoot rays from x/z plane to y direction:
    for (int x = 0; x < sz; x++) {
      for (int z = 0; z < sz; z++) {

      }
    }
    */

    for (int y = sz; y >= 0; y--) {
      for (int x = 0; x < sz; x++) {
        float rx = min.x + x * bound.getXExtent() * 2 / sz;
        float ry = min.y + y * bound.getYExtent() * 2 / sz;
        Ray ray = new Ray(new Vector3f(rx, ry, 0), Vector3f.UNIT_Z.negate());
        CollisionResults collision = new CollisionResults();
        int collisions = model.collideWith(ray, collision);
        if (collisions != 0) {
          System.out.print("#");
        } else {
          System.out.print(".");
        }
      }
      System.out.println();
    }

    System.out.println("get collisions");
    Vector3f camPos = cam.getLocation();
    Ray ray = new Ray(camPos, cam.getDirection());
    CollisionResults collision = new CollisionResults();
    int collisions = model.collideWith(ray, collision);
    if (collisions != 0) {
      for (int i = 0; i < collisions; i++) {
        System.out.println("collision: " + collision.getCollision(i));
      }
    }

  }


}
