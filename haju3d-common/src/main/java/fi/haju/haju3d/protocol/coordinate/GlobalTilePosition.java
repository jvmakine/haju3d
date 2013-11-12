package fi.haju.haju3d.protocol.coordinate;


/**
 * Represents a 3d coordinates of a tile within a world
 */
public final class GlobalTilePosition extends Vector3i {

  private static final long serialVersionUID = 1L;

  public GlobalTilePosition(int x, int y, int z) {
    super(x, y, z);
  }

  @Override
  public GlobalTilePosition add(Vector3i v) {
    return convertFromVector(super.add(v));
  }

  @Override
  public GlobalTilePosition add(int x, int y, int z) {
    return convertFromVector(super.add(x, y, z));
  }

  private static GlobalTilePosition convertFromVector(Vector3i v) {
    return new GlobalTilePosition(v.x, v.y, v.z);
  }


}
