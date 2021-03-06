package fi.haju.haju3d.client.bones;

import com.jme3.math.Vector3f;
import org.apache.commons.lang3.Validate;

public class MyBone {
  private Vector3f attachPoint;
  private Vector3f freePoint;
  private float thickness = 1.0f;
  //private float rotation;
  private String meshName;
  private MyBone mirrorBone;
  private MyBone parentBone;

  public MyBone(Vector3f attachPoint, Vector3f freePoint, float thickness, String meshName) {
    Validate.notNull(attachPoint);
    Validate.notNull(freePoint);
    Validate.notNull(meshName);
    this.attachPoint = attachPoint;
    this.freePoint = freePoint;
    this.thickness = thickness;
    this.meshName = meshName;
  }

  public void setMirrorBone(MyBone mirrorBone) {
    // detach from old mirror:
    if (this.mirrorBone != null) {
      this.mirrorBone.mirrorBone = null;
    }
    // set up new mirror link:
    this.mirrorBone = mirrorBone;
    if (mirrorBone != null) {
      mirrorBone.mirrorBone = this;
    }
  }

  public void setParentBone(MyBone parentBone) {
    this.parentBone = parentBone;
  }

  // only use for animated bones, not for editable bones
  public void setThicknessSelf(float value) {
    this.thickness = value;
  }

  public void addThickness(float value) {
    addThicknessSelf(value);
    if (mirrorBone != null) {
      mirrorBone.addThicknessSelf(value);
    }
  }

  public void addThicknessSelf(float value) {
    this.thickness = this.thickness + value * 0.05f;
    if (this.thickness < CharacterEditorApp.MINIMUM_BONE_THICKNESS) {
      this.thickness = CharacterEditorApp.MINIMUM_BONE_THICKNESS;
    }
  }

  public void setPosition(Vector3f p, boolean isAttachPosition) {
    setPositionSelf(p, isAttachPosition);
    if (mirrorBone != null) {
      mirrorBone.setPositionSelf(CharacterEditorApp.getMirroredVector(p), isAttachPosition);
    }
  }

  public void setPositionSelf(Vector3f p, boolean isAttachPosition) {
    if (isAttachPosition) {
      this.attachPoint = p;
    } else {
      this.freePoint = p;
    }
  }

  public Vector3f getAttachPoint() {
    return attachPoint;
  }

  public Vector3f getFreePoint() {
    return freePoint;
  }

  public float getThickness() {
    return thickness;
  }

  public MyBone getMirrorBone() {
    return mirrorBone;
  }

  public String getMeshName() {
    return meshName;
  }

  public void setMeshName(String meshName) {
    this.meshName = meshName;
  }

  public MyBone getParentBone() {
    return parentBone;
  }
}
