package fi.haju.haju3d.client.ui.mesh;

import com.jme3.math.Vector3f;

public final class MyVertex {
  public Vector3f v;
  public int smooths = 0;

  public MyVertex(Vector3f v) {
    this.v = v;
  }
}