package fi.haju.haju3d.client.bones;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import fi.haju.haju3d.client.SimpleApplicationUtils;
import fi.haju.haju3d.protocol.world.ByteArray3d;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fi.haju.haju3d.client.SimpleApplicationUtils.makeColorMaterial;
import static fi.haju.haju3d.client.SimpleApplicationUtils.makeLineMaterial;

/**
 * TODO:
 * - when looking at mesh:
 * -- somehow attach joints together
 * -- ideally IK movement of joints
 * - marching cubes meshing (just blur the boneworldgrid a bit)
 * -- solves: smoothing spikes, higher quality sphere, ladders in bone weights..and can be faster
 * - MeshToBone
 * - bug: somehow location of mesh affects transforms?
 * <p/>
 * Backlog:
 * - IK animation on bones
 * - ability to set constraints on mesh joints
 * - ability to quickly edit bone mesh
 * - ability to rotate bone mesh
 * - quick way to create a leg or an arm (both consist of 3 bones)
 * Tough problems:
 * - boneTransform is not very robust, the rotation is random
 * - texturing
 * <p/>
 * Bone snapping:
 * - create bone endpoint inside mesh, not on surface (try to snap to a "good" location..)
 * - the bigger bone is, the deeper inside its endpoint needs to be; how to make it so that endpoint location
 * is fixed regardless of bone size?
 * - or maybe endpoint should be on surface, but bone mesh extends beyond endpoint?
 * - maybe endpoint should always be forced on surface, no free movement allowed?
 * <p/>
 * Done
 * - when looking at mesh, use different boneTransform; scale ~ sqrt(length)
 * - when looking at mesh, don't save bone locations
 * - when looking at mesh, allow non-mirrored bones move off x-plane
 * - ability to select bone mesh
 * - show bone mesh when editing
 * - create a voxel representation out of bones, make solid mesh
 * - apply perlin noise to voxel mesh
 * - skin/bones animatable mesh
 * - ability to delete bones
 * - save/load bones
 * - RMB to create, LMB to edit
 * - symmetrical editing (forced?)
 * - LMB on empty to rotate
 * - snap dragPlane to x/y/z axes
 */
