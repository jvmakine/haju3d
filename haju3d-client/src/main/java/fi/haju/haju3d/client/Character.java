package fi.haju.haju3d.client;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class Character {
  private Vector3f position = new Vector3f();
  private Vector3f velocity = new Vector3f();
  private float faceAzimuth;
  private float lookAzimuth;
  private float feetCycle;
  private float lookElevation = (float) (Math.PI / 4);

  public Vector3f getVelocity() {
    return velocity;
  }

  public float getFaceAzimuth() {
    return faceAzimuth;
  }

  public float getLookAzimuth() {
    return lookAzimuth;
  }

  public float getLookElevation() {
    return lookElevation;
  }

  public void setVelocity(Vector3f velocity) {
    this.velocity = velocity;
  }

  public void setFaceAzimuth(float faceAzimuth) {
    this.faceAzimuth = faceAzimuth;
  }

  public void setLookAzimuth(float lookAzimuth) {
    this.lookAzimuth = lookAzimuth;
  }

  public void setLookElevation(float lookElevation) {
    this.lookElevation = lookElevation;
  }

  public Vector3f getPosition() {
    return position;
  }

  public void setPosition(Vector3f position) {
    this.position = position;
  }

  public Quaternion getFacingQuaternion() {
    Quaternion quat = new Quaternion();
    quat.fromAngles(0, getFaceAzimuth(), 0.0f);
    return quat;
  }

  public Quaternion getLookQuaternion() {
    Quaternion quat = new Quaternion();
    quat.fromAngles(getLookElevation(), getLookAzimuth(), 0.0f);
    return quat;
  }

  public Vector3f getLookVector() {
    return new Vector3f(FastMath.sin(getLookAzimuth()), 0, FastMath.cos(getLookAzimuth()));
  }

  public Vector3f getFaceVector() {
    return new Vector3f(FastMath.sin(getFaceAzimuth()), 0, FastMath.cos(getFaceAzimuth()));
  }

  public float getFeetCycle() {
    return feetCycle;
  }

  public void setFeetCycle(float feetCycle) {
    this.feetCycle = feetCycle;
  }
}
