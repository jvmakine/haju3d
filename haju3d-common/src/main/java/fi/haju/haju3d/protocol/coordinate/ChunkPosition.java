package fi.haju.haju3d.protocol.coordinate;

import com.google.common.collect.Lists;

import java.util.List;


/**
 * Represents a 3d coordinates of a single chunk within world
 */
public final class ChunkPosition extends Vector3i {

  private static final long serialVersionUID = 1L;

  public ChunkPosition(int x, int y, int z) {
    super(x, y, z);
  }

  @Override
  public ChunkPosition add(int x, int y, int z) {
    return convertFromVector(super.add(x, y, z));
  }

  public List<ChunkPosition> getPositionsAtMaxDistance(int distance) {
    List<ChunkPosition> positions = getSurroundingPositions(distance, distance, distance);
    List<ChunkPosition> result = Lists.newArrayList();
    for (ChunkPosition pos : positions) {
      if (pos.distanceTo(this) <= distance) result.add(pos);
    }
    return result;
  }

  /**
   * Returns 3x3x3 list of all positions around this position. (This vector is also included in the set)
   */
  public List<ChunkPosition> getSurroundingPositions() {
    return getSurroundingPositions(1, 1, 1);
  }

  public List<ChunkPosition> getSurroundingPositions(int w, int h, int d) {
    List<ChunkPosition> positions = Lists.newArrayList();
    for (int x = -w; x <= w; x++) {
      for (int y = -h; y <= h; y++) {
        for (int z = -d; z <= d; z++) {
          positions.add(this.add(x, y, z));
        }
      }
    }
    return positions;
  }

  private static ChunkPosition convertFromVector(Vector3i v) {
    return new ChunkPosition(v.x, v.y, v.z);
  }

}
