package fi.haju.haju3d.protocol.coordinate;

import java.util.List;

import com.google.common.collect.Lists;


/**
 * Represents a 3d coordinates of a tile within a chunk
 */
public final class LocalTilePosition extends Vector3i {

  private static final long serialVersionUID = 1L;

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
  
  public List<LocalTilePosition> getSurroundingPositions(int w, int h, int d) {
    List<LocalTilePosition> positions = Lists.newArrayList();
    for (int x = -w; x <= w; x++) {
      for (int y = -h; y <= h; y++) {
        for (int z = -d; z <= d; z++) {
          positions.add(this.add(x, y, z));
        }
      }
    }
    return positions;
  }
  

}
