package fi.haju.haju3d.protocol.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class World implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final int CHUNK_SIZE = 64;
  private static final int CHUNK_OFFSET_INDEX = Integer.MAX_VALUE / 2 / CHUNK_SIZE;
  private static final int CHUNK_OFFSET_WORLD = CHUNK_OFFSET_INDEX * CHUNK_SIZE;

  private Map<ChunkPosition, Chunk> chunks = new HashMap<>();

  public Tile get(int x, int y, int z) {
    ChunkPosition c = new ChunkPosition(getChunkIndex(x), getChunkIndex(y), getChunkIndex(z));
    GlobalTilePosition wp = getWorldPosition(c);
    return chunks.get(c).get(x - wp.x, y - wp.y, z - wp.z);
  }

  public float getColor(int x, int y, int z) {
    ChunkPosition c = new ChunkPosition(getChunkIndex(x), getChunkIndex(y), getChunkIndex(z));
    GlobalTilePosition wp = getWorldPosition(c);
    return chunks.get(c).getColor(x - wp.x, y - wp.y, z - wp.z);
  }

  public static LocalTilePosition getPositionWithinChunk(GlobalTilePosition position) {
    ChunkPosition c = getChunkIndex(position);
    GlobalTilePosition wp = getWorldPosition(c);
    return new LocalTilePosition(position.x - wp.x, position.y - wp.y, position.z - wp.z);
  }
  
  public synchronized void setChunk(ChunkPosition position, Chunk chunk) {
    chunks.put(position, chunk);
  }

  public synchronized Chunk getChunk(ChunkPosition position) {
    return chunks.get(position);
  }

  public synchronized boolean hasChunk(ChunkPosition position) {
    return chunks.get(position) != null;
  }

  public static ChunkPosition getChunkIndex(GlobalTilePosition worldPosition) {
    return new ChunkPosition(getChunkIndex(worldPosition.x), getChunkIndex(worldPosition.y), getChunkIndex(worldPosition.z));
  }

  public static GlobalTilePosition getWorldPosition(ChunkPosition chunkIndex) {
    return new GlobalTilePosition(getWorldPosition(chunkIndex.x), getWorldPosition(chunkIndex.y), getWorldPosition(chunkIndex.z));
  }

  private static int getChunkIndex(int worldPosition) {
    return (worldPosition + CHUNK_OFFSET_WORLD) / CHUNK_SIZE - CHUNK_OFFSET_INDEX;
  }

  private static int getWorldPosition(int chunkIndex) {
    return chunkIndex * CHUNK_SIZE;
  }

  public int getChunkSize() {
    return CHUNK_SIZE;
  }
}
