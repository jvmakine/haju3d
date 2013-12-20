package fi.haju.haju3d.client;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static fi.haju.haju3d.client.SimpleApplicationUtils.makeColorMaterial;
import static fi.haju.haju3d.client.SimpleApplicationUtils.makeLineMaterial;

/**
 * TODO:
 * - RMB to create, LMB to edit
 * X LMB on empty to rotate
 * - symmetrical editing (forced?)
 * - create bone endpoint inside mesh, not on surface (try to snap to a "good" location..)
 * - the bigger bone is, the deeper inside its endpoint needs to be; how to make it so that endpoint location
 * is fixed regardless of bone size?
 * - snap dragPlane to x/y/z axes
 * <p/>
 * Backlog:
 * - save/load bones
 * - create a voxel representation out of bones, make solid mesh
 * - apply perlin noise to voxel mesh
 * - ability to select bone mesh
 * - ability to quickly edit bone mesh
 * - skin/bones animatable mesh
 * - ability to set constraints on mesh joints
 * - IK animation on bones
 * - quick way to create a leg or an arm (both consist of 3 bones)
 * - texturing
 */
public class CharacterBoneApp extends SimpleApplication {

  public static final float MINIMUM_BONE_THICKNESS = 0.05f;

  private static class Actions {
    public static final String ROTATE_LEFT = "RotateLeft";
    public static final String ROTATE_RIGHT = "RotateRight";
    public static final String ROTATE_UP = "RotateUp";
    public static final String ROTATE_DOWN = "RotateDown";
    public static final String ROTATE_LEFT_MOUSE = "RotateLeftMouse";
    public static final String ROTATE_RIGHT_MOUSE = "RotateRightMouse";
    public static final String ROTATE_UP_MOUSE = "RotateUpMouse";
    public static final String ROTATE_DOWN_MOUSE = "RotateDownMouse";
    public static final String CLICK = "Click";
    public static final String RESIZE_DOWN = "ResizeDown";
    public static final String RESIZE_UP = "ResizeUp";
    public static final String SHOW_GUIDES = "ShowGuides";
  }

  private static class MyBone {
    private Vector3f start;
    private Vector3f end;
    private float thickness = 1.0f;
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
  private boolean cameraDragging;
  private float camDistance = 10;
  private float camElevation = 0;
  private float camAzimuth = 0;
  private List<MyBone> bones = new ArrayList<>();

  private Vector3f dragTarget;
  private Spatial dragPlane;
  private Spatial dragPlanePreview;
  private boolean showGuides = true;

