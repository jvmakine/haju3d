package fi.haju.haju3d.protocol.coordinate;

/**
 * Represents a 3d coordinates of a tile within a chunk
 */
public final class LocalTilePosition extends Vector3i {

  private static final long serialVersionUID = 1L;

  public final static LocalTilePosition UP = new LocalTilePosition(0, 1, 0);
  public final static LocalTilePosition DOWN = new LocalTilePosition(0, -1, 0);
  
  public LocalTilePosition(int x, int y, int z) {
    super(x, y, z);
  }

  @Override
  public LocalTilePosition add(Vector3i v) {
    return convertFromVector(super.add(v));
  }

  @Override
  public LocalTilePosition add(int x, int y, int z) {
    return convertFromVector(super.add(x, y, z));
  }

  private static LocalTilePosition convertFromVector(Vector3i v) {
    return new LocalTilePosition(v.x, v.y, v.z);
  }

}
