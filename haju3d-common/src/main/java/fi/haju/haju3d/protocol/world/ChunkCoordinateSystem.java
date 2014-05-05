package fi.haju.haju3d.protocol.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;

public final class ChunkCoordinateSystem {
  public static final ChunkCoordinateSystem DEFAULT = new ChunkCoordinateSystem(6);

  private final int chunkSizeLog2;
  private final int chunkOffsetIndex;
  private final int chunkOffsetWorld;

  public ChunkCoordinateSystem(int chunkSizeLog2) {
    this.chunkSizeLog2 = chunkSizeLog2;
    this.chunkOffsetIndex = Integer.MAX_VALUE / 2 / getChunkSize();
    this.chunkOffsetWorld = chunkOffsetIndex << chunkSizeLog2;
  }

  public LocalTilePosition getPositionWithinChunk(GlobalTilePosition position) {
    ChunkPosition c = getChunkPosition(position);
    GlobalTilePosition wp = getWorldPosition(c);
    return new LocalTilePosition(position.x - wp.x, position.y - wp.y, position.z - wp.z);
  }

  public ChunkPosition getChunkPosition(int x, int y, int z) {
    return new ChunkPosition(getChunkPosition(x), getChunkPosition(y), getChunkPosition(z));
  }

  public ChunkPosition getChunkPosition(GlobalTilePosition worldPosition) {
    return new ChunkPosition(getChunkPosition(worldPosition.x), getChunkPosition(worldPosition.y), getChunkPosition(worldPosition.z));
  }

  public GlobalTilePosition getWorldPosition(ChunkPosition chunkPosition) {
    return new GlobalTilePosition(getWorldPosition(chunkPosition.x), getWorldPosition(chunkPosition.y), getWorldPosition(chunkPosition.z));
  }

  private int getChunkPosition(int worldPosition) {
    return (worldPosition + chunkOffsetWorld) / getChunkSize() - chunkOffsetIndex;
  }

  private int getWorldPosition(int chunkPosition) {
    return chunkPosition * getChunkSize();
  }

  public int getChunkSize() {
    return 1 << chunkSizeLog2;
  }
  
  public int getChunkSizeLog2() {
    return chunkSizeLog2;
  }
}