public class CharacterBoneApp extends SimpleApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(CharacterBoneApp.class);

  public static final float MINIMUM_BONE_THICKNESS = 0.05f;
  public static final File BONE_FILE = new File("bones3.json");
  public static final Charset BONE_FILE_ENCODING = Charset.forName("UTF-8");

  private MyBone camTarget;
  private boolean cameraDragging;
  private float camDistance = 15;
  private float camElevation = 0;
  private float camAzimuth = 0;
  private List<MyBone> bones;
  private List<MyBone> activeBones;
  private Node boneSpatials = new Node();

  private DragTarget dragTarget;
  private Spatial dragPlane;
  private Spatial dragPlanePreview;
  private boolean showGuides = false;
  private boolean showMesh = false;

  private Spatial axisIndicators;
  private Geometry meshSpatial = null;
  private List<Matrix4f> meshBoneBindPoseInverseTransforms;
  private String currentBoneMeshName = SPHERE_MESH;

  private static class Actions {
    public static final String ROTATE_LEFT_MOUSE = "RotateLeftMouse";
    public static final String ROTATE_RIGHT_MOUSE = "RotateRightMouse";
    public static final String ROTATE_UP_MOUSE = "RotateUpMouse";
    public static final String ROTATE_DOWN_MOUSE = "RotateDownMouse";
    public static final String CLICK = "Click";
    public static final String CLICK_RMB = "ClickRmb";
    public static final String RESIZE_DOWN = "ResizeDown";
    public static final String RESIZE_UP = "ResizeUp";
    public static final String SHOW_GUIDES = "ShowGuides";
    public static final String DELETE_BONE = "DeleteBone";
    public static final String SHOW_MESH = "ShowMesh";
    public static final String SELECT_BONE_MESH_1 = "SelectBoneMesh1";
    public static final String SELECT_BONE_MESH_2 = "SelectBoneMesh2";
    public static final String SELECT_BONE_MESH_3 = "SelectBoneMesh3";
  }

  private static final String SPHERE_MESH = "SPHERE";
  private static final String BOX_MESH = "BOX";
  private static final String BLOB_MESH = "BLOB";

  private static final Map<String, ByteArray3d> MESH_GRID_MAP = new HashMap<>();

  static {
    MESH_GRID_MAP.put(SPHERE_MESH, BoneMeshUtils.makeSphereBoneMeshGrid());
    MESH_GRID_MAP.put(BOX_MESH, BoneMeshUtils.makeBoxBoneMeshGrid());
    MESH_GRID_MAP.put(BLOB_MESH, BoneMeshUtils.makeBlobBoneMeshGrid());
  }

  private static final Map<String, Mesh> MESH_MAP = new HashMap<>();

  static {
    for (Map.Entry<String, ByteArray3d> e : MESH_GRID_MAP.entrySet()) {
      MESH_MAP.put(e.getKey(), BoneMeshUtils.makeBoneMesh(e.getValue()));
    }
  }

  private static class DragTarget {
    private MyBone bone;
    private boolean isStart;

    private DragTarget(MyBone bone, boolean isStart) {
      this.bone = bone;
      this.isStart = isStart;
    }

    public Vector3f getPosition() {
      return isStart ? bone.getStart() : bone.getEnd();
    }

    public void setPosition(Vector3f p) {
      bone.setPosition(p, isStart);
    }
  }

  public static Vector3f getMirroredVector(Vector3f p) {
    return new Vector3f(-p.x, p.y, p.z);
  }

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
    rootNode.attachChild(BoneMeshUtils.makeFloor(makeColorMaterial(assetManager, ColorRGBA.Blue)));
    axisIndicators = makeAxisIndicators();
    if (showGuides) {
      rootNode.attachChild(axisIndicators);
    }
    rootNode.attachChild(boneSpatials);

    try {
      bones = BoneSaveUtils.readBones(FileUtils.readFileToString(BONE_FILE, BONE_FILE_ENCODING));
    } catch (Exception e) {
      e.printStackTrace();
      bones = new ArrayList<>();

      LOGGER.warn("Could not read bone file. Create default bone.");
      MyBone bone = new MyBone(new Vector3f(0, 2, 0), new Vector3f(0, -1, 2), 0.2f, SPHERE_MESH);
      bones.add(bone);
    }

    activeBones = bones;

    camTarget = bones.get(0);

    for (MyBone bone : bones) {
      addBoneSpatial(bone);
    }

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
        switch (name) {
        case Actions.ROTATE_LEFT_MOUSE:
          camAzimuth -= v;
          break;
        case Actions.ROTATE_RIGHT_MOUSE:
          camAzimuth += v;
          break;
        case Actions.ROTATE_DOWN_MOUSE:
          camElevation -= v;
          if (camElevation < -FastMath.PI / 2) {
            camElevation = -FastMath.PI / 2;
          }
          break;
        case Actions.ROTATE_UP_MOUSE:
          camElevation += v;
          if (camElevation > FastMath.PI / 2) {
            camElevation = FastMath.PI / 2;
          }
          break;
        }
      }
    }, Actions.ROTATE_LEFT_MOUSE, Actions.ROTATE_RIGHT_MOUSE, Actions.ROTATE_UP_MOUSE, Actions.ROTATE_DOWN_MOUSE);

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
          bone.addThickness(value);
        }
      }
    }, Actions.RESIZE_DOWN, Actions.RESIZE_UP);

    inputManager.addMapping(Actions.CLICK, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addMapping(Actions.CLICK_RMB, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          stopDragging();
          if (name.equals(Actions.CLICK)) {
            dragTarget = findDragTarget();
          } else if (name.equals(Actions.CLICK_RMB) && !showMesh) {
            Vector3f attachPoint = findBoneCollisionPoint();
            if (attachPoint != null) {
              MyBone bone = createNewBone(attachPoint);
              dragTarget = new DragTarget(bone, false);
            }
          }
          if (dragTarget != null) {
            startDragging();
          } else {
            // start moving camera
            cameraDragging = true;
          }
        } else {
          stopDragging();
          cameraDragging = false;
        }
      }
    }, Actions.CLICK, Actions.CLICK_RMB);

    inputManager.addMapping(Actions.SHOW_GUIDES, new KeyTrigger(KeyInput.KEY_RETURN));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          showGuides = !showGuides;
          if (showGuides) {
            rootNode.attachChild(axisIndicators);
          } else {
            rootNode.detachChild(axisIndicators);
          }
        }
      }
    }, Actions.SHOW_GUIDES);

    inputManager.addMapping(Actions.SHOW_MESH, new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          showMesh = !showMesh;
          if (showMesh) {
            LOGGER.info("Show mesh");

            activeBones = BoneSaveUtils.cloneBones(bones);

            for (MyBone bone : activeBones) {
              Vector3f dir = bone.getStart().subtract(bone.getEnd());
              float length = dir.length();
              // the way bone scaling works is different when moving mesh bones than when editing them,
              // so we must solve for new thickness:
              // t1*len = t2/sqrt(len), solve for t2
              bone.setThicknessSelf(bone.getThickness() * length * FastMath.sqrt(length));
            }

            Mesh mesh = BoneMeshUtils.buildMesh(activeBones, MESH_GRID_MAP);

            meshBoneBindPoseInverseTransforms = new ArrayList<>();
            for (MyBone bone : activeBones) {
              Transform transform = BoneMeshUtils.boneTransform2(bone);
              meshBoneBindPoseInverseTransforms.add(BoneMeshUtils.getTransformMatrix(transform).invert());
            }

            // Create model
            Geometry geom = new Geometry("BoneMesh", mesh);
            Material meshMaterial = SimpleApplicationUtils.makeColorMaterial(assetManager, ColorRGBA.White);
            meshMaterial.setBoolean("UseVertexColor", true);
            meshMaterial.setInt("NumberOfBones", activeBones.size());
            geom.setMaterial(meshMaterial);
            geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

            meshSpatial = geom;
            rootNode.attachChild(meshSpatial);
            rootNode.detachChild(boneSpatials);
          } else {
            LOGGER.info("Hide mesh");
            activeBones = bones;
            if (meshSpatial != null) {
              rootNode.detachChild(meshSpatial);
            }
            meshSpatial = null;
            meshBoneBindPoseInverseTransforms = null;
            rootNode.attachChild(boneSpatials);
          }

        }
      }
    }, Actions.SHOW_MESH);

    inputManager.addMapping(Actions.DELETE_BONE, new KeyTrigger(KeyInput.KEY_DELETE));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed && !showMesh) {
          MyBone bone = findCurrentBone();
          if (bone != null && bones.indexOf(bone) != 0) {
            removeBoneAndMirror(bone);
          }
        }
      }
    }, Actions.DELETE_BONE);

    inputManager.addMapping(Actions.SELECT_BONE_MESH_1, new KeyTrigger(KeyInput.KEY_1));
    inputManager.addMapping(Actions.SELECT_BONE_MESH_2, new KeyTrigger(KeyInput.KEY_2));
    inputManager.addMapping(Actions.SELECT_BONE_MESH_3, new KeyTrigger(KeyInput.KEY_3));
    inputManager.addListener(new ActionListener() {
      @Override
      public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
          MyBone bone = findCurrentBone();
          if (bone != null) {
            switch (name) {
            case Actions.SELECT_BONE_MESH_1:
              selectBoneMeshAndMirror(bone, BOX_MESH);
              break;
            case Actions.SELECT_BONE_MESH_2:
              selectBoneMeshAndMirror(bone, SPHERE_MESH);
              break;
            case Actions.SELECT_BONE_MESH_3:
              selectBoneMeshAndMirror(bone, BLOB_MESH);
              break;
            }
          }
        }
      }
    }, Actions.SELECT_BONE_MESH_1, Actions.SELECT_BONE_MESH_2, Actions.SELECT_BONE_MESH_3);
  }

  private MyBone createNewBone(Vector3f attachPoint) {
    // create new bone
    MyBone bone = new MyBone(attachPoint.clone(), attachPoint.clone(), 0.2f, currentBoneMeshName);
    bones.add(bone);
    addBoneSpatial(bone);

    MyBone bone2 = new MyBone(getMirroredVector(attachPoint), getMirroredVector(attachPoint), 0.2f, currentBoneMeshName);
    bones.add(bone2);
    addBoneSpatial(bone2);

    bone.setMirrorBone(bone2);
    return bone;
  }

  private void selectBoneMeshAndMirror(MyBone bone, String meshName) {
    if (bone.getMirrorBone() != null) {
      selectBoneMesh(bone.getMirrorBone(), meshName);
    }
    selectBoneMesh(bone, meshName);
  }

  private void selectBoneMesh(MyBone bone, String meshName) {
    bone.setMeshName(meshName);
    int index = bones.indexOf(bone);
    boneSpatials.detachChildAt(index);
    boneSpatials.attachChildAt(makeBoneSpatial(bone), index);
  }

  private void addBoneSpatial(MyBone bone) {
    boneSpatials.attachChild(makeBoneSpatial(bone));
  }

  private Geometry makeBoneSpatial(MyBone bone) {
    Mesh mesh = MESH_MAP.get(bone.getMeshName());
    final Geometry geom = new Geometry("BoneSpatial", mesh);
    geom.setMaterial(makeColorMaterial(assetManager, ColorRGBA.White));
    geom.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    return geom;
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
    // possibly remove mirror buddy if bones lie along X plane
    if (dragTarget != null && dragTarget.bone.getMirrorBone() != null) {
      final float snapToXDistance = 0.3f;
      if (FastMath.abs(dragTarget.bone.getStart().x) < snapToXDistance && FastMath.abs(dragTarget.bone.getEnd().x) < snapToXDistance) {
        removeBone(dragTarget.bone.getMirrorBone());
        dragTarget.bone.getStart().x = 0;
        dragTarget.bone.getEnd().x = 0;
      }
    }
    dragPlane = null;
    dragPlanePreview = null;
    dragTarget = null;
  }

  public void removeBoneAndMirror(MyBone bone) {
    if (bone.getMirrorBone() != null) {
      removeBone(bone.getMirrorBone());
    }
    removeBone(bone);
  }

  public void removeBone(MyBone bone) {
    bone.setMirrorBone(null);
    boneSpatials.detachChildAt(bones.indexOf(bone));
    bones.remove(bone);
  }

  private Vector3f findBoneCollisionPoint() {
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
      return activeBones.get(boneSpatials.getChildIndex(geom));
    }
    return null;
  }

  private CollisionResult findBoneCollision() {
    CollisionResults results = new CollisionResults();
    int i = boneSpatials.collideWith(getCursorRay(), results);
    if (i > 0) {
      return results.getClosestCollision();
    }
    return null;
  }

  private DragTarget findDragTarget() {
    DragTarget best = null;
    float bestDist = 20;
    for (MyBone bone : activeBones) {
      float distance = cursorDistanceTo(bone.getStart());
      if (distance < bestDist) {
        bestDist = distance;
        best = new DragTarget(bone, true);
      }
      distance = cursorDistanceTo(bone.getEnd());
      if (distance < bestDist) {
        bestDist = distance;
        best = new DragTarget(bone, false);
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
    Mesh m = BoneMeshUtils.makeGridMesh(sz, sz, 20, 20);
    //n.attachChild(makeDragPlane(sz, m, makeLineMaterial(assetManager, ColorRGBA.Red), Vector3f.UNIT_Z, Vector3f.UNIT_X, Vector3f.ZERO));
    ColorRGBA color = new ColorRGBA(0.5f, 0.5f, 0, 0.2f);
    n.attachChild(BoneMeshUtils.makeDragPlane(sz, m, makeLineMaterial(assetManager, color), Vector3f.UNIT_X, Vector3f.UNIT_Z, Vector3f.ZERO));
    return n;
  }

  private Spatial makeDragPlanePreview() {
    float sz = 20;
    Mesh m = BoneMeshUtils.makeGridMesh(sz, sz, 50, 50);
    return makeDragPlane(sz, m, makeLineMaterial(assetManager, ColorRGBA.Red));
  }

  private Spatial makeDragPlane() {
    float sz = 100;
    Mesh m = new Quad(sz, sz);
    return makeDragPlane(sz, m, makeColorMaterial(assetManager, ColorRGBA.Red));
  }

  private Spatial makeDragPlane(float sz, Mesh mesh, Material material) {
    // mesh should be a plane [x=[0..sz],y=[0..sz],z=0]
    Vector3f direction = BoneMeshUtils.getAxisSnappedVector(cam.getDirection());
    Vector3f left = BoneMeshUtils.getAxisSnappedVector(cam.getLeft());
    Vector3f center = dragTarget.getPosition();

    // non-mirrored bones only dragged along X-plane
    if (!showMesh) {
      if (dragTarget.bone.getMirrorBone() == null) {
        direction = Vector3f.UNIT_X;
        left = Vector3f.UNIT_Z;
      }
    }

    return BoneMeshUtils.makeDragPlane(sz, mesh, material, direction, left, center);
  }

  @Override
  public void simpleUpdate(float tpf) {
    if (dragTarget != null) {
      Ray ray = getCursorRay();
      CollisionResults results = new CollisionResults();
      if (dragPlane.collideWith(ray, results) > 0) {
        dragTarget.setPosition(results.getCollision(0).getContactPoint());
      }
    }

    setCameraPosition();
    updateBoneSpatials();

    if (showMesh) {
      // apply new bone tranformations to BoneMatrices
      int i = 0;
      Matrix4f[] offsetMatrices = new Matrix4f[activeBones.size()];
      for (MyBone bone : activeBones) {
        Transform transform = BoneMeshUtils.boneTransform2(bone);
        Matrix4f m = BoneMeshUtils.getTransformMatrix(transform);
        offsetMatrices[i] = m.mult(meshBoneBindPoseInverseTransforms.get(i));

        i++;
      }
      meshSpatial.getMaterial().setParam("BoneMatrices", VarType.Matrix4Array, offsetMatrices);
    }
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
    guiNode.detachAllChildren();

    int i = 0;
    for (MyBone b : activeBones) {
      Transform t = showMesh ? BoneMeshUtils.boneTransform2(b) : BoneMeshUtils.boneTransform(b);
      if (Vector3f.isValidVector(t.getTranslation())) {
        boneSpatials.getChild(i).setLocalTransform(t);
      }
      i++;
    }

    MyBone b = findCurrentBone();
    if (b != null) {
      Node gui = new Node();

      Vector3f screenStart = cam.getScreenCoordinates(b.getStart());
      gui.attachChild(BoneMeshUtils.makeCircle(screenStart, guiFont));

      Vector3f screenEnd = cam.getScreenCoordinates(b.getEnd());
      gui.attachChild(BoneMeshUtils.makeCircle(screenEnd, guiFont));
      gui.attachChild(BoneMeshUtils.makeLine(screenStart, screenEnd, makeLineMaterial(assetManager, ColorRGBA.Green)));
      guiNode.attachChild(gui);
    }

  }

  private void setCameraPosition() {
    Quaternion quat = getCameraQuaternion();
    cam.setRotation(quat);
    Vector3f camPos = camTarget.getStart().add(camTarget.getEnd()).divideLocal(2);
    Vector3f lookDir = quat.mult(Vector3f.UNIT_Z);
    camPos.addLocal(lookDir.mult(-camDistance));
    cam.setLocation(camPos);
  }

  private Quaternion getCameraQuaternion() {
    Quaternion quat = new Quaternion();
    quat.fromAngles(camElevation, camAzimuth, 0.0f);
    return quat;
  }

  @Override
  public void destroy() {
    super.destroy();
    if (bones != null) {
      try {
        FileUtils.writeStringToFile(BONE_FILE, BoneSaveUtils.saveBones(bones), BONE_FILE_ENCODING);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
