package fi.haju.haju3d.protocol.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.GlobalTilePosition;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class World implements Serializable {
  private static final long serialVersionUID = 1L;

  private ChunkCoordinateSystem chunkCoordinateSystem;
  private Map<ChunkPosition, Chunk> chunks = new HashMap<>();

  public World() {
    this(ChunkCoordinateSystem.DEFAULT);
  }

  public World(ChunkCoordinateSystem chunkCoordinateSystem) {
    this.chunkCoordinateSystem = chunkCoordinateSystem;
  }

  public ChunkCoordinateSystem getChunkCoordinateSystem() {
    return chunkCoordinateSystem;
  }

  public Tile get(int x, int y, int z) {
    ChunkPosition c = chunkCoordinateSystem.getChunkPosition(x, y, z);
    GlobalTilePosition wp = chunkCoordinateSystem.getWorldPosition(c);
    return chunks.get(c).get(x - wp.x, y - wp.y, z - wp.z);
  }

  public float getColor(int x, int y, int z) {
    ChunkPosition c = chunkCoordinateSystem.getChunkPosition(x, y, z);
    GlobalTilePosition wp = chunkCoordinateSystem.getWorldPosition(c);
    return chunks.get(c).getColor(x - wp.x, y - wp.y, z - wp.z);
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
}
