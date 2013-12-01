package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class CharacterBoneApp extends SimpleApplication {
  private static class Actions {
    public static final String ROTATE_LEFT = "RotateLeft";
    public static final String ROTATE_RIGHT = "RotateRight";
    public static final String ROTATE_UP = "RotateUp";
    public static final String ROTATE_DOWN = "RotateDown";
    public static final String CLICK = "Click";
  }

  private static class MyBone {
    private Vector3f start;
    private Vector3f end;
    private float scale = 1.0f;
    private float rotation;
    private Spatial spatial;
    private Spatial guiSpatial;
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
  private List<MyBone> bones = new ArrayList<>();

  private Vector3f dragTarget;
  private Spatial dragPlane;
  private Spatial dragPlanePreview;

  @Override
  public void simpleInitApp() {
    flyCam.setEnabled(false);
    SimpleApplicationUtils.addLights(this);
    SimpleApplicationUtils.addCartoonEdges(this);
    Spatial box = makeBox();
    camTarget = box;
    rootNode.attachChild(box);
    rootNode.attachChild(makeFloor());

    MyBone bone = new MyBone();
    bone.start = new Vector3f(0, 2, 0);
    bone.end = new Vector3f(1, -1, 2);
    bone.scale = 0.5f;
    bone.spatial = makeBox();
    bones.add(bone);

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

    inputManager.addMapping(Actions.CLICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          dragTarget = bones.get(0).start;
          dragPlane = makeDragPlane();
          dragPlanePreview = makeDragPlanePreview();
          rootNode.attachChild(dragPlanePreview);
        } else {
          if (dragPlanePreview != null) {
            rootNode.detachChild(dragPlanePreview);
          }
          dragPlane = null;
          dragPlanePreview = null;
          dragTarget = null;
        }
      }
    }, Actions.CLICK);
  }

  private Spatial makeDragPlanePreview() {
    float sz = 20;
    Mesh m = makeGridMesh(sz, sz, 50, 50);
    final Geometry geom = new Geometry("DragPlane", m);
    geom.setMaterial(SimpleApplicationUtils.makeLineMaterial(assetManager, ColorRGBA.Red));
    setDragPlaneTransform(sz, geom);
    return geom;
  }

  private Spatial makeDragPlane() {
    float sz = 100;
    Mesh m = new Quad(sz, sz);
    final Geometry geom = new Geometry("DragPlane", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.Red));
    setDragPlaneTransform(sz, geom);
    return geom;
  }

  private void setDragPlaneTransform(float sz, Geometry geom) {
    geom.lookAt(cam.getDirection().negate(), cam.getLeft());
    geom.setLocalTranslation(dragTarget.add(cam.getLeft().multLocal(-sz / 2)).add(cam.getUp().multLocal(-sz / 2)));
  }

  @Override
  public void simpleUpdate(float tpf) {
    if (dragTarget != null) {
      Vector3f origin = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.0f);
      Vector3f direction = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.3f);
      direction.subtractLocal(origin).normalizeLocal();
      Ray ray = new Ray(origin, direction);
      CollisionResults results = new CollisionResults();
      if (dragPlane.collideWith(ray, results) > 0) {
        dragTarget.set(results.getCollision(0).getContactPoint());
      }
    }

    setCameraPosition();
    updateBoneSpatials();
  }

  private void updateBoneSpatials() {
    for (MyBone b : bones) {
      b.spatial.setLocalTransform(transformBetween(b.start, b.end, Vector3f.UNIT_Z, b.scale));
      rootNode.attachChild(b.spatial);

      if (b.guiSpatial != null) {
        guiNode.detachChild(b.guiSpatial);
      }

      Node gui = new Node();

      Vector3f screenStart = cam.getScreenCoordinates(b.start);
      gui.attachChild(makeCircle(screenStart));

      Vector3f screenEnd = cam.getScreenCoordinates(b.end);
      gui.attachChild(makeCircle(screenEnd));
      gui.attachChild(makeLine(screenStart, screenEnd, ColorRGBA.Green));

      b.guiSpatial = gui;
      guiNode.attachChild(b.guiSpatial);
    }
  }

  private BitmapText makeCircle(Vector3f screenPos) {
    BitmapText text = new BitmapText(guiFont, false);
    text.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    text.setText("O");
    text.setLocalTranslation(screenPos.add(-text.getLineWidth() / 2, text.getLineHeight() / 2, 0));
    return text;
  }

  private Geometry makeLine(Vector3f screenStart, Vector3f screenEnd, ColorRGBA color) {
    Line lineMesh = new Line(screenStart, screenEnd);
    lineMesh.setLineWidth(2);
    Geometry line = new Geometry("line", lineMesh);
    line.setMaterial(SimpleApplicationUtils.makeLineMaterial(assetManager, color));
    return line;
  }

  private Transform transformBetween(Vector3f start, Vector3f end, Vector3f front, float scale) {
    Vector3f dir = start.subtract(end);
    Vector3f left = dir.normalize().cross(front.normalize());
    Vector3f ahead = dir.normalize().cross(left.normalize());

    Quaternion q = new Quaternion();
    q.fromAxes(left.normalize(), ahead.normalize(), dir.normalize());
    return new Transform(
        start.add(end).multLocal(0.5f), q,
        new Vector3f(dir.length() * scale, dir.length() * scale, dir.length()));
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
    Mesh m = new Box(0.2f, 0.2f, 0.5f);
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

  public Spatial makeFloor() {
    Mesh m = new Quad(100, 100);
    //Mesh m = makeGridMesh(100, 100, 50, 50);
    final Geometry geom = new Geometry("Floor", m);
    geom.setMaterial(SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.Blue));
    geom.setShadowMode(RenderQueue.ShadowMode.Receive);
    Matrix3f rot = new Matrix3f();
    rot.fromAngleNormalAxis(3 * FastMath.PI / 2, Vector3f.UNIT_X);
    geom.setLocalRotation(rot);
    geom.setLocalTranslation(-50, -3, 50);
    return geom;
  }

  public Mesh makeGridMesh(float width, float height, int xsteps, int ysteps) {
    Mesh m = new WireBox(5, 5, 5);
    Mesh mesh = new Mesh();
    mesh.setMode(Mesh.Mode.Lines);
    int lines = (xsteps + 1) + (ysteps + 1);
    IntBuffer indexes = BufferUtils.createIntBuffer(lines * 2);
    FloatBuffer vertexes = BufferUtils.createFloatBuffer(lines * 2 * 3);

    int i = 0;
    for (int x = 0; x <= xsteps; x++) {
      float xpos = x * width / xsteps;
      vertexes.put(xpos).put(0).put(0);
      vertexes.put(xpos).put(height).put(0);
      indexes.put(i).put(i + 1);
      i += 2;
    }
    for (int y = 0; y <= ysteps; y++) {
      float ypos = y * height / ysteps;
      vertexes.put(0).put(ypos).put(0);
      vertexes.put(width).put(ypos).put(0);
      indexes.put(i).put(i + 1);
      i += 2;
    }

    mesh.setBuffer(VertexBuffer.Type.Index, 2, indexes);
    mesh.setBuffer(VertexBuffer.Type.Position, 3, vertexes);

    mesh.updateBound();
    mesh.setStatic();
    return mesh;
  }

}
