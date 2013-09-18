package fi.haju.haju3d.protocol.world;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import fi.haju.haju3d.protocol.Vector3i;

public class World implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private static final int CHUNK_SIZE = 64;
  private static final int CHUNK_OFFSET_INDEX = Integer.MAX_VALUE / 2 / CHUNK_SIZE;
  private static final int CHUNK_OFFSET_WORLD = CHUNK_OFFSET_INDEX * CHUNK_SIZE;
  
  private Map<Vector3i, Chunk> chunks = new HashMap<>();
  
  public Tile get(int x, int y, int z) {
    Vector3i c = new Vector3i(getChunkIndex(x), getChunkIndex(y), getChunkIndex(z));
    Vector3i wp = getWorldPosition(c);
    return chunks.get(c).get(x - wp.x, y - wp.y, z - wp.z);
  }
  
  public void setChunk(Vector3i position, Chunk chunk) {
    chunks.put(position, chunk);
  }
  
  public Chunk getChunk(Vector3i position) {
    return chunks.get(position);
  }
  
  public boolean hasChunk(Vector3i position) {
    return chunks.get(position) != null;
  }
  
  public Vector3i getChunkIndex(Vector3i worldPosition) {
    return new Vector3i(getChunkIndex(worldPosition.x), getChunkIndex(worldPosition.y), getChunkIndex(worldPosition.z));
  }
  
  public Vector3i getWorldPosition(Vector3i chunkIndex) {
    return new Vector3i(getWorldPosition(chunkIndex.x), getWorldPosition(chunkIndex.y), getWorldPosition(chunkIndex.z));
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
