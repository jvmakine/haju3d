package fi.haju.haju3d.client;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class Character {
  private Vector3f velocity = new Vector3f();
  private float faceAzimuth;
  private float lookAzimuth;
  private float lookElevation = (float) (Math.PI / 4);
  private final Node node;

  public Character(Node node) {
    this.node = node;
  }

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

  public Node getNode() {
    return node;
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
    return node.getLocalTranslation();
  }

  public void setPosition(Vector3f position) {
    node.setLocalTranslation(position);
    node.setLocalRotation(getFacingQuaternion());
  }

  private Quaternion getFacingQuaternion() {
    Quaternion quat = new Quaternion();
    quat.fromAngles(0, getFaceAzimuth(), 0.0f);
    return quat;
  }

  public Quaternion getLookQuaternion() {
    Quaternion quat = new Quaternion();
    quat.fromAngles(getLookElevation(), getLookAzimuth(), 0.0f);
    return quat;
  }

}
