package fi.haju.haju3d.protocol;

/**
 * Represents a 3d coordinates of a tile within a chunk
 */
public class PositionWithinChunk extends Vector3i {

  private static final long serialVersionUID = 1L;

  public PositionWithinChunk(int x, int y, int z) {
    super(x, y, z);
  }

  @Override
  public PositionWithinChunk add(Vector3i v) {
    return convertFromVector(super.add(v));
  }

  @Override
  public PositionWithinChunk add(int x, int y, int z) {
    return convertFromVector(super.add(x, y, z));
  }
  
  private static PositionWithinChunk convertFromVector(Vector3i v) {
    return new PositionWithinChunk(v.x, v.y, v.z);
  }
  

}
