package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;

public class CharacterBoneApp extends SimpleApplication {
  private static class Actions {
    public static final String ROTATE_LEFT = "RotateLeft";
    public static final String ROTATE_RIGHT = "RotateRight";
    public static final String ROTATE_UP = "RotateUp";
    public static final String ROTATE_DOWN = "RotateDown";
  }

  public static void main(String[] args) {
    CharacterBoneApp app = new CharacterBoneApp();
    SimpleApplicationUtils.configureSimpleApplication(app);
    app.start();
  }

  private Spatial camTarget;
  private float camDistance = 10;
  private float camElevation = 0;
  private float camAzimuth = 0;

  @Override
  public void simpleInitApp() {
    flyCam.setEnabled(false);
    SimpleApplicationUtils.addLights(this);
    SimpleApplicationUtils.addCartoonEdges(this);
    Spatial box = makeBox();
    camTarget = box;
    rootNode.attachChild(box);
    rootNode.attachChild(makeFloor());

    inputManager.addMapping(Actions.ROTATE_LEFT, new KeyTrigger(KeyInput.KEY_LEFT));
    inputManager.addMapping(Actions.ROTATE_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
    inputManager.addMapping(Actions.ROTATE_UP, new KeyTrigger(KeyInput.KEY_UP));
    inputManager.addMapping(Actions.ROTATE_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));
    inputManager.addListener(new AnalogListener() {
      public static final float ROTATE_SPEED = 3;

      @Override
      public void onAnalog(String name, float value, float tpf) {
        if (name.equals(Actions.ROTATE_LEFT)) {
          camAzimuth -= value * ROTATE_SPEED;
        } else if (name.equals(Actions.ROTATE_RIGHT)) {
          camAzimuth += value * ROTATE_SPEED;
        } else if (name.equals(Actions.ROTATE_DOWN)) {
          camElevation -= value * ROTATE_SPEED;
          if (camElevation < -FastMath.PI / 2) {
            camElevation = -FastMath.PI / 2;
          }
        } else if (name.equals(Actions.ROTATE_UP)) {
          camElevation += value * ROTATE_SPEED;
          if (camElevation > FastMath.PI / 2) {
            camElevation = FastMath.PI / 2;
          }
        }
      }
    }, Actions.ROTATE_LEFT, Actions.ROTATE_RIGHT, Actions.ROTATE_UP, Actions.ROTATE_DOWN);
  }

  @Override
  public void simpleUpdate(float tpf) {
    setCameraPosition();
  }

  private void setCameraPosition() {
    Quaternion quat = getCameraQuaternion();
    cam.setRotation(quat);
    Vector3f camPos = camTarget.getWorldTranslation().clone();
    Vector3f lookDir = quat.mult(Vector3f.UNIT_Z);
    camPos.addLocal(lookDir.mult(-camDistance));
    cam.setLocation(camPos);
  }

  private Quaternion getCameraQuaternion() {
    Quaternion quat = new Quaternion();
    quat.fromAngles(camElevation, camAzimuth, 0.0f);
    return quat;
  }

  public Spatial makeBox() {
    Mesh m = new Box(1, 2, 3);
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

  public Spatial makeFloor() {
    Mesh m = new Quad(100, 100);
    final Geometry geom = new Geometry("Floor", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.Blue));
    geom.setShadowMode(RenderQueue.ShadowMode.Receive);
    Matrix3f rot = new Matrix3f();
    rot.fromAngleNormalAxis(3 * FastMath.PI / 2, Vector3f.UNIT_X);
    geom.setLocalRotation(rot);
    geom.setLocalTranslation(-50, -3, 50);
    return geom;
  }

}
