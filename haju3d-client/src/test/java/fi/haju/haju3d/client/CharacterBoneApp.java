package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class CharacterBoneApp extends SimpleApplication {
  public static void main(String[] args) {
    CharacterBoneApp app = new CharacterBoneApp();
    SimpleApplicationUtils.configureSimpleApplication(app);
    app.start();
  }

  @Override
  public void simpleInitApp() {
    flyCam.setEnabled(false);
    SimpleApplicationUtils.addLights(this);
    SimpleApplicationUtils.addCartoonEdges(this);
    rootNode.attachChild(makeBox());
    rootNode.attachChild(makeFloor());
  }

  public Spatial makeBox() {
    Mesh m = new Box(1, 2, 3);
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

  public Spatial makeFloor() {
    Mesh m = new Box(100, 1, 100);
    final Geometry geom = new Geometry("Floor", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.Blue));
    geom.setShadowMode(RenderQueue.ShadowMode.Receive);
    geom.setLocalTranslation(0, -2, 0);
    return geom;
  }

}