  @Override
  public void simpleInitApp() {
    flyCam.setEnabled(false);
    SimpleApplicationUtils.addLights(this);
    SimpleApplicationUtils.addCartoonEdges(this);
    rootNode.attachChild(makeFloor());
    rootNode.attachChild(makeAxisIndicators());

    MyBone bone = new MyBone();
    bone.start = new Vector3f(0, 2, 0);
    bone.end = new Vector3f(0, -1, 2);
    bone.thickness = 0.2f;
    bone.spatial = makeSphere();
    bones.add(bone);

    camTarget = bone.spatial;

    inputManager.addMapping(Actions.ROTATE_LEFT_MOUSE, new MouseAxisTrigger(MouseInput.AXIS_X, true));
    inputManager.addMapping(Actions.ROTATE_RIGHT_MOUSE, new MouseAxisTrigger(MouseInput.AXIS_X, false));
    inputManager.addMapping(Actions.ROTATE_UP_MOUSE, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
    inputManager.addMapping(Actions.ROTATE_DOWN_MOUSE, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
    inputManager.addListener(new AnalogListener() {
      public static final float ROTATE_SPEED = 5;

      @Override
      public void onAnalog(String name, float value, float tpf) {
        if (!cameraDragging) {
          return;
        }
        float v = value * ROTATE_SPEED;
        if (name.equals(Actions.ROTATE_LEFT_MOUSE)) {
          camAzimuth -= v;
        } else if (name.equals(Actions.ROTATE_RIGHT_MOUSE)) {
          camAzimuth += v;
        } else if (name.equals(Actions.ROTATE_DOWN_MOUSE)) {
          camElevation -= v;
          if (camElevation < -FastMath.PI / 2) {
            camElevation = -FastMath.PI / 2;
          }
        } else if (name.equals(Actions.ROTATE_UP_MOUSE)) {
          camElevation += v;
          if (camElevation > FastMath.PI / 2) {
            camElevation = FastMath.PI / 2;
          }
        }
      }
    }, Actions.ROTATE_LEFT_MOUSE, Actions.ROTATE_RIGHT_MOUSE, Actions.ROTATE_UP_MOUSE, Actions.ROTATE_DOWN_MOUSE);

    inputManager.addMapping(Actions.ROTATE_LEFT, new KeyTrigger(KeyInput.KEY_LEFT));
    inputManager.addMapping(Actions.ROTATE_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
    inputManager.addMapping(Actions.ROTATE_UP, new KeyTrigger(KeyInput.KEY_UP));
    inputManager.addMapping(Actions.ROTATE_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));
    inputManager.addListener(new AnalogListener() {
      public static final float ROTATE_SPEED = 3;

      @Override
      public void onAnalog(String name, float value, float tpf) {
        float v = value * ROTATE_SPEED;
        if (name.equals(Actions.ROTATE_LEFT)) {
          camAzimuth -= v;
        } else if (name.equals(Actions.ROTATE_RIGHT)) {
          camAzimuth += v;
        } else if (name.equals(Actions.ROTATE_DOWN)) {
          camElevation -= v;
          if (camElevation < -FastMath.PI / 2) {
            camElevation = -FastMath.PI / 2;
          }
        } else if (name.equals(Actions.ROTATE_UP)) {
          camElevation += v;
          if (camElevation > FastMath.PI / 2) {
            camElevation = FastMath.PI / 2;
          }
        }
      }
    }, Actions.ROTATE_LEFT, Actions.ROTATE_RIGHT, Actions.ROTATE_UP, Actions.ROTATE_DOWN);

    inputManager.addMapping(Actions.RESIZE_DOWN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
    inputManager.addMapping(Actions.RESIZE_UP, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
    inputManager.addListener(new AnalogListener() {
      @Override
      public void onAnalog(String name, float value, float tpf) {
        if (name.equals(Actions.RESIZE_DOWN)) {
          value = -value;
        }
        MyBone bone = findCurrentBone();
        if (bone != null) {
          bone.thickness += value * 0.05f;
          if (bone.thickness < MINIMUM_BONE_THICKNESS) {
            bone.thickness = MINIMUM_BONE_THICKNESS;
          }
        }
      }
    }, Actions.RESIZE_DOWN, Actions.RESIZE_UP);

    inputManager.addMapping(Actions.CLICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          stopDragging();
          dragTarget = findDragTarget();
          if (dragTarget != null) {
            startDragging();
          } else {
            Vector3f attachPoint = findBoneAttachPoint();
            if (attachPoint != null) {
              // create new bone
              MyBone bone = new MyBone();
              bone.start = attachPoint.clone();
              bone.end = attachPoint.clone();
              bone.thickness = 0.2f;
              bone.spatial = makeSphere();
              bones.add(bone);
              dragTarget = bone.end;
              startDragging();
            } else {
              // start moving camera
              cameraDragging = true;
            }
          }
        } else {
          stopDragging();
          cameraDragging = false;
        }
      }
    }, Actions.CLICK);

    inputManager.addMapping(Actions.SHOW_GUIDES, new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          showGuides = !showGuides;
        }
      }
    }, Actions.SHOW_GUIDES);
  }


  private void startDragging() {
    // start dragging
    dragPlane = makeDragPlane();
//    dragPlanePreview = makeDragPlanePreview();
//    rootNode.attachChild(dragPlanePreview);
  }

  private void stopDragging() {
    // stop dragging
    if (dragPlanePreview != null) {
      rootNode.detachChild(dragPlanePreview);
    }
    dragPlane = null;
    dragPlanePreview = null;
    dragTarget = null;
  }

  private Vector3f findBoneAttachPoint() {
    CollisionResult collision = findBoneCollision();
    if (collision != null) {
      return collision.getContactPoint();
    }
    return null;
  }

  private MyBone findCurrentBone() {
    CollisionResult collision = findBoneCollision();
    if (collision != null) {
      Geometry geom = collision.getGeometry();
      for (MyBone bone : bones) {
        if (bone.spatial == geom) {
          return bone;
        }
      }
    }
    return null;
  }

  private CollisionResult findBoneCollision() {
    Node allBones = new Node();
    for (MyBone bone : bones) {
      allBones.attachChild(bone.spatial);
    }
    CollisionResults results = new CollisionResults();
    int i = allBones.collideWith(getCursorRay(), results);
    if (i > 0) {
      return results.getClosestCollision();
    }
    return null;
  }

  private Vector3f findDragTarget() {
    Vector3f best = null;
    float bestDist = 20;
    for (MyBone bone : bones) {
      float distance = cursorDistanceTo(bone.start);
      if (distance < bestDist) {
        bestDist = distance;
        best = bone.start;
      }
      distance = cursorDistanceTo(bone.end);
      if (distance < bestDist) {
        bestDist = distance;
        best = bone.end;
      }
    }
    return best;
  }

  private float cursorDistanceTo(Vector3f pos) {
    Vector3f sc = cam.getScreenCoordinates(pos);
    return new Vector2f(sc.x, sc.y).distance(inputManager.getCursorPosition());
  }

  private Spatial makeAxisIndicators() {
    Node n = new Node("AxisIndicators");
    float sz = 20;
    Mesh m = makeGridMesh(sz, sz, 20, 20);
    //n.attachChild(makeDragPlane(sz, m, makeLineMaterial(assetManager, ColorRGBA.Red), Vector3f.UNIT_Z, Vector3f.UNIT_X, Vector3f.ZERO));
    ColorRGBA color = new ColorRGBA(0.5f, 0.5f, 0, 0.2f);
    n.attachChild(makeDragPlane(sz, m, makeLineMaterial(assetManager, color), Vector3f.UNIT_X, Vector3f.UNIT_Z, Vector3f.ZERO));
    return n;
  }

  private Spatial makeDragPlanePreview() {
    float sz = 20;
    Mesh m = makeGridMesh(sz, sz, 50, 50);
    return makeDragPlane(sz, m, makeLineMaterial(assetManager, ColorRGBA.Red));
  }

  private Spatial makeDragPlane() {
    float sz = 100;
    Mesh m = new Quad(sz, sz);
    return makeDragPlane(sz, m, makeColorMaterial(assetManager, ColorRGBA.Red));
  }

  private Spatial makeDragPlane(float sz, Mesh mesh, Material material) {
    // mesh should be a plane [x=[0..sz],y=[0..sz],z=0]
    Vector3f direction = getSnappedVector(cam.getDirection());
    Vector3f left = getSnappedVector(cam.getLeft());
    Vector3f center = dragTarget;
    return makeDragPlane(sz, mesh, material, direction, left, center);
  }

  public static Vector3f[] AXES = new Vector3f[] {
      Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z,
      Vector3f.UNIT_X.negate(), Vector3f.UNIT_Y.negate(), Vector3f.UNIT_Z.negate()};

  public static Vector3f getSnappedVector(Vector3f v) {
    double snapDist = 0.4;
    for (Vector3f axis : AXES) {
      if (v.distanceSquared(axis) < snapDist) {
        return axis;
      }
    }
    return v;
  }

  private static Spatial makeDragPlane(float sz, Mesh mesh, Material material, Vector3f direction, Vector3f left, Vector3f center) {
    final Geometry geom = new Geometry("DragPlane", mesh);
    geom.setMaterial(material);
    geom.setLocalTranslation(-sz / 2, -sz / 2, 0);
    Node node = new Node("DragPlane2");
    node.attachChild(geom);
    node.lookAt(direction.negate(), left);
    node.setLocalTranslation(center);
    return node;
  }

  @Override
  public void simpleUpdate(float tpf) {
    if (dragTarget != null) {
      Ray ray = getCursorRay();
      CollisionResults results = new CollisionResults();
      if (dragPlane.collideWith(ray, results) > 0) {
        dragTarget.set(results.getCollision(0).getContactPoint());
      }
    }

    setCameraPosition();
    updateBoneSpatials();
  }

  /**
   * returns a ray pointing from camera to cursor
   */
  private Ray getCursorRay() {
    Vector3f origin = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.0f);
    Vector3f direction = cam.getWorldCoordinates(inputManager.getCursorPosition(), 0.3f);
    direction.subtractLocal(origin).normalizeLocal();
    return new Ray(origin, direction);
  }

  private void updateBoneSpatials() {
    for (MyBone b : bones) {
      Transform t = transformBetween(b.start, b.end, Vector3f.UNIT_Z, b.thickness);
      if (Vector3f.isValidVector(t.getTranslation())) {
        b.spatial.setLocalTransform(t);
        rootNode.attachChild(b.spatial);
      }

      if (b.guiSpatial != null) {
        guiNode.detachChild(b.guiSpatial);
        b.guiSpatial = null;
      }

      if (showGuides) {
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
    line.setMaterial(makeLineMaterial(assetManager, color));
    return line;
  }

  private Geometry makeArrow(Vector3f screenStart, Vector3f screenEnd, ColorRGBA color) {
    Arrow lineMesh = new Arrow(screenEnd.subtract(screenStart));
    lineMesh.setLineWidth(2);
    Geometry line = new Geometry("line", lineMesh);
    line.setLocalTranslation(screenStart);
    line.setMaterial(makeLineMaterial(assetManager, color));
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
    geom.setMaterial(makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

  public Spatial makeSphere() {
    Mesh m = new Sphere(20, 20, 0.6f);
    final Geometry geom = new Geometry("ColoredMesh", m);
    geom.setMaterial(makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
  }

  public Spatial makeFloor() {
    Mesh m = new Quad(100, 100);
    final Geometry geom = new Geometry("Floor", m);
    geom.setMaterial(makeColorMaterial(assetManager, ColorRGBA.Blue));
    geom.setShadowMode(RenderQueue.ShadowMode.Receive);
    Matrix3f rot = new Matrix3f();
    rot.fromAngleNormalAxis(3 * FastMath.PI / 2, Vector3f.UNIT_X);
    geom.setLocalRotation(rot);
    geom.setLocalTranslation(-50, -3, 50);
    return geom;
  }

  public Mesh makeGridMesh(float width, float height, int xsteps, int ysteps) {
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
