package fi.haju.haju3d.protocol.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;

public final class ChunkCoordinateSystem {
  public static final ChunkCoordinateSystem DEFAULT = new ChunkCoordinateSystem(64);

  private final int chunkSize;
  private final int chunkOffsetIndex;
  private final int chunkOffsetWorld;

  public ChunkCoordinateSystem(int chunkSize) {
    this.chunkSize = chunkSize;
    this.chunkOffsetIndex = Integer.MAX_VALUE / 2 / this.chunkSize;
    this.chunkOffsetWorld = chunkOffsetIndex * this.chunkSize;
  }

  public LocalTilePosition getPositionWithinChunk(GlobalTilePosition position) {
    ChunkPosition c = getChunkIndex(position);
    GlobalTilePosition wp = getWorldPosition(c);
    return new LocalTilePosition(position.x - wp.x, position.y - wp.y, position.z - wp.z);
  }

  public ChunkPosition getChunkIndex(int x, int y, int z) {
    return new ChunkPosition(getChunkIndex(x), getChunkIndex(y), getChunkIndex(z));
  }

  public ChunkPosition getChunkIndex(GlobalTilePosition worldPosition) {
    return new ChunkPosition(getChunkIndex(worldPosition.x), getChunkIndex(worldPosition.y), getChunkIndex(worldPosition.z));
  }

  public GlobalTilePosition getWorldPosition(ChunkPosition chunkIndex) {
    return new GlobalTilePosition(getWorldPosition(chunkIndex.x), getWorldPosition(chunkIndex.y), getWorldPosition(chunkIndex.z));
  }

  private int getChunkIndex(int worldPosition) {
    return (worldPosition + chunkOffsetWorld) / chunkSize - chunkOffsetIndex;
  }

  private int getWorldPosition(int chunkIndex) {
    return chunkIndex * chunkSize;
  }

  public int getChunkSize() {
    return chunkSize;
  }
}
