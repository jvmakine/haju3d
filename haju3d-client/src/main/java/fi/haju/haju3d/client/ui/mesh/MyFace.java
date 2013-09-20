package fi.haju.haju3d.client.ui.mesh;

import com.jme3.math.Vector3f;

public class MyFace {
  public MyVertex v1;
  public MyVertex v2;
  public MyVertex v3;
  public MyVertex v4;
  public Vector3f normal;
  public Vector3f center;
  public MyTexture texture;
  public float color;
  public boolean realTile;
  public int zIndex;

  public MyFace(MyVertex v1, MyVertex v2, MyVertex v3, MyVertex v4, MyTexture texture, float color, boolean realTile, int zIndex) {
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    this.v4 = v4;
    this.texture = texture;
    this.color = color;
    this.realTile = realTile;
    this.zIndex = zIndex;
  }
  
  public void calcCenter() {
    this.center = v1.v.clone().addLocal(v2.v).addLocal(v3.v).addLocal(v4.v).divide(4);
  }
  
  public Vector3f getCenter() {
    return center;
  }
}