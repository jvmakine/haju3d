package fi.haju.haju3d.client.bones;

import com.jme3.math.*;

/**
 * Utilities for converting bone information into a Transform.
 */
public final class BoneTransformUtils {
  private BoneTransformUtils() {
    assert false;
  }


  public static Transform boneTransform(MyBone b) {
    return transformBetween(b.getAttachPoint(), b.getFreePoint(), Vector3f.UNIT_X, b.getThickness(), false);
  }

  public static Transform boneTransform2(MyBone b) {
    return transformBetween(b.getAttachPoint(), b.getFreePoint(), Vector3f.UNIT_X, b.getThickness(), true);
  }

  public static Matrix4f getTransformMatrix(Transform t) {
    Matrix4f m = new Matrix4f();
    m.setTransform(t.getTranslation(), t.getScale(), t.getRotation().toRotationMatrix());
    return m;
  }

  public static Transform transformBetween(Vector3f start, Vector3f end, Vector3f front, float scale, boolean preserveVolume) {
    Vector3f dir = start.subtract(end);
    Vector3f dirn = dir.normalize();
    Vector3f left = dirn.cross(front.normalize());
    Vector3f ahead = dirn.cross(left.normalize());

    Quaternion q = new Quaternion();
    q.fromAxes(left.normalize(), ahead.normalize(), dirn);

    Vector3f midPoint = start.add(end).multLocal(0.5f);
    Vector3f scalev = preserveVolume
        ? new Vector3f(scale / FastMath.sqrt(dir.length()), scale / FastMath.sqrt(dir.length()), dir.length())
        : new Vector3f(scale * dir.length(), scale * dir.length(), dir.length());
    return new Transform(midPoint, q, scalev);
  }

}
