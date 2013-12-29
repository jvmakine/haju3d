package fi.haju.haju3d.client.ui.mesh;

import com.jme3.math.Vector3f;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;

public final class MyFace {
  public final MyVertex v1;
  public final MyVertex v2;
  public final MyVertex v3;
  public final MyVertex v4;
  public Vector3f normal;
  public Vector3f center;
  public final MyTexture texture;
  public final float color;
  public final boolean realTile;
  public final int zIndex;
  public final Tile tile;

  public final int lightR;
  public final int lightG;
  public final int lightB;
  public final Vector3i worldPos;

  public MyFace(MyVertex v1, MyVertex v2, MyVertex v3, MyVertex v4, MyTexture texture, float color, boolean realTile, int zIndex, Tile tile, int lightR, int lightG, int lightB, Vector3i worldPos) {
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    this.v4 = v4;
    this.texture = texture;
    this.color = color;
    this.realTile = realTile;
    this.zIndex = zIndex;
    this.tile = tile;
    this.lightR = lightR;
    this.lightG = lightG;
    this.lightB = lightB;
    this.worldPos = worldPos;
  }

  public void calcCenter() {
    this.center = v1.v.clone().addLocal(v2.v).addLocal(v3.v).addLocal(v4.v).divideLocal(4);
  }
}
