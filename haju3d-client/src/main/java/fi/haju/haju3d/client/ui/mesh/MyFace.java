package fi.haju.haju3d.client.ui.mesh;

import com.jme3.math.Vector3f;

class MyFace {
  MyVertex v1;
  MyVertex v2;
  MyVertex v3;
  MyVertex v4;
  Vector3f normal;

  public MyFace(MyVertex v1, MyVertex v2, MyVertex v3, MyVertex v4) {
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    this.v4 = v4;
  }

  public Vector3f getCenter() {
    return v1.v.clone().addLocal(v2.v).addLocal(v3.v).addLocal(v4.v).divide(4);
  }
}