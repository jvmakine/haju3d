package fi.haju.haju3d.client.bones;

import com.jme3.math.Vector3f;
import org.apache.commons.lang3.Validate;

public class MyBone {
  private Vector3f start;
  private Vector3f end;
  private float thickness = 1.0f;
  //private float rotation;
  private String meshName;
  private MyBone mirrorBone;

  public MyBone(Vector3f start, Vector3f end, float thickness, String meshName) {
    Validate.notNull(start);
    Validate.notNull(end);
    Validate.notNull(meshName);
    this.start = start;
    this.end = end;
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

  public void setPosition(Vector3f p, boolean start) {
    setPositionSelf(p, start);
    if (mirrorBone != null) {
      mirrorBone.setPositionSelf(CharacterEditorApp.getMirroredVector(p), start);
    }
  }

  public void setPositionSelf(Vector3f p, boolean start) {
    if (start) {
      this.start = p;
    } else {
      this.end = p;
    }
  }

  public Vector3f getStart() {
    return start;
  }

  public Vector3f getEnd() {
    return end;
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
}
