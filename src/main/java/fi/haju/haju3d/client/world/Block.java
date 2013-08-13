package fi.haju.haju3d.client.world;

import java.io.Serializable;

import com.google.common.base.Optional;

public class Block implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private final int depthLevel;         // 1 = 1m^3, 2 = 0.5m^3, 3 = 0.25m^3 etc.
  private final Optional<Block> parent; // not given if depthLevel == 1
  private final Material material;
  
  public Block(Material material) {
    this.material = material;
    this.depthLevel = 1;
    this.parent = Optional.absent();
  }
  
  public int getDepthLevel() {
    return depthLevel;
  }
  
  public Optional<Block> getParent() {
    return parent;
  }
  
  public Material getMaterial() {
    return material;
  }
  
}
