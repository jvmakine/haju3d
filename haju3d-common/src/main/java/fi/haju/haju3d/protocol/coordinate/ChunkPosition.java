package fi.haju.haju3d.protocol.coordinate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;


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
  
  public Set<TilePosition> getEdgeTilePositions(int chunkSize) {
    Set<TilePosition> result = Sets.newHashSet();
    for(int i = 0; i < chunkSize; ++i) {
      for(int j = 0; j < chunkSize; ++j) {
        result.add(new TilePosition(this, new LocalTilePosition(i, j, 0)));
        result.add(new TilePosition(this, new LocalTilePosition(i, j, chunkSize-1)));
        
        result.add(new TilePosition(this, new LocalTilePosition(i, 0, j)));
        result.add(new TilePosition(this, new LocalTilePosition(i, chunkSize-1, j)));
        
        result.add(new TilePosition(this, new LocalTilePosition(0, i, j)));
        result.add(new TilePosition(this, new LocalTilePosition(chunkSize-1, i, j)));
      } 
    }
    return result;
  }

  private static ChunkPosition convertFromVector(Vector3i v) {
    return new ChunkPosition(v.x, v.y, v.z);
  }

}
