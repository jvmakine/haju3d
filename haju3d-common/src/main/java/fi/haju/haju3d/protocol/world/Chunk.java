package fi.haju.haju3d.protocol.world;

import java.io.Serializable;

/**
 * 64 x 64 x 64 set of blocks
 */
public abstract class Chunk implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int x;
  private int y;
  private int z;
  
  public abstract Block getBlockAt(int x, int y, int z);
  
  public Chunk(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public int getXCoord() {
    return x;
  }
  
  public int getYCoord() {
    return y;
  }
  
  public int getZCoord() {
    return z;
  }
  
}
